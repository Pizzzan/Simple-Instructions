package com.yasha.simple_instructions.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.util.Util;

public class PlaqueEditorScreen extends Screen {
	private static final int PLAQUE_HEIGHT = InstructionRenderer.getBaseHeight();
	private static final int HANDLE_SIZE = 6;
	private static final int MIN_WIDTH = 160;
	private static final int MAX_WIDTH = 320;

	// Layout constants (computed from screen size in init)
	private int leftW, rightW, centerX1, centerX2, panelTop, panelBottom;

	// Icon items for the icon picker
	private static final String[] ICON_ITEMS = {
		"minecraft:book", "minecraft:compass", "minecraft:map", "minecraft:clock",
		"minecraft:spyglass", "minecraft:torch", "minecraft:lantern", "minecraft:campfire",
		"minecraft:chest", "minecraft:ender_chest", "minecraft:barrel", "minecraft:shulker_box",
		"minecraft:crafting_table", "minecraft:furnace", "minecraft:anvil", "minecraft:enchanting_table",
		"minecraft:brewing_stand", "minecraft:smithing_table", "minecraft:stonecutter", "minecraft:grindstone",
		"minecraft:diamond_sword", "minecraft:iron_sword", "minecraft:bow", "minecraft:crossbow",
		"minecraft:trident", "minecraft:shield", "minecraft:diamond_pickaxe", "minecraft:iron_pickaxe",
		"minecraft:diamond_axe", "minecraft:iron_shovel", "minecraft:fishing_rod", "minecraft:shears",
		"minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings",
		"minecraft:diamond_boots", "minecraft:leather_boots", "minecraft:leather_leggings",
		"minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:golden_apple",
		"minecraft:apple", "minecraft:bread", "minecraft:cooked_beef", "minecraft:cake",
		"minecraft:cookie", "minecraft:melon_slice", "minecraft:carrot", "minecraft:potato",
		"minecraft:wheat", "minecraft:sugar", "minecraft:egg", "minecraft:feather",
		"minecraft:bone", "minecraft:string", "minecraft:leather", "minecraft:paper",
		"minecraft:ink_sac", "minecraft:redstone", "minecraft:glowstone_dust", "minecraft:gunpowder",
		"minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:iron_ingot",
		"minecraft:coal", "minecraft:lapis_lazuli", "minecraft:quartz", "minecraft:amethyst_shard",
		"minecraft:oak_sapling", "minecraft:oak_log", "minecraft:cobblestone", "minecraft:dirt",
		"minecraft:sand", "minecraft:gravel", "minecraft:glass", "minecraft:brick",
		"minecraft:bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:milk_bucket",
		"minecraft:painting", "minecraft:item_frame", "minecraft:armor_stand", "minecraft:name_tag",
		"minecraft:lead", "minecraft:saddle", "minecraft:minecart", "minecraft:oak_boat",
		"minecraft:ender_pearl", "minecraft:blaze_rod", "minecraft:nether_star", "minecraft:heart_of_the_sea",
		"minecraft:experience_bottle", "minecraft:totem_of_undying", "minecraft:elytra", "minecraft:firework_rocket"
	};

	private final Screen parent;
	private int selectedStepIndex = 0;
	private int stepListScroll = 0;

	// Drag state (plaque positioning)
	private boolean dragging = false;
	private int activeDragHandle = -1;
	private int dragOffsetX, dragOffsetY;
	private int resizeStartWidth, resizeStartScale;
	private int resizeStartMouseX, resizeStartMouseY;

	// Hover state
	private boolean hoveringPlaque = false;
	private int hoveringHandle = -1;
	private boolean hoveringIcon = false;
	private boolean wasHudHidden = false;
	private boolean hudStateCaptured = false;

	// Inline edit (right-click context menu on plaque)
	private int hoveringRegion = -1; // -1=none, 0=title, 1=desc, 2=bg
	private boolean contextMenuOpen = false;
	private int contextMenuX, contextMenuY;
	private int contextMenuTarget = -1;

	// Icon picker modal
	private boolean iconPickerOpen = false;
	private int iconPickerScroll = 0;

	// Key listening mode
	private boolean listeningForKey = false;

	// Right panel widgets
	private TextFieldWidget titleField, descField, countField;

	// Timed button messages
	private final java.util.ArrayList<TimedMsg> timedMsgs = new java.util.ArrayList<>();
	private record TimedMsg(ButtonWidget btn, Text original, long resetAt) {}

	private void flashButton(ButtonWidget btn, String msg, int durationMs) {
		Text original = btn.getMessage();
		btn.setMessage(Text.literal(msg));
		timedMsgs.add(new TimedMsg(btn, original, Util.getMeasuringTimeMs() + durationMs));
	}

