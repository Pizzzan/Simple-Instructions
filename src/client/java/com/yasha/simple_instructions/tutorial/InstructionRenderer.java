package com.yasha.simple_instructions.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class InstructionRenderer implements HudRenderCallback {
	private static final int BASE_HEIGHT = 44;
	private static final int PROGRESS_BAR_HEIGHT = 2;


	// 3D beveled border colors (raised look — matching vanilla advancement toast)
	// Outer dark edge
	private static final int BORDER_OUTER = 0xFF1C1810;
	// Bevel layer 1 (outermost bevel ring — dark bronze)
	private static final int BEVEL1_HIGHLIGHT = 0xFF6C5420; // top/left
	private static final int BEVEL1_SHADOW = 0xFF241808;    // bottom/right
	// Bevel layer 2 (medium gold)
	private static final int BEVEL2_HIGHLIGHT = 0xFF8C6C28; // top/left
	private static final int BEVEL2_SHADOW = 0xFF382410;    // bottom/right
	// Bevel layer 3 (bright gold)
	private static final int BEVEL3_HIGHLIGHT = 0xFFB49038; // top/left
	private static final int BEVEL3_SHADOW = 0xFF504018;    // bottom/right
	// Thin bright inner edge (1px — brightest point of border)
	private static final int INNER_HIGHLIGHT = 0xFFD4A840;  // top/left
	private static final int INNER_SHADOW = 0xFF685020;     // bottom/right

	// Icon frame colors (inset/recessed — opposite bevel direction)
	private static final int ICON_OUTER = 0xFF1C1810;           // same as main outer
	private static final int ICON_RING = 0xFF7C5C20;            // gold connecting border
	private static final int ICON_INSET_SHADOW = 0xFF141008;    // top/left (dark = pushed in)
	private static final int ICON_INSET_HIGHLIGHT = 0xFF4C3C18; // bottom/right (lighter = lit edge)
	private static final int ICON_FILL = 0xFF141008;            // very dark fill

	// Progress bar background
	private static final int BAR_BG = 0xFF302410;

	// Available font types (ID → display label)
	// Only built-in Minecraft fonts are guaranteed to work
	public static final String[] FONT_TYPES = {
		"default", "uniform", "alt"
	};
	public static final String[] FONT_LABELS = {
		"Pixel", "Smooth", "Enchant"
	};

	@Override
	public void onHudRender(DrawContext drawContext, float tickDelta) {
		// Skip HUD rendering when editor is open
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof PlaqueEditorScreen) return;

		InstructionManager mgr = InstructionManager.INSTANCE;
		if (!mgr.isActive()) return;

		InstructionStep step = mgr.getCurrentStep();
		if (step == null) return;

		AnimationState state = mgr.getState();
		if (state == AnimationState.IDLE || state == AnimationState.DONE) return;

		int screenWidth = client.getWindow().getScaledWidth();
		TextRenderer textRenderer = client.textRenderer;

		float anim = mgr.getAnimationProgress();

		// Per-step visual overrides
		StepVisualOverrides overrides = ModConfig.getOverridesForStep(step.getId());

		int plaqueWidth = overrides.resolvePlaqueWidth();
		float scale = overrides.resolvePlaqueScale() / 100f;
		float anchorX = screenWidth * overrides.resolvePositionXPercent() / 100f;
		int x = (int) (anchorX - plaqueWidth / 2f);
		int y = overrides.resolvePositionY();
		float centerX = anchorX;
		float centerY = y + BASE_HEIGHT / 2f;

		// Opacity
		float opacity = 1.0f;
		if (state == AnimationState.SLIDING_IN) {
			opacity = Math.min(1f, anim / 0.3f);
		} else if (state == AnimationState.SLIDING_OUT) {
			opacity = 1.0f - anim;
		}
		if (opacity < 0.01f) return;

		MatrixStack matrices = drawContext.getMatrices();
		matrices.push();

		matrices.translate(centerX, centerY, 0);
		matrices.scale(scale, scale, 1f);

		if (state == AnimationState.SLIDING_IN) {
			float expandRaw = Math.min(1f, anim / 0.45f);
			float scaleY = easeOutCubic(expandRaw);
			matrices.scale(1f, scaleY, 1f);
		}

		if (state == AnimationState.SLIDING_OUT) {
			float shrink = 1.0f - 0.05f * anim;
			matrices.scale(shrink, shrink, 1f);
		}

		matrices.translate(-centerX, -centerY, 0);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		String iconId = null;
		if (ModConfig.isShowIcon()) {
			iconId = (step.getIcon() != null) ? step.getIcon() : ModConfig.getDefaultIconItem();
		}

		drawPlaqueContent(drawContext, textRenderer, x, y, plaqueWidth,
				step.getTitle(), step.getDescription(), iconId,
				mgr.getProgress(), state, opacity, overrides, step.getActionType());

		// White flash overlay
		if (state == AnimationState.SLIDING_IN && ModConfig.isEnableFlash()) {
			float flashIntensity;
			if (anim < 0.25f) {
				flashIntensity = anim / 0.25f;
			} else if (anim < 0.45f) {
				flashIntensity = 1.0f;
			} else {
				flashIntensity = 1.0f - (anim - 0.45f) / 0.55f;
			}
			flashIntensity = Math.max(0f, Math.min(1f, flashIntensity));
			int maxFlashAlpha = (int) (ModConfig.getFlashIntensity() / 100f * 255);
			int flashAlpha = (int) (flashIntensity * maxFlashAlpha);
			if (flashAlpha > 2) {
				drawContext.fill(x, y, x + plaqueWidth, y + BASE_HEIGHT,
						(flashAlpha << 24) | 0xFFFFFF);
			}
		}

		matrices.pop();

		int screenHeight = client.getWindow().getScaledHeight();

		// Test mode overlay
		if (mgr.isTestMode()) {
			String testLabel = "TEST MODE \u2014 Press ESC to exit";
			int tw = textRenderer.getWidth(testLabel);
			int tx = screenWidth / 2 - tw / 2;
			int ty = screenHeight - 14;
			drawContext.fill(tx - 3, ty - 2, tx + tw + 3, ty + 10, 0xAA000000);
			drawContext.drawTextWithShadow(textRenderer, testLabel, tx, ty, 0xFFFF55);
		}

		// Skip button (below plaque, only when skippable and not in test mode)
		if (!mgr.isTestMode() && ModConfig.isSkippable() && state == AnimationState.WAITING) {
			float skipScale = overrides.resolvePlaqueScale() / 100f;
			int scaledW = (int) (plaqueWidth * skipScale);
			int scaledH = (int) (BASE_HEIGHT * skipScale);
			int skipScaledX = (int) (anchorX - scaledW / 2f);
			int skipScaledY = (int) (y + BASE_HEIGHT / 2f - scaledH / 2f);
			String skipLabel = "Skip All [X]";
			int sw = textRenderer.getWidth(skipLabel);
			int skipX = skipScaledX + scaledW - sw - 2;
			int skipY = skipScaledY + scaledH + 2;
			drawContext.drawTextWithShadow(textRenderer, skipLabel, skipX, skipY, 0x88AAAAAA);
		}

		// Step counter
		String counter = "Step " + (mgr.getCurrentIndex() + 1) + "/" + mgr.getTotalSteps();
		float counterScale = overrides.resolvePlaqueScale() / 100f;
		int scaledW2 = (int) (plaqueWidth * counterScale);
		int counterScaledX = (int) (anchorX - scaledW2 / 2f);
		int scaledH2 = (int) (BASE_HEIGHT * counterScale);
		int counterScaledY = (int) (y + BASE_HEIGHT / 2f - scaledH2 / 2f);
		int counterX = counterScaledX + 2;
		int counterY = counterScaledY + scaledH2 + 2;
		drawContext.drawTextWithShadow(textRenderer, counter, counterX, counterY, 0x88AAAAAA);
	}

	/**
	 * Draws the parchment background — either using a nine-slice texture or solid color layers.
	 * @param style plaque style preset name (or null to use global config)
	 */
	public static void drawParchmentBackground(DrawContext ctx, int x, int y, int w, int h, float opacity, int bgColorRgb, @Nullable String style) {
		String resolvedStyle = style != null ? style : ModConfig.getPlaqueStyle();
		if (PlaqueTextures.isSolidMode(resolvedStyle)) {
			drawSolidBackground(ctx, x, y, w, h, opacity, bgColorRgb);
		} else {
			Identifier texture = PlaqueTextures.getTexture(resolvedStyle);
			if (texture != null) {
				drawNineSliceBackground(ctx, x, y, w, h, opacity, texture, bgColorRgb, resolvedStyle);
			} else {
				drawSolidBackground(ctx, x, y, w, h, opacity, bgColorRgb);
			}
		}
	}

	private static void drawSolidBackground(DrawContext ctx, int x, int y, int w, int h, float opacity, int bgColorRgb) {
		int a = Math.max(4, (int) (opacity * 255));

		// Layer 0: 1px dark outer edge
		ctx.fill(x, y, x + w, y + h, applyAlpha(BORDER_OUTER, a));

		// Layer 1: bevel ring 1 (per-side colors for 3D raised effect)
		drawBevelRing(ctx, x + 1, y + 1, w - 2, h - 2, BEVEL1_HIGHLIGHT, BEVEL1_SHADOW, a);

		// Layer 2: bevel ring 2
		drawBevelRing(ctx, x + 2, y + 2, w - 4, h - 4, BEVEL2_HIGHLIGHT, BEVEL2_SHADOW, a);

		// Layer 3: bevel ring 3
		drawBevelRing(ctx, x + 3, y + 3, w - 6, h - 6, BEVEL3_HIGHLIGHT, BEVEL3_SHADOW, a);

		// Layer 4: thin bright inner edge
		drawBevelRing(ctx, x + 4, y + 4, w - 8, h - 8, INNER_HIGHLIGHT, INNER_SHADOW, a);

		// Layer 5: main parchment fill
		int fillColor = 0xFF000000 | bgColorRgb;
		ctx.fill(x + 5, y + 5, x + w - 5, y + h - 5, applyAlpha(fillColor, a));
	}

	private static void drawBevelRing(DrawContext ctx, int x, int y, int w, int h, int highlight, int shadow, int alpha) {
		// Top edge (highlight)
		ctx.fill(x, y, x + w, y + 1, applyAlpha(highlight, alpha));
		// Left edge (highlight)
		ctx.fill(x, y + 1, x + 1, y + h, applyAlpha(highlight, alpha));
		// Bottom edge (shadow)
		ctx.fill(x, y + h - 1, x + w, y + h, applyAlpha(shadow, alpha));
		// Right edge (shadow)
		ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, applyAlpha(shadow, alpha));
	}

	/**
	 * Draws a nine-sliced texture background with proportionally-scaled tiled edges and center.
	 * The texture is scaled so its height matches the plaque height, preserving visual proportions.
	 * Corners are drawn scaled, edges and center are tiled at the proportional scale.
	 */
	private static void drawNineSliceBackground(DrawContext ctx, int x, int y, int w, int h, float opacity, Identifier texture, int bgColorRgb, String style) {
		int b = ModConfig.getNineSliceBorder();

		// Look up actual texture dimensions for this style
		int[] texSize = PlaqueTextures.getTextureSize(style);
		int ts = texSize[0];
		int tsH = texSize[1];

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		float alpha = Math.max(0.02f, opacity);

		// Apply background color as a tint via shader color
		int defaultBg = 0xC6A050;
		if (bgColorRgb != defaultBg) {
			float r = ((bgColorRgb >> 16) & 0xFF) / 255f;
			float g = ((bgColorRgb >> 8) & 0xFF) / 255f;
			float bl = (bgColorRgb & 0xFF) / 255f;
			float maxC = Math.max(0.01f, Math.max(r, Math.max(g, bl)));
			RenderSystem.setShaderColor(r / maxC, g / maxC, bl / maxC, alpha);
		} else {
			RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
		}

		// Proportional scaling: scale texture so its height matches plaque height
		float vScale = (float) h / tsH;
		int cb = Math.max(1, Math.round(b * vScale)); // rendered border in screen pixels
		if (cb * 2 > w) cb = w / 2;
		if (cb * 2 > h) cb = h / 2;

		int innerW = w - cb * 2;
		int innerH = h - cb * 2;
		int texInnerW = ts - b * 2;  // source inner width in texels
		int texInnerH = tsH - b * 2; // source inner height in texels

		// Screen size of one full tile (proportionally scaled)
		int tileScrW = Math.max(1, Math.round(texInnerW * vScale));
		int tileScrH = Math.max(1, Math.round(texInnerH * vScale));

		// --- Corners (scaled proportionally) ---
		ctx.drawTexture(texture, x, y, cb, cb, 0, 0, b, b, ts, tsH);
		ctx.drawTexture(texture, x + w - cb, y, cb, cb, ts - b, 0, b, b, ts, tsH);
		ctx.drawTexture(texture, x, y + h - cb, cb, cb, 0, tsH - b, b, b, ts, tsH);
		ctx.drawTexture(texture, x + w - cb, y + h - cb, cb, cb, ts - b, tsH - b, b, b, ts, tsH);

		// --- Edges (tiled at proportional scale) ---
		if (innerW > 0) {
			// Top edge
			drawTiledScaled(ctx, texture, x + cb, y, innerW, cb,
				b, 0, texInnerW, b, tileScrW, cb, ts, tsH);
			// Bottom edge
			drawTiledScaled(ctx, texture, x + cb, y + h - cb, innerW, cb,
				b, tsH - b, texInnerW, b, tileScrW, cb, ts, tsH);
		}
		if (innerH > 0) {
			// Left edge
			drawTiledScaled(ctx, texture, x, y + cb, cb, innerH,
				0, b, b, texInnerH, cb, tileScrH, ts, tsH);
			// Right edge
			drawTiledScaled(ctx, texture, x + w - cb, y + cb, cb, innerH,
				ts - b, b, b, texInnerH, cb, tileScrH, ts, tsH);
		}

		// --- Center (tiled at proportional scale) ---
		if (innerW > 0 && innerH > 0) {
			drawTiledScaled(ctx, texture, x + cb, y + cb, innerW, innerH,
				b, b, texInnerW, texInnerH, tileScrW, tileScrH, ts, tsH);
		}

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	/**
	 * Draws a texture region tiled at a proportional scale to fill the destination area.
	 * Each full tile renders at (tileScrW x tileScrH) screen pixels from (srcW x srcH) source texels.
	 * Partial tiles at edges are clipped proportionally.
	 */
	private static void drawTiledScaled(DrawContext ctx, Identifier texture,
										 int destX, int destY, int destW, int destH,
										 int srcX, int srcY, int srcW, int srcH,
										 int tileScrW, int tileScrH,
										 int texW, int texH) {
		if (srcW <= 0 || srcH <= 0 || tileScrW <= 0 || tileScrH <= 0) return;
		for (int dy = 0; dy < destH; dy += tileScrH) {
			int drawH = Math.min(tileScrH, destH - dy);
			int clipSrcH = (drawH < tileScrH)
				? Math.max(1, Math.round((float) srcH * drawH / tileScrH))
				: srcH;
			for (int dx = 0; dx < destW; dx += tileScrW) {
				int drawW = Math.min(tileScrW, destW - dx);
				int clipSrcW = (drawW < tileScrW)
					? Math.max(1, Math.round((float) srcW * drawW / tileScrW))
					: srcW;
				ctx.drawTexture(texture, destX + dx, destY + dy, drawW, drawH,
					srcX, srcY, clipSrcW, clipSrcH, texW, texH);
			}
		}
	}

	/**
	 * Draws the complete plaque content (background, icon, text, progress bar).
	 * Used by both the HUD renderer and the visual editor for live preview.
	 * @param overrides per-step visual overrides (nullable — falls back to global config)
	 */
	public static void drawPlaqueContent(DrawContext ctx, TextRenderer textRenderer,
										  int x, int y, int w,
										  String title, String description,
										  String iconId, float progress,
										  AnimationState state, float opacity,
										  @Nullable StepVisualOverrides overrides,
										  @Nullable ActionType actionType) {
		int h = BASE_HEIGHT;
		int alpha = Math.max(4, (int) (opacity * 255));
		int alphaShift = alpha << 24;

		// Resolve per-step overrides with global fallback
		int bgColorRgb = overrides != null ? overrides.resolveBackgroundColor() : ModConfig.getBackgroundColor();
		int titleColorRgb = overrides != null ? overrides.resolveTitleColor() : ModConfig.getTitleColor();
		int descColorRgb = overrides != null ? overrides.resolveDescriptionColor() : ModConfig.getDescriptionColor();
		String font = overrides != null ? overrides.resolveFontType() : ModConfig.getFontType();
		boolean shadow = overrides != null ? overrides.resolveTextShadow() : ModConfig.isTextShadow();
		String style = overrides != null ? overrides.resolvePlaqueStyle() : ModConfig.getPlaqueStyle();

		// Background
		int bgOpacityPct = ModConfig.getBackgroundOpacity();
		float bgOpacity = opacity * bgOpacityPct / 100f;
		drawParchmentBackground(ctx, x, y, w, h, bgOpacity, bgColorRgb, style);

		// Icon
		boolean hasIcon = iconId != null;
		int iconOffset = 0;
		if (hasIcon) {
			iconOffset = 28;
			try {
				Identifier itemId = new Identifier(iconId);
				Item item = Registries.ITEM.get(itemId);
				if (item != Items.AIR) {
					int iconSize = 26;
					int ix = x + 6;
					int iy = y + (h - iconSize) / 2;
					ctx.fill(ix, iy, ix + iconSize, iy + iconSize, applyAlpha(ICON_OUTER, alpha));
					ctx.fill(ix + 1, iy + 1, ix + iconSize - 1, iy + iconSize - 1,
						applyAlpha(ICON_RING, alpha));
					drawBevelRing(ctx, ix + 2, iy + 2, iconSize - 4, iconSize - 4,
						ICON_INSET_SHADOW, ICON_INSET_HIGHLIGHT, alpha);
					ctx.fill(ix + 3, iy + 3, ix + iconSize - 3, iy + iconSize - 3,
						applyAlpha(ICON_FILL, alpha));
					float itemAlpha = opacity * (ModConfig.getBackgroundOpacity() / 100f);
					RenderSystem.setShaderColor(1f, 1f, 1f, itemAlpha);
					ctx.drawItem(new ItemStack(item), ix + 5, iy + 5);
					RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
				}
			} catch (Exception ignored) {}
		}

		// Text
		int textAreaLeft = x + iconOffset + 4;
		int textAreaRight = x + w - 4;
		int textMaxWidth = textAreaRight - textAreaLeft;
		int textCenterX = textAreaLeft + textMaxWidth / 2;
		int titleColor = titleColorRgb | alphaShift;
		int descColor = descColorRgb | alphaShift;

		// Font style
		Identifier fontId = resolveFontId(font);
		Style fontStyle = Style.EMPTY.withFont(fontId);

		// Trim text to fit within text area
		String trimmedTitle = title;
		String trimmedDesc = description;
		if (textRenderer.getWidth(title) > textMaxWidth) {
			trimmedTitle = textRenderer.trimToWidth(title, textMaxWidth - 6) + "..";
		}
		if (textRenderer.getWidth(description) > textMaxWidth) {
			trimmedDesc = textRenderer.trimToWidth(description, textMaxWidth - 6) + "..";
		}

		Text titleText = Text.literal(trimmedTitle).setStyle(fontStyle);
		Text descText = Text.literal(trimmedDesc).setStyle(fontStyle);

		if (shadow) {
			ctx.drawCenteredTextWithShadow(textRenderer, titleText, textCenterX, y + 8, titleColor);
			ctx.drawCenteredTextWithShadow(textRenderer, descText, textCenterX, y + 21, descColor);
		} else {
			int titleW = textRenderer.getWidth(titleText);
			int descW = textRenderer.getWidth(descText);
			ctx.drawText(textRenderer, titleText, textCenterX - titleW / 2, y + 8, titleColor, false);
			ctx.drawText(textRenderer, descText, textCenterX - descW / 2, y + 21, descColor, false);
		}

		// Progress bar / dismiss indicator
		int barX = x + (hasIcon ? iconOffset : 6);
		int barY = y + h - PROGRESS_BAR_HEIGHT - 4;
		int barMaxWidth = w - (hasIcon ? iconOffset : 6) - 6;

		if (actionType == ActionType.DISMISS) {
			// No progress bar — show "Click to continue" hint
			Text hintText = Text.literal("Click to continue").setStyle(fontStyle);
			int hintW = textRenderer.getWidth(hintText);
			int hintX = textCenterX - hintW / 2;
			int hintColor = (descColorRgb | alphaShift) & 0x88FFFFFF; // dimmer
			ctx.drawText(textRenderer, hintText, hintX, barY - 2, hintColor, shadow);
		} else {
			int barBgAlpha = Math.max(4, (int) (opacity * 0x88));
			ctx.fill(barX, barY, barX + barMaxWidth, barY + PROGRESS_BAR_HEIGHT,
					applyAlpha(BAR_BG, barBgAlpha));

			int fillWidth = (int) (barMaxWidth * progress);
			if (fillWidth > 0) {
				int barColor = (state == AnimationState.COMPLETING)
						? ModConfig.getBarCompleteColor() : ModConfig.getBarColor();
				ctx.fill(barX, barY, barX + fillWidth, barY + PROGRESS_BAR_HEIGHT,
						barColor | alphaShift);
			}
		}
	}

	private static int applyAlpha(int color, int alpha) {
		return (alpha << 24) | (color & 0x00FFFFFF);
	}

	private static float easeOutCubic(float t) {
		float t1 = 1f - t;
		return 1f - t1 * t1 * t1;
	}

	public static Identifier resolveFontId(String fontType) {
		return switch (fontType) {
			case "uniform" -> new Identifier("minecraft", "uniform");
			case "alt" -> new Identifier("minecraft", "alt");
			default -> Style.DEFAULT_FONT_ID;
		};
	}

	public static String nextFontType(String current) {
		for (int i = 0; i < FONT_TYPES.length; i++) {
			if (FONT_TYPES[i].equals(current)) {
				return FONT_TYPES[(i + 1) % FONT_TYPES.length];
			}
		}
		return FONT_TYPES[0];
	}

	public static String fontLabel(String fontType) {
		for (int i = 0; i < FONT_TYPES.length; i++) {
			if (FONT_TYPES[i].equals(fontType)) return FONT_LABELS[i];
		}
		return "Pixel";
	}

	public static int getBaseHeight() {
		return BASE_HEIGHT;
	}
}
