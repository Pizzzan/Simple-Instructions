package com.krimitt.simple_instructions.tutorial;

import com.krimitt.simple_instructions.SimpleInstructions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Path;

public class TextureGuideScreen extends Screen {
	private final Screen parent;
	private int scroll = 0;
	private String exportedPath = null;
	private long exportedPathResetAt = 0;

	private static final String[] LINES = {
		"\u00a7e\u00a7lHow to Create Custom Plaque Textures",
		"",
		"\u00a7fTexture Specifications:",
		"\u00a77  - Image: any size PNG (e.g. 64x64, 128x128)",
		"\u00a77  - The texture is nine-sliced using a configurable border",
		"\u00a77  - Border size is adjustable in the Texture Editor",
		"\u00a77  - Corners are scaled proportionally to plaque height",
		"\u00a77  - Edges and center are tiled (not stretched)",
		"",
		"\u00a7fRegion Map (border = B pixels):",
		"\u00a79  [Corner BxB] [--Top Edge--] [Corner BxB]",
		"\u00a79  [Left Edge ]  [  Center   ]  [Right Edge]",
		"\u00a79  [Corner BxB] [-Bot Edge---] [Corner BxB]",
		"",
		"\u00a7fStep-by-Step:",
		"\u00a7a  1. Open any pixel art editor (Aseprite, GIMP, Paint.NET)",
		"\u00a7a  2. Create a PNG image (64x64 recommended)",
		"\u00a7a  3. Draw your border in the outer ring",
		"\u00a7a  4. Draw your fill pattern in the center area",
		"\u00a7a  5. Export as PNG",
		"\u00a7a  6. Click 'Open Textures Folder' below and drop the file in",
		"\u00a7a  7. Re-open the Texture Editor to see your texture",
		"\u00a7a  8. Adjust the border size slider if edges look off",
		"",
		"\u00a7fTemplate:",
		"\u00a77  Click 'Export Template' below to save a color-coded",
		"\u00a77  template PNG to the textures folder.",
		"\u00a77  Blue = corners, Red = border edges, Green = center fill.",
		"\u00a77  Paint over it with your design and rename it.",
		"",
		"\u00a7fTips:",
		"\u00a7e  - Keep it simple! Minecraft-style pixel art works best",
		"\u00a7e  - Use warm/muted colors for a parchment look",
		"\u00a7e  - The tint color in the editor multiplies your texture colors",
		"\u00a7e  - Name your file something descriptive (e.g. diamond.png)",
	};

	public TextureGuideScreen(Screen parent) {
		super(Text.literal("Custom Texture Guide"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int btnW = 140;
		int btnGap = 4;
		int bbY = height - 28;

		addDrawableChild(ButtonWidget.builder(Text.literal("Export Template"), btn -> {
			try {
				Path out = PlaqueTextures.exportTemplate();
				btn.setMessage(Text.literal("\u00a7aExported!"));
				exportedPath = "\u00a77Saved to textures folder: \u00a7a" + out.getFileName().toString();
				exportedPathResetAt = Util.getMeasuringTimeMs() + 5000;
			} catch (Exception e) {
				SimpleInstructions.LOGGER.error("Failed to export template", e);
				btn.setMessage(Text.literal("\u00a7cFailed!"));
				exportedPath = null;
				exportedPathResetAt = Util.getMeasuringTimeMs() + 3000;
			}
		}).dimensions(centerX - btnW - btnGap / 2 - btnW / 2, bbY, btnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Open Textures Folder"), btn -> {
			try {
				java.nio.file.Files.createDirectories(PlaqueTextures.CUSTOM_DIR);
				Util.getOperatingSystem().open(PlaqueTextures.CUSTOM_DIR.toFile());
			} catch (Exception ignored) {}
		}).dimensions(centerX - btnW / 2, bbY, btnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
			.dimensions(centerX + btnW / 2 + btnGap, bbY, 60, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);

		TextRenderer tr = client.textRenderer;
		ctx.drawCenteredTextWithShadow(tr, title, width / 2, 6, 0xFFFFDD00);

		int contentBottom = height - 42;
		int y = 22 - scroll;
		int left = width / 2 - 160;
		for (String line : LINES) {
			if (line.isEmpty()) {
				y += 5;
				continue;
			}
			if (y >= 16 && y < contentBottom) {
				ctx.drawTextWithShadow(tr, Text.literal(line), left, y, 0xFFFFFFFF);
			}
			y += 11;
		}

		ctx.fill(0, contentBottom, width, height, 0xFF000000);

		if (exportedPath != null) {
			Text pathText = Text.literal(exportedPath);
			int textWidth = tr.getWidth(pathText);
			int drawX = Math.max(2, width / 2 - textWidth / 2);
			ctx.drawTextWithShadow(tr, pathText, drawX, contentBottom + 2, 0xFFFFFFFF);
		}

		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		int totalHeight = LINES.length * 11;
		int contentBottom = height - 42;
		int viewHeight = contentBottom - 22;
		int maxScroll = Math.max(0, totalHeight - viewHeight);
		scroll = Math.max(0, Math.min(maxScroll, scroll - (int)(amount * 11)));
		return true;
	}

	@Override
	public void tick() {
		if (exportedPathResetAt > 0 && Util.getMeasuringTimeMs() >= exportedPathResetAt) {
			exportedPath = null;
			exportedPathResetAt = 0;
		}
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