	public PlaqueEditorScreen(@Nullable Screen parent) {
		super(Text.literal("Plaque Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		// Hide HUD (hotbar, health) while editor is open — only capture once
		if (client != null && !hudStateCaptured) {
			hudStateCaptured = true;
			wasHudHidden = client.options.hudHidden;
			client.options.hudHidden = true;
		}

		iconPickerOpen = false;
		contextMenuOpen = false;
		listeningForKey = false;

		List<InstructionStep> steps = ModConfig.getSteps();
		if (selectedStepIndex >= steps.size()) {
			selectedStepIndex = Math.max(0, steps.size() - 1);
		}
		if (stepListScroll > Math.max(0, steps.size() - 1)) {
			stepListScroll = Math.max(0, steps.size() - 1);
		}

		// === Layout computation ===
		leftW = Math.max(100, (int) (width * 0.22));
		rightW = Math.max(130, (int) (width * 0.25));
		centerX1 = leftW;
		centerX2 = width - rightW;
		panelTop = 18;
		panelBottom = height - 28;
		int bottomBarY = height - 24;

		// === Left panel: Step list ===
		int btnY = panelBottom - 16;
		int btnW = (leftW - 12) / 4;

		addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
			List<InstructionStep> s = ModConfig.getSteps();
			ModConfig.addStep(new InstructionStep(
				"step_" + (s.size() + 1), "New Step", "Description",
				ActionType.KEY_PRESS, "forward", 1, "minecraft:book"
			));
			selectedStepIndex = ModConfig.getSteps().size() - 1;
			ModConfig.save();
			rebuildWidgets();
		}).dimensions(4, btnY, btnW, 14).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("\u2191"), btn -> {
			ModConfig.moveStepUp(selectedStepIndex);
			selectedStepIndex = Math.max(0, selectedStepIndex - 1);
			ModConfig.save();
			rebuildWidgets();
		}).dimensions(4 + btnW + 1, btnY, btnW, 14).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("\u2193"), btn -> {
			ModConfig.moveStepDown(selectedStepIndex);
			selectedStepIndex = Math.min(ModConfig.getSteps().size() - 1, selectedStepIndex + 1);
			ModConfig.save();
			rebuildWidgets();
		}).dimensions(4 + (btnW + 1) * 2, btnY, btnW, 14).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("x"), btn -> {
			if (ModConfig.getSteps().size() > 1) {
				ModConfig.removeStep(selectedStepIndex);
				selectedStepIndex = Math.min(selectedStepIndex, ModConfig.getSteps().size() - 1);
				ModConfig.save();
				rebuildWidgets();
			}
		}).dimensions(4 + (btnW + 1) * 3, btnY, btnW, 14).build());

		// === Right panel: Step properties ===
		InstructionStep selected = getSelectedStep();
		if (selected != null) {
			int rx = centerX2 + 6;
			int rw = rightW - 12;
			int ry = panelTop + 24;
			int rowH = 22;

			// Title
			int labelW = textRenderer.getWidth("Title: ");
			titleField = addDrawableChild(new TextFieldWidget(
				textRenderer, rx + labelW, ry, rw - labelW, 14, Text.literal("Title")));
			titleField.setMaxLength(48);
			titleField.setText(selected.getTitle());
			titleField.setChangedListener(t -> {
				InstructionStep s = getSelectedStep();
				if (s != null && !t.trim().isEmpty()) { s.setTitle(t.trim()); ModConfig.save(); }
			});
			ry += rowH;

			// Description
			int descLabelW = textRenderer.getWidth("Desc: ");
			descField = addDrawableChild(new TextFieldWidget(
				textRenderer, rx + descLabelW, ry, rw - descLabelW, 14, Text.literal("Desc")));
			descField.setMaxLength(64);
			descField.setText(selected.getDescription());
			descField.setChangedListener(t -> {
				InstructionStep s = getSelectedStep();
				if (s != null && !t.trim().isEmpty()) { s.setDescription(t.trim()); ModConfig.save(); }
			});
			ry += rowH;

			// Icon button
			String iconName = selected.getIcon() != null ? selected.getIcon().replace("minecraft:", "") : "none";
			addDrawableChild(ButtonWidget.builder(Text.literal("Icon: " + iconName), btn -> {
				iconPickerOpen = !iconPickerOpen;
				iconPickerScroll = 0;
			}).dimensions(rx, ry, rw, 14).build());
			ry += rowH + 4;

			// Trigger type button
			String typeLabel = getActionTypeLabel(selected.getActionType());
			addDrawableChild(ButtonWidget.builder(Text.literal("Type: " + typeLabel), btn -> {
				InstructionStep s = getSelectedStep();
				if (s != null) {
					ActionType[] types = ActionType.values();
					int idx = s.getActionType().ordinal();
					s.setActionType(types[(idx + 1) % types.length]);
					// Set sensible defaults for new types
					if (s.getActionType() == ActionType.WAIT_DURATION && s.getRequiredCount() < 20) {
						s.setRequiredCount(60); // 3 seconds default
					}
					ModConfig.save();
					rebuildWidgets();
				}
			}).dimensions(rx, ry, rw, 14).build());
			ry += rowH;

			// Trigger-specific fields
			ActionType aType = selected.getActionType();
			if (aType == ActionType.KEY_PRESS || aType == ActionType.KEY_HOLD) {
				// Key binding button
				String keyDisplay = getKeyDisplayName(selected.getTargetKey());
				addDrawableChild(ButtonWidget.builder(
					Text.literal("Key: " + keyDisplay), btn -> {
						listeningForKey = true;
						btn.setMessage(Text.literal("Press a key..."));
					}).dimensions(rx, ry, rw, 14).build());
				ry += rowH;

				// Required count
				String countLabel = aType == ActionType.KEY_PRESS ? "Presses:" : "Hold ticks:";
				countField = addDrawableChild(new TextFieldWidget(
					textRenderer, rx + textRenderer.getWidth(countLabel) + 4, ry, rw - textRenderer.getWidth(countLabel) - 4, 14, Text.literal("Count")));
				countField.setMaxLength(4);
				countField.setText(String.valueOf(selected.getRequiredCount()));
				countField.setChangedListener(t -> {
					try {
						int val = Integer.parseInt(t);
						if (val > 0) {
							InstructionStep s = getSelectedStep();
							if (s != null) { s.setRequiredCount(val); ModConfig.save(); }
						}
					} catch (NumberFormatException ignored) {}
				});
				ry += rowH;
			} else if (aType == ActionType.WAIT_DURATION) {
				// Duration field (in ticks, 20 = 1 second)
				String durLabel = "Ticks (20=1s):";
				countField = addDrawableChild(new TextFieldWidget(
					textRenderer, rx + textRenderer.getWidth(durLabel) + 4, ry, rw - textRenderer.getWidth(durLabel) - 4, 14, Text.literal("Duration")));
				countField.setMaxLength(5);
				countField.setText(String.valueOf(selected.getRequiredCount()));
				countField.setChangedListener(t -> {
					try {
						int val = Integer.parseInt(t);
						if (val > 0) {
							InstructionStep s = getSelectedStep();
							if (s != null) { s.setRequiredCount(val); ModConfig.save(); }
						}
					} catch (NumberFormatException ignored) {}
				});
				ry += rowH;
			}
			// DISMISS has no extra fields

			// Texture style override
			ry += 4;
			StepVisualOverrides styleOv = ModConfig.getOverridesForStep(selected.getId());
			String currentStyle = styleOv.resolvePlaqueStyle();
			String styleDisplay = PlaqueTextures.getDisplayName(currentStyle);
			addDrawableChild(ButtonWidget.builder(Text.literal("Tex: " + styleDisplay), btn -> {
				InstructionStep s = getSelectedStep();
				if (s == null) return;
				StepVisualOverrides ov = ModConfig.getOverridesForStep(s.getId());
				java.util.List<String> presets = PlaqueTextures.getPresetNames();
				String cur = ov.resolvePlaqueStyle();
				int idx = presets.indexOf(cur);
				String next = presets.get((idx + 1) % presets.size());
				ov.setPlaqueStyle(next);
				ModConfig.save();
				rebuildWidgets();
			}).dimensions(rx, ry, rw, 14).build());
		}

		// === Center panel: Position/scale controls below preview ===
		StepVisualOverrides initOv = getSelectedOverrides();
		int ctrlY = panelBottom - 24;
		int ctrlW = (centerX2 - centerX1 - 16) / 3;
		int ctrlX = centerX1 + 4;

		addDrawableChild(new EditorSlider(ctrlX, ctrlY, ctrlW, 20,
			"W:", "px", MIN_WIDTH, MAX_WIDTH, initOv.resolvePlaqueWidth(), v -> {
			getSelectedOverrides().setPlaqueWidth(v); ModConfig.save();
		}));

		addDrawableChild(new EditorSlider(ctrlX + ctrlW + 2, ctrlY, ctrlW, 20,
			"S:", "%", 50, 200, initOv.resolvePlaqueScale(), v -> {
			getSelectedOverrides().setPlaqueScale(v); ModConfig.save();
		}));

		addDrawableChild(ButtonWidget.builder(Text.literal("Reset Pos"), btn -> {
			InstructionStep s = getSelectedStep();
			if (s != null) {
				StepVisualOverrides ov = ModConfig.getOverridesForStep(s.getId());
				ov.setPositionXPercent(null);
				ov.setPositionY(null);
				ov.setPlaqueWidth(null);
				ov.setPlaqueScale(null);
			}
			ModConfig.save();
			rebuildWidgets();
		}).dimensions(ctrlX + (ctrlW + 2) * 2, ctrlY, ctrlW, 20).build());

		// === Bottom bar ===
		int bbBtnW = 70;
		int bbGap = 4;
		int totalBbW = bbBtnW * 4 + bbGap * 3;
		int bbX = width / 2 - totalBbW / 2;

		addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), btn -> {
			client.setScreen(new SimpleInstructionsConfigScreen(this));
		}).dimensions(bbX, bottomBarY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Quick Test"), btn -> {
			if (client.world == null) {
				flashButton(btn, "\u00a7cNeed a world!", 2000);
				return;
			}
			ModConfig.save();
			Screen thisParent = this.parent;
			InstructionManager.INSTANCE.startTest(0, false, () -> {
				client.execute(() -> client.setScreen(new PlaqueEditorScreen(thisParent)));
			});
			client.setScreen(null);
		}).dimensions(bbX + bbBtnW + bbGap, bottomBarY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Test Step"), btn -> {
			if (client.world == null) {
				flashButton(btn, "\u00a7cNeed a world!", 2000);
				return;
			}
			ModConfig.save();
			int stepIdx = selectedStepIndex;
			Screen thisParent = this.parent;
			InstructionManager.INSTANCE.startTest(stepIdx, true, () -> {
				client.execute(() -> client.setScreen(new PlaqueEditorScreen(thisParent)));
			});
			client.setScreen(null);
		}).dimensions(bbX + (bbBtnW + bbGap) * 2, bottomBarY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
			.dimensions(bbX + (bbBtnW + bbGap) * 3, bottomBarY, bbBtnW, 20).build());
	}

	// ============================================================
	// Rendering
	// ============================================================

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		boolean isDragging = dragging || activeDragHandle >= 0;

		// During drag with a world: show game HUD, no dark overlay so world is visible.
		// During drag without a world (Mod Menu): still draw background to prevent trail artifacts.
		// Not dragging: hide HUD, dark overlay.
		if (isDragging && client.world != null) {
			client.options.hudHidden = wasHudHidden;
		} else {
			if (!isDragging) client.options.hudHidden = true;
			ctx.fill(0, 0, width, height, 0xCC000000);
		}

		TextRenderer tr = client.textRenderer;
		List<InstructionStep> steps = ModConfig.getSteps();

		// --- Left panel background ---
		if (!iconPickerOpen && !isDragging) {
			ctx.fill(0, panelTop, leftW, panelBottom, 0x44000000);
			ctx.drawTextWithShadow(tr, "Steps", 5, panelTop + 2, 0xFFFFDD00);

			// Step list entries
			int stepEntryH = 18;
			int listTop = panelTop + 14;
			int listBottom = panelBottom - 20;
			int maxVisible = (listBottom - listTop) / stepEntryH;

			for (int i = 0; i < maxVisible && i + stepListScroll < steps.size(); i++) {
				int idx = i + stepListScroll;
				InstructionStep step = steps.get(idx);
				int ey = listTop + i * stepEntryH;
				boolean sel = (idx == selectedStepIndex);
				boolean hover = mouseX >= 2 && mouseX < leftW - 2 && mouseY >= ey && mouseY < ey + stepEntryH - 1;
				int bgColor = sel ? 0xAA335588 : (hover ? 0x44FFFFFF : 0);
				if (bgColor != 0) ctx.fill(2, ey, leftW - 2, ey + stepEntryH - 1, bgColor);

				// Step number + title
				String label = (idx + 1) + ". " + step.getTitle();
				int maxLabelW = leftW - 12;
				if (tr.getWidth(label) > maxLabelW) label = tr.trimToWidth(label, maxLabelW - 8) + "..";
				ctx.drawTextWithShadow(tr, label, 6, ey + 4, sel ? 0xFFFF88 : 0xCCCCCC);
			}

			// Step list scroll indicator
			if (steps.size() > maxVisible) {
				int barH = Math.max(6, (listBottom - listTop) * maxVisible / steps.size());
				int maxScr = steps.size() - maxVisible;
				int barY = listTop + (listBottom - listTop - barH) * Math.min(stepListScroll, maxScr) / Math.max(1, maxScr);
				ctx.fill(leftW - 4, barY, leftW - 1, barY + barH, 0x66FFFFFF);
			}
		}

		// --- Right panel background ---
		if (!iconPickerOpen && !isDragging) {
			ctx.fill(centerX2, panelTop, width, panelBottom, 0x44000000);
			ctx.drawTextWithShadow(tr, "Properties", centerX2 + 6, panelTop + 2, 0xFFFFDD00);
		}

		// Right panel labels (skip during drag and icon picker)
		InstructionStep selected = getSelectedStep();
		if (!isDragging && !iconPickerOpen) {
			if (selected != null) {
				int rx = centerX2 + 6;
				int ry = panelTop + 24;
				int rowH = 22;
				// Title label (inline, left of field)
				ctx.drawTextWithShadow(tr, "Title:", rx, ry + 3, 0xAAAAAA);
				ry += rowH;
				// Desc label (inline, left of field)
				ctx.drawTextWithShadow(tr, "Desc:", rx, ry + 3, 0xAAAAAA);

				// If listening for key, show overlay text
				if (listeningForKey) {
					ctx.drawTextWithShadow(tr, ">> Press any key <<", rx, panelBottom - 34, 0xFFFF55);
				}
			} else {
				ctx.drawTextWithShadow(tr, "No step selected", centerX2 + 6, panelTop + 30, 0x888888);
			}
		}

		// --- Center panel: Live plaque preview ---
		if (!iconPickerOpen && selected != null) {
			renderPlaquePreview(ctx, tr, mouseX, mouseY, selected);
		}

		// During drag: only show plaque + guides, skip all chrome
		if (isDragging) {
			return;
		}

		// --- Title bar ---
		ctx.drawCenteredTextWithShadow(tr, title, width / 2, 4, 0xFFFFDD00);

		// --- Bottom bar background ---
		ctx.fill(0, height - 28, width, height, 0x66000000);

		// --- Widgets (skip when icon picker is open) ---
		if (!iconPickerOpen) {
			super.render(ctx, mouseX, mouseY, delta);

			// Inline edit tooltip
			if (hoveringRegion >= 0 && !contextMenuOpen && !dragging && activeDragHandle < 0) {
				String tip = switch (hoveringRegion) {
					case 0 -> "Right-click: edit title style";
					case 1 -> "Right-click: edit description style";
					default -> "Right-click: edit colors";
				};
				ctx.drawTextWithShadow(tr, tip, mouseX + 10, mouseY - 10, 0xFFFF88);
			}

			// Context menu
			if (contextMenuOpen) renderContextMenu(ctx, tr, mouseX, mouseY);
		} else {
			renderIconPicker(ctx, tr, mouseX, mouseY);
		}
	}

	private void renderPlaquePreview(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, InstructionStep selectedStep) {
		StepVisualOverrides ov = ModConfig.getOverridesForStep(selectedStep.getId());
		int plaqueWidth = ov.resolvePlaqueWidth();
		float scale = ov.resolvePlaqueScale() / 100f;

		float anchorX = width * ov.resolvePositionXPercent() / 100f;
		int px = (int) (anchorX - plaqueWidth / 2f);
		int py = ov.resolvePositionY();

		int scaledW = (int) (plaqueWidth * scale);
		int scaledH = (int) (PLAQUE_HEIGHT * scale);
		int scaledX = (int) (anchorX - scaledW / 2f);
		int scaledY = (int) (py + PLAQUE_HEIGHT / 2f - scaledH / 2f);

		updateHoverState(mouseX, mouseY, scaledX, scaledY, scaledW, scaledH);

		// Draw plaque with scale transform
		ctx.getMatrices().push();
		float centerX = anchorX;
		float centerY = py + PLAQUE_HEIGHT / 2f;
		ctx.getMatrices().translate(centerX, centerY, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.getMatrices().translate(-centerX, -centerY, 0);

		String iconId = ModConfig.isShowIcon()
			? (selectedStep.getIcon() != null ? selectedStep.getIcon() : ModConfig.getDefaultIconItem())
			: null;
		StepVisualOverrides overrides = ModConfig.getOverridesForStep(selectedStep.getId());
		InstructionRenderer.drawPlaqueContent(ctx, tr, px, py, plaqueWidth,
			selectedStep.getTitle(), selectedStep.getDescription(), iconId,
			0.6f, AnimationState.WAITING, 1.0f, overrides, selectedStep.getActionType());

		// Inline edit region highlight
		if (hoveringRegion >= 0 && !contextMenuOpen && !dragging && activeDragHandle < 0) {
			int ioOff = (iconId != null) ? 28 : 0;
			int textL = px + ioOff + 4;
			int textR = px + plaqueWidth - 4;
			int hx1, hy1, hx2, hy2;
			switch (hoveringRegion) {
				case 0 -> { hx1 = textL; hy1 = py + 5; hx2 = textR; hy2 = py + 18; }
				case 1 -> { hx1 = textL; hy1 = py + 18; hx2 = textR; hy2 = py + 33; }
				default -> { hx1 = px + 5; hy1 = py + 5; hx2 = px + plaqueWidth - 5; hy2 = py + PLAQUE_HEIGHT - 5; }
			}
			ctx.fill(hx1, hy1, hx2, hy2, 0x33FFFFFF);
		}

		ctx.getMatrices().pop();

		// Selection outline + handles
		if (hoveringPlaque || dragging || activeDragHandle >= 0 || hoveringHandle >= 0) {
			drawHandles(ctx, scaledX, scaledY, scaledW, scaledH);
		}

		if (hoveringIcon) {
			ctx.drawTextWithShadow(tr, "Click to change icon", mouseX + 8, mouseY - 8, 0xFFFF88);
		}

		// Positioning guidelines when dragging
		if (dragging || activeDragHandle >= 0) {
			drawPositioningGuides(ctx, tr, mouseX, mouseY, scaledX, scaledY, scaledW, scaledH);
		}
	}

	// ============================================================
	// Handle drawing & positioning guides
	// ============================================================

	private void drawHandles(DrawContext ctx, int sx, int sy, int sw, int sh) {
		int oc = 0xAAFFFFFF;
		ctx.fill(sx - 1, sy - 1, sx + sw + 1, sy, oc);
		ctx.fill(sx - 1, sy + sh, sx + sw + 1, sy + sh + 1, oc);
		ctx.fill(sx - 1, sy, sx, sy + sh, oc);
		ctx.fill(sx + sw, sy, sx + sw + 1, sy + sh, oc);

		int[][] positions = getHandlePositions(sx, sy, sw, sh);
		for (int i = 0; i < 6; i++) {
			int hx = positions[i][0], hy = positions[i][1];
			boolean active = (hoveringHandle == i || activeDragHandle == i);
			int color = active ? 0xFFFFFF00 : (i < 4 ? 0xBBFFCC88 : 0xAAFFFFFF);
			ctx.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, color);
		}
	}

	private void drawPositioningGuides(DrawContext ctx, TextRenderer tr,
									   int mouseX, int mouseY,
									   int sx, int sy, int sw, int sh) {
		StepVisualOverrides ov = getSelectedOverrides();
		int xPct = ov.resolvePositionXPercent();
		int yPx = ov.resolvePositionY();
		int screenCenterX = width / 2;

		// Dashed vertical center line
		int dashColor = (xPct == 50) ? 0x66FFFF00 : 0x33FFFFFF;
		for (int dy = 0; dy < height; dy += 8) {
			ctx.fill(screenCenterX, dy, screenCenterX + 1, Math.min(dy + 4, height), dashColor);
		}

		// Dashed horizontal line at plaque center
		int plaqueCenterY = sy + sh / 2;
		for (int dx = centerX1; dx < centerX2; dx += 8) {
			ctx.fill(dx, plaqueCenterY, Math.min(dx + 4, centerX2), plaqueCenterY + 1, 0x22FFFFFF);
		}

		// HUD mockup outlines
		int hudColor = 0x33FFFFFF;
		int hotbarW = 182, hotbarH = 22;
		int hotbarX = screenCenterX - hotbarW / 2;
		int hotbarY = height - hotbarH - 2;
		drawDashedRect(ctx, hotbarX, hotbarY, hotbarW, hotbarH, hudColor);
		drawDashedRect(ctx, screenCenterX - 91, hotbarY - 12, 81, 9, hudColor);
		drawDashedRect(ctx, screenCenterX + 10, hotbarY - 12, 81, 9, hudColor);

		// Snap indicator
		if (xPct == 50) {
			ctx.fill(screenCenterX - 2, sy - 1, screenCenterX + 3, sy, 0xCCFFFF00);
			ctx.fill(screenCenterX - 2, sy + sh, screenCenterX + 3, sy + sh + 1, 0xCCFFFF00);
		}

		// Live position readout
		String readout = "X: " + xPct + "%  Y: " + yPx + "px";
		int readoutX = mouseX + 14;
		int readoutY = mouseY + 14;
		if (readoutX + tr.getWidth(readout) + 4 > width) readoutX = mouseX - tr.getWidth(readout) - 10;
		ctx.fill(readoutX - 2, readoutY - 1, readoutX + tr.getWidth(readout) + 2, readoutY + 10, 0xAA000000);
		ctx.drawTextWithShadow(tr, readout, readoutX, readoutY, 0xFFFFFF);
	}

	private void drawDashedRect(DrawContext ctx, int x, int y, int w, int h, int color) {
		for (int dx = 0; dx < w; dx += 6) {
			ctx.fill(x + dx, y, Math.min(x + dx + 3, x + w), y + 1, color);
			ctx.fill(x + dx, y + h - 1, Math.min(x + dx + 3, x + w), y + h, color);
		}
		for (int dy = 0; dy < h; dy += 6) {
			ctx.fill(x, y + dy, x + 1, Math.min(y + dy + 3, y + h), color);
			ctx.fill(x + w - 1, y + dy, x + w, Math.min(y + dy + 3, y + h), color);
		}
	}

	private int[][] getHandlePositions(int sx, int sy, int sw, int sh) {
		int hh = HANDLE_SIZE / 2;
		return new int[][] {
			{sx - hh, sy - hh},
			{sx + sw - hh, sy - hh},
			{sx - hh, sy + sh - hh},
			{sx + sw - hh, sy + sh - hh},
			{sx - hh, sy + sh / 2 - hh},
			{sx + sw - hh, sy + sh / 2 - hh},
		};
	}

	private void updateHoverState(int mouseX, int mouseY, int sx, int sy, int sw, int sh) {
		if (dragging || activeDragHandle >= 0) return;

		hoveringHandle = -1;
		int[][] handles = getHandlePositions(sx, sy, sw, sh);
		for (int i = 0; i < 6; i++) {
			int hx = handles[i][0] - 2, hy = handles[i][1] - 2;
			if (mouseX >= hx && mouseX <= hx + HANDLE_SIZE + 4 && mouseY >= hy && mouseY <= hy + HANDLE_SIZE + 4) {
				hoveringHandle = i;
				break;
			}
		}

		hoveringIcon = false;
		if (hoveringHandle < 0 && ModConfig.isShowIcon()) {
			StepVisualOverrides ov = getSelectedOverrides();
			float scale = ov.resolvePlaqueScale() / 100f;
			float anchorX = width * ov.resolvePositionXPercent() / 100f;
			int px = (int) (anchorX - ov.resolvePlaqueWidth() / 2f);
			int py = ov.resolvePositionY();
			int iconX = (int) ((px + 6) * scale + anchorX * (1 - scale));
			int iconY = (int) ((py + (PLAQUE_HEIGHT - 22) / 2f) * scale + (py + PLAQUE_HEIGHT / 2f) * (1 - scale));
			int iconSize = (int) (22 * scale);
			hoveringIcon = mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize;
		}

		hoveringPlaque = hoveringHandle < 0 && !hoveringIcon
			&& mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;

		// Determine which plaque region is hovered (for inline editing)
		hoveringRegion = -1;
		if (hoveringPlaque) {
			StepVisualOverrides ov = getSelectedOverrides();
			float scale = ov.resolvePlaqueScale() / 100f;
			float anchorX = width * ov.resolvePositionXPercent() / 100f;
			int px = (int) (anchorX - ov.resolvePlaqueWidth() / 2f);
			int py = ov.resolvePositionY();
			float centerX = anchorX;
			float centerY = py + PLAQUE_HEIGHT / 2f;

			float plaqueMouseX = (mouseX - centerX) / scale + centerX;
			float plaqueMouseY = (mouseY - centerY) / scale + centerY;

			int iconOff = ModConfig.isShowIcon() ? 28 : 0;
			int textLeft = px + iconOff + 4;
			int textRight = px + ov.resolvePlaqueWidth() - 4;
			float relY = plaqueMouseY - py;

			if (relY >= 5 && relY < 18 && plaqueMouseX >= textLeft && plaqueMouseX <= textRight) {
				hoveringRegion = 0;
			} else if (relY >= 18 && relY < 33 && plaqueMouseX >= textLeft && plaqueMouseX <= textRight) {
				hoveringRegion = 1;
			} else {
				hoveringRegion = 2;
			}
		}
	}

	// ============================================================
	// Input handling
	// ============================================================

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (iconPickerOpen) {
			if (handleIconPickerClick((int) mouseX, (int) mouseY)) return true;
			iconPickerOpen = false;
			return true;
		}

		if (contextMenuOpen) {
			if (handleContextMenuClick((int) mouseX, (int) mouseY)) return true;
			contextMenuOpen = false;
			return true;
		}

		// Key listening - capture mouse button
		if (listeningForKey && button <= 2) {
			String keyName = switch (button) {
				case 0 -> "attack";
				case 1 -> "use";
				default -> "key.mouse." + button;
			};
			InstructionStep s = getSelectedStep();
			if (s != null) { s.setTargetKey(keyName); ModConfig.save(); }
			listeningForKey = false;
			rebuildWidgets();
			return true;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		// Step list click
		int stepEntryH = 18;
		int listTop = panelTop + 14;
		int listBottom = panelBottom - 20;
		if (mouseX >= 2 && mouseX < leftW - 2 && mouseY >= listTop && mouseY < listBottom) {
			int clickedIdx = (int) ((mouseY - listTop) / stepEntryH) + stepListScroll;
			if (clickedIdx >= 0 && clickedIdx < ModConfig.getSteps().size()) {
				selectedStepIndex = clickedIdx;
				rebuildWidgets();
				return true;
			}
		}

		// Right-click on plaque region → open context menu
		if (button == 1 && hoveringRegion >= 0) {
			contextMenuOpen = true;
			contextMenuTarget = hoveringRegion;
			contextMenuX = (int) mouseX;
			contextMenuY = (int) mouseY;
			int menuW = 120;
			int menuH = getContextMenuRows() * 16 + 4;
			// Keep within center panel — don't overlap left/right panels or bottom bar
			if (contextMenuX + menuW > centerX2) contextMenuX = centerX2 - menuW;
			if (contextMenuX < leftW) contextMenuX = leftW;
			if (contextMenuY + menuH > height - 28) contextMenuY = height - 28 - menuH;
			if (contextMenuY < panelTop) contextMenuY = panelTop;
			return true;
		}

		if (button != 0) return false;

		if (hoveringIcon) { iconPickerOpen = true; iconPickerScroll = 0; return true; }
		if (hoveringHandle >= 0) {
			activeDragHandle = hoveringHandle;
			StepVisualOverrides ov = getSelectedOverrides();
			resizeStartWidth = ov.resolvePlaqueWidth();
			resizeStartScale = ov.resolvePlaqueScale();
			resizeStartMouseX = (int) mouseX;
			resizeStartMouseY = (int) mouseY;
			return true;
		}
		if (hoveringPlaque) {
			dragging = true;
			StepVisualOverrides ov = getSelectedOverrides();
			float scale = ov.resolvePlaqueScale() / 100f;
			int scaledW = (int) (ov.resolvePlaqueWidth() * scale);
			float anchorX = width * ov.resolvePositionXPercent() / 100f;
			int scaledX = (int) (anchorX - scaledW / 2f);
			dragOffsetX = (int) mouseX - scaledX;
			dragOffsetY = (int) mouseY - ov.resolvePositionY();
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging || activeDragHandle >= 0) {
			dragging = false;
			activeDragHandle = -1;
			ModConfig.save();
			rebuildWidgets();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
		if (super.mouseDragged(mouseX, mouseY, button, dX, dY)) return true;

		if (dragging) {
			StepVisualOverrides ov = getSelectedOverrides();
			float scale = ov.resolvePlaqueScale() / 100f;
			int scaledW = (int) (ov.resolvePlaqueWidth() * scale);
			int newScaledX = (int) mouseX - dragOffsetX;
			float newAnchorX = newScaledX + scaledW / 2f;
			int xPct = clamp((int) (newAnchorX / width * 100), 0, 100);
			if (Math.abs(xPct - 50) <= 2) xPct = 50;
			ov.setPositionXPercent(xPct);
			int scaledH = (int) (PLAQUE_HEIGHT * scale);
			ov.setPositionY(clamp((int) mouseY - dragOffsetY, 0, height - scaledH));
			return true;
		}

		if (activeDragHandle >= 0) {
			StepVisualOverrides ov = getSelectedOverrides();
			float scale = ov.resolvePlaqueScale() / 100f;
			int dx = (int) mouseX - resizeStartMouseX;
			int dy = (int) mouseY - resizeStartMouseY;

			if (activeDragHandle <= 3) {
				int diagonal = (activeDragHandle == 0 || activeDragHandle == 2) ? -dx + dy : dx + dy;
				if (activeDragHandle <= 1) diagonal = (activeDragHandle == 0) ? -dx - dy : dx - dy;
				int newScale = clamp(resizeStartScale + diagonal, 50, 200);
				ov.setPlaqueScale(newScale);
			} else {
				int scaledDx = (int) (dx / scale);
				int widthDelta = (activeDragHandle == 4) ? -scaledDx * 2 : scaledDx * 2;
				ov.setPlaqueWidth(clamp(resizeStartWidth + widthDelta, MIN_WIDTH, MAX_WIDTH));
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (iconPickerOpen) {
			int cols = 9;
			int visibleRows = (180 - 32) / 20;
			int totalRows = (ICON_ITEMS.length + cols - 1) / cols;
			int maxScroll = Math.max(0, totalRows - visibleRows);
			iconPickerScroll = clamp(iconPickerScroll - (int) amount, 0, maxScroll);
			return true;
		}
		if (mouseX >= 0 && mouseX < leftW) {
			int stepEntryH = 18;
			int listBottom = panelBottom - 20;
			int listTop = panelTop + 14;
			int maxVisible = (listBottom - listTop) / stepEntryH;
			int maxScroll = Math.max(0, ModConfig.getSteps().size() - maxVisible);
			stepListScroll = clamp(stepListScroll - (int) amount, 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (contextMenuOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			contextMenuOpen = false;
			return true;
		}
		if (listeningForKey) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				listeningForKey = false;
				rebuildWidgets();
				return true;
			}
			InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
			InstructionStep s = getSelectedStep();
			if (s != null) {
				s.setTargetKey(key.getTranslationKey());
				ModConfig.save();
			}
			listeningForKey = false;
			rebuildWidgets();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// ============================================================
	// Icon picker modal
	// ============================================================

	private void renderIconPicker(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
		ctx.fill(0, 0, width, height, 0xCC000000);

		int pw = 200, ph = 180;
		int px = width / 2 - pw / 2;
		int py = height / 2 - ph / 2;
		int cols = 9, cellSize = 20;

		ctx.fill(px - 3, py - 3, px + pw + 3, py + ph + 3, 0xFF111111);
		ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF555555);
		ctx.fill(px, py, px + pw, py + ph, 0xFF2A2A2A);

		ctx.drawCenteredTextWithShadow(tr, "Choose Icon", px + pw / 2, py + 4, 0xFFFFDD00);
		ctx.drawTextWithShadow(tr, "Scroll for more | Click outside to close", px + 4, py + ph - 10, 0x888888);

		int gridTop = py + 18;
		int gridH = ph - 32;
		int visibleRows = gridH / cellSize;

		for (int i = 0; i < ICON_ITEMS.length; i++) {
			int row = i / cols - iconPickerScroll;
			int col = i % cols;
			if (row < 0 || row >= visibleRows) continue;
			int ix = px + 4 + col * (cellSize + 1);
			int iy = gridTop + row * cellSize;
			boolean hovered = mouseX >= ix && mouseX < ix + cellSize && mouseY >= iy && mouseY < iy + cellSize;
			if (hovered) {
				ctx.fill(ix, iy, ix + cellSize, iy + cellSize, 0x44FFFFFF);
			}
			try {
				Item item = Registries.ITEM.get(new Identifier(ICON_ITEMS[i]));
				if (item != Items.AIR) {
					ctx.drawItem(new ItemStack(item), ix + 2, iy + 2);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
				}
			} catch (Exception ignored) {}
		}

		int totalRows = (ICON_ITEMS.length + cols - 1) / cols;
		if (totalRows > visibleRows) {
			int barH = Math.max(8, gridH * visibleRows / totalRows);
			int maxScr = totalRows - visibleRows;
			int barY = gridTop + (gridH - barH) * Math.min(iconPickerScroll, maxScr) / Math.max(1, maxScr);
			ctx.fill(px + pw - 4, barY, px + pw - 1, barY + barH, 0x88FFFFFF);
		}
	}

	private boolean handleIconPickerClick(int mx, int my) {
		int pw = 200, ph = 180;
		int px = width / 2 - pw / 2;
		int py = height / 2 - ph / 2;
		int cols = 9, cellSize = 20;
		int gridTop = py + 18;
		int gridH = ph - 32;
		int visibleRows = gridH / cellSize;

		for (int i = 0; i < ICON_ITEMS.length; i++) {
			int row = i / cols - iconPickerScroll;
			int col = i % cols;
			if (row < 0 || row >= visibleRows) continue;
			int ix = px + 4 + col * (cellSize + 1);
			int iy = gridTop + row * cellSize;
			if (mx >= ix && mx < ix + cellSize && my >= iy && my < iy + cellSize) {
				InstructionStep s = getSelectedStep();
				if (s != null) { s.setIcon(ICON_ITEMS[i]); ModConfig.save(); rebuildWidgets(); }
				iconPickerOpen = false;
				return true;
			}
		}
		return mx >= px && mx <= px + pw && my >= py && my <= py + ph;
	}

	// ============================================================
	// Context menu (inline edit)
	// ============================================================

	private int getContextMenuRows() {
		// Row 0: color pick, Row 1 (text targets): font, Row 2 (text targets): shadow, last: reset colors
		return (contextMenuTarget <= 1) ? 4 : 2;
	}

	private void renderContextMenu(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
		int menuW = 120, rowH = 16;
		int rows = getContextMenuRows();
		int menuH = rows * rowH + 4;
		int mx = contextMenuX, my = contextMenuY;

		ctx.fill(mx - 1, my - 1, mx + menuW + 1, my + menuH + 1, 0xFF444444);
		ctx.fill(mx, my, mx + menuW, my + menuH, 0xFF1A1A1A);

		StepVisualOverrides ov = getSelectedOverrides();
		int ry = my + 2;

		// Row 0: Color picker
		boolean hovered = mouseX >= mx && mouseX < mx + menuW && mouseY >= ry && mouseY < ry + rowH;
		if (hovered) ctx.fill(mx + 1, ry, mx + menuW - 1, ry + rowH, 0x44FFFFFF);
		String colorLabel;
		int previewColor;
		int defaultColor;
		boolean isTextureTint = false;
		switch (contextMenuTarget) {
			case 0:
				colorLabel = "Title Color";
				previewColor = ov.resolveTitleColor();
				defaultColor = 0xFFFFFF;
				break;
			case 1:
				colorLabel = "Desc Color";
				previewColor = ov.resolveDescriptionColor();
				defaultColor = 0xFFDD00;
				break;
			default:
				isTextureTint = "texture".equals(ov.resolvePlaqueStyle() != null
					? ("clean".equals(ov.resolvePlaqueStyle()) ? "solid" : ModConfig.getRenderMode())
					: ModConfig.getRenderMode());
				colorLabel = isTextureTint ? "Tint Color" : "BG Color";
				previewColor = ov.resolveBackgroundColor();
				defaultColor = 0xC6A050;
				break;
		}
		ctx.drawTextWithShadow(tr, colorLabel, mx + 4, ry + 4, 0xCCCCCC);
		// Default color swatch (dimmed) + current color swatch
		if (previewColor != defaultColor) {
			ctx.fill(mx + menuW - 30, ry + 4, mx + menuW - 21, ry + 12, 0xFF000000 | defaultColor);
			ctx.drawTextWithShadow(tr, "\u00a77\u2192", mx + menuW - 21, ry + 3, 0xCCCCCC);
		}
		ctx.fill(mx + menuW - 18, ry + 3, mx + menuW - 6, ry + 13, 0xFFFFFFFF);
		ctx.fill(mx + menuW - 17, ry + 4, mx + menuW - 7, ry + 12, 0xFF000000 | previewColor);

		if (contextMenuTarget <= 1) {
			// Row 1: Font
			ry = my + 2 + rowH;
			hovered = mouseX >= mx && mouseX < mx + menuW && mouseY >= ry && mouseY < ry + rowH;
			if (hovered) ctx.fill(mx + 1, ry, mx + menuW - 1, ry + rowH, 0x44FFFFFF);
			ctx.drawTextWithShadow(tr, "Font: " + InstructionRenderer.fontLabel(ov.resolveFontType()), mx + 4, ry + 4, 0xCCCCCC);

			// Row 2: Shadow
			ry = my + 2 + rowH * 2;
			hovered = mouseX >= mx && mouseX < mx + menuW && mouseY >= ry && mouseY < ry + rowH;
			if (hovered) ctx.fill(mx + 1, ry, mx + menuW - 1, ry + rowH, 0x44FFFFFF);
			boolean shadow = ov.resolveTextShadow();
			ctx.drawTextWithShadow(tr, "Shadow: " + (shadow ? "ON" : "OFF"), mx + 4, ry + 4, 0xCCCCCC);
		}

		// Last row: Reset Colors
		ry = my + 2 + rowH * (rows - 1);
		hovered = mouseX >= mx && mouseX < mx + menuW && mouseY >= ry && mouseY < ry + rowH;
		if (hovered) ctx.fill(mx + 1, ry, mx + menuW - 1, ry + rowH, 0x44FFFFFF);
		boolean hasOverride = ov.getTitleColor() != null || ov.getDescriptionColor() != null || ov.getBackgroundColor() != null;
		ctx.drawTextWithShadow(tr, hasOverride ? "\u00a7eReset Colors" : "\u00a78Reset Colors", mx + 4, ry + 4, 0xCCCCCC);
	}

	private boolean handleContextMenuClick(int mx, int my) {
		int menuW = 120, rowH = 16;
		int rows = getContextMenuRows();
		int menuH = rows * rowH + 4;

		if (mx < contextMenuX || mx >= contextMenuX + menuW
			|| my < contextMenuY || my >= contextMenuY + menuH) {
			return false;
		}

		int rowIdx = (my - contextMenuY - 2) / rowH;
		if (rowIdx < 0 || rowIdx >= rows) return false;

		if (rowIdx == 0) {
			openColorPickerForTarget();
			contextMenuOpen = false;
			return true;
		}
		if (contextMenuTarget <= 1 && rowIdx == 1) {
			StepVisualOverrides ov = getSelectedOverrides();
			ov.setFontType(InstructionRenderer.nextFontType(ov.resolveFontType()));
			ModConfig.save();
			contextMenuOpen = false;
			return true;
		}
		if (contextMenuTarget <= 1 && rowIdx == 2) {
			StepVisualOverrides ov = getSelectedOverrides();
			ov.setTextShadow(!ov.resolveTextShadow());
			ModConfig.save();
			contextMenuOpen = false;
			return true;
		}
		// Last row: Reset Colors
		if (rowIdx == rows - 1) {
			StepVisualOverrides ov = getSelectedOverrides();
			ov.setTitleColor(null);
			ov.setDescriptionColor(null);
			ov.setBackgroundColor(null);
			ModConfig.save();
			contextMenuOpen = false;
			return true;
		}
		return false;
	}

	private void openColorPickerForTarget() {
		StepVisualOverrides ov = getSelectedOverrides();
		int currentColor;
		IntConsumer onPick;

		switch (contextMenuTarget) {
			case 0:
				currentColor = ov.resolveTitleColor();
				onPick = c -> { getSelectedOverrides().setTitleColor(c); ModConfig.save(); };
				break;
			case 1:
				currentColor = ov.resolveDescriptionColor();
				onPick = c -> { getSelectedOverrides().setDescriptionColor(c); ModConfig.save(); };
				break;
			default:
				currentColor = ov.resolveBackgroundColor();
				onPick = c -> { getSelectedOverrides().setBackgroundColor(c); ModConfig.save(); };
				break;
		}

		client.setScreen(new ColorPickerPopup(this, currentColor, onPick));
	}

	// ============================================================
	// Helpers
	// ============================================================

	private void rebuildWidgets() {
		clearChildren();
		init();
	}

	private StepVisualOverrides getSelectedOverrides() {
		InstructionStep step = getSelectedStep();
		if (step == null) return new StepVisualOverrides();
		return ModConfig.getOverridesForStep(step.getId());
	}

	@Nullable
	private InstructionStep getSelectedStep() {
		List<InstructionStep> steps = ModConfig.getSteps();
		if (selectedStepIndex >= 0 && selectedStepIndex < steps.size()) return steps.get(selectedStepIndex);
		return null;
	}

	@Override
	public void removed() {
		// Always restore HUD when this screen goes away (close, test, etc.)
		client.options.hudHidden = wasHudHidden;
	}

	@Override
	public void close() {
		ModConfig.save();
		client.setScreen(parent);
	}

	@Override
	public void tick() {
		if (titleField != null) titleField.tick();
		if (descField != null) descField.tick();
		if (countField != null) countField.tick();

		long now = Util.getMeasuringTimeMs();
		timedMsgs.removeIf(tm -> {
			if (now >= tm.resetAt()) {
				tm.btn().setMessage(tm.original());
				return true;
			}
			return false;
		});
	}

	private static String getActionTypeLabel(ActionType type) {
		return switch (type) {
			case KEY_PRESS -> "Key Press";
			case KEY_HOLD -> "Key Hold";
			case WAIT_DURATION -> "Wait";
			case DISMISS -> "Dismiss";
		};
	}

	private static String getKeyDisplayName(String targetKey) {
		if (targetKey.startsWith("key.keyboard.")) {
			return targetKey.substring("key.keyboard.".length()).toUpperCase();
		}
		if (targetKey.startsWith("key.mouse.")) {
			return "Mouse " + targetKey.substring("key.mouse.".length());
		}
		return switch (targetKey) {
			case "forward" -> "W (Forward)";
			case "back" -> "S (Back)";
			case "left" -> "A (Left)";
			case "right" -> "D (Right)";
			case "jump" -> "Space (Jump)";
			case "sneak" -> "Shift (Sneak)";
			case "sprint" -> "Ctrl (Sprint)";
			case "inventory" -> "E (Inventory)";
			case "attack" -> "LMB (Attack)";
			case "use" -> "RMB (Use)";
			case "drop" -> "Q (Drop)";
			case "chat" -> "T (Chat)";
			default -> targetKey;
		};
	}

	private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

	private static class EditorSlider extends SliderWidget {
		private final int min, max;
		private final String prefix, suffix;
		private final IntConsumer onChange;

		EditorSlider(int x, int y, int w, int h, String prefix, String suffix, int min, int max, int current, IntConsumer onChange) {
			super(x, y, w, h, Text.empty(), clampD(current, min, max));
			this.min = min; this.max = max; this.prefix = prefix; this.suffix = suffix; this.onChange = onChange;
			updateMessage();
		}

		private static double clampD(int c, int min, int max) { return max <= min ? 0 : Math.max(0, Math.min(1, (double)(c - min) / (max - min))); }
		private int getVal() { return (int) Math.round(min + value * (max - min)); }
		@Override protected void updateMessage() { setMessage(Text.literal(prefix + getVal() + suffix)); }
		@Override protected void applyValue() { onChange.accept(getVal()); }
	}
}
