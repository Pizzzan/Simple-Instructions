package com.krimitt.simple_instructions.tutorial;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class SimpleInstructionsConfigScreen extends Screen {
	private enum Tab { GENERAL, APPEARANCE, COLORS, EFFECTS }

	private final Screen parent;
	private Tab currentTab = Tab.GENERAL;
	private final List<LabelInfo> labels = new ArrayList<>();

	private int activeColorRow = 0;
	private final IntConsumer[] colorSetters = new IntConsumer[5];
	private final TextFieldWidget[] colorFields = new TextFieldWidget[5];
	private int swatchScroll = 0;
	private int swatchGridY = 0;

	private final List<TimedMsg> timedMsgs = new ArrayList<>();
	private record TimedMsg(ButtonWidget btn, Text original, long resetAt) {}

	private void flashButton(ButtonWidget btn, String msg, int durationMs) {
		Text original = btn.getMessage();
		btn.setMessage(Text.literal(msg).formatted(Formatting.GREEN));
		timedMsgs.add(new TimedMsg(btn, original, Util.getMeasuringTimeMs() + durationMs));
	}

	private record LabelInfo(String text, int x, int y) {}

	public SimpleInstructionsConfigScreen(Screen parent) {
		super(Text.literal("Simple Instructions Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		labels.clear();

		int centerX = width / 2;
		int tabY = 32;
		int tabWidth = 72;
		int tabGap = 2;
		int totalTabWidth = tabWidth * Tab.values().length + tabGap * (Tab.values().length - 1);
		int tabStartX = centerX - totalTabWidth / 2;

		for (int i = 0; i < Tab.values().length; i++) {
			Tab tab = Tab.values()[i];
			int tx = tabStartX + i * (tabWidth + tabGap);
			Text label = (tab == currentTab)
				? Text.literal(tabName(tab)).formatted(Formatting.YELLOW, Formatting.UNDERLINE)
				: Text.literal(tabName(tab));
			addDrawableChild(ButtonWidget.builder(label, btn -> switchTab(tab))
				.dimensions(tx, tabY, tabWidth, 20).build());
		}

		int contentY = 60;
		switch (currentTab) {
			case GENERAL -> buildGeneralTab(centerX, contentY);
			case APPEARANCE -> buildAppearanceTab(centerX, contentY);
			case COLORS -> buildColorsTab(centerX, contentY);
			case EFFECTS -> buildEffectsTab(centerX, contentY);
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
			.dimensions(centerX - 100, height - 28, 200, 20).build());
	}

	private String tabName(Tab tab) {
		return switch (tab) {
			case GENERAL -> "General";
			case APPEARANCE -> "Appearance";
			case COLORS -> "Colors";
			case EFFECTS -> "Effects";
		};
	}

	private void switchTab(Tab tab) {
		currentTab = tab;
		clearChildren();
		init();
	}

	private void buildGeneralTab(int centerX, int startY) {
		int w = 200, x = centerX - w / 2;

		var tutorialBtn = addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isTutorialEnabled())
			.build(x, startY, w, 20, Text.literal("Tutorial"), (btn, val) -> {
				ModConfig.setTutorialEnabled(val);
				ModConfig.save();
			}));
		tutorialBtn.setTooltip(Tooltip.of(Text.literal(
			"Enable or disable the tutorial overlay.\nWhen OFF, no instructions will appear.")));

		var joinBtn = addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isShowOnWorldJoin())
			.build(x, startY + 24, w, 20, Text.literal("Show on Join"), (btn, val) -> {
				ModConfig.setShowOnWorldJoin(val);
				ModConfig.save();
			}));
		joinBtn.setTooltip(Tooltip.of(Text.literal(
			"Automatically show the tutorial when joining a world.\nDisable to only trigger manually.")));

		var skipBtn = addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isSkippable())
			.build(x, startY + 48, w, 20, Text.literal("Skippable"), (btn, val) -> {
				ModConfig.setSkippable(val);
				ModConfig.save();
			}));
		skipBtn.setTooltip(Tooltip.of(Text.literal(
			"Allow players to skip the tutorial by pressing X.\nWhen OFF, the skip option is hidden.")));

		var resetBtn = addDrawableChild(ButtonWidget.builder(
				Text.literal("Reset Tutorial Progress"), btn -> {
			CompletionPersistence.resetAll();
			flashButton(btn, "Progress Reset!", 2000);
		}).dimensions(x, startY + 72, w, 20).build());
		resetBtn.setTooltip(Tooltip.of(Text.literal(
			"Clear all completion data.\nThe tutorial will show again on next world join.")));

		var defaultsBtn = addDrawableChild(ButtonWidget.builder(
				Text.literal("Reset All to Defaults"), btn -> {
			ModConfig.resetToDefaults();
			switchTab(currentTab);
		}).dimensions(x, startY + 96, w, 20).build());
		defaultsBtn.setTooltip(Tooltip.of(Text.literal(
			"Reset ALL settings back to their default values.\nThis cannot be undone.")));

		var resetStepsBtn = addDrawableChild(ButtonWidget.builder(
				Text.literal("Reset Steps to Defaults"), btn -> {
			ModConfig.resetStepsToDefaults();
			flashButton(btn, "Steps Reset!", 2000);
		}).dimensions(x, startY + 120, w, 20).build());
		resetStepsBtn.setTooltip(Tooltip.of(Text.literal(
			"Reset all instruction steps back to defaults.\nAny custom steps will be lost.")));

		var customizeBtn = addDrawableChild(ButtonWidget.builder(
				Text.literal("Customize Textures..."), btn -> {
			client.setScreen(new TextureEditorScreen(this));
		}).dimensions(x, startY + 144, w, 20).build());
		customizeBtn.setTooltip(Tooltip.of(Text.literal(
			"Open the texture editor to change the\nplaque's visual style.")));
	}

	private void buildAppearanceTab(int centerX, int startY) {
		int w = 200, x = centerX - w / 2;
		int halfW = w / 2 - 1;
		int row = 0;
		int gap = 22;

		addDrawableChild(new IntSlider(x, startY + row * gap, w, 20,
			"Width: ", " px", 160, 320, ModConfig.getPlaqueWidth(), v -> {
				ModConfig.setPlaqueWidth(v); ModConfig.save();
			}));
		row++;

		addDrawableChild(new IntSlider(x, startY + row * gap, w, 20,
			"Scale: ", "%", 50, 200, ModConfig.getPlaqueScale(), v -> {
				ModConfig.setPlaqueScale(v); ModConfig.save();
			}));
		row++;

		addDrawableChild(new IntSlider(x, startY + row * gap, w, 20,
			"Position X: ", "%", 0, 100, ModConfig.getPositionXPercent(), v -> {
				ModConfig.setPositionXPercent(v); ModConfig.save();
			}));
		row++;

		addDrawableChild(new IntSlider(x, startY + row * gap, w, 20,
			"Position Y: ", " px", 0, 500, ModConfig.getPositionY(), v -> {
				ModConfig.setPositionY(v); ModConfig.save();
			}));
		row++;

		addDrawableChild(new IntSlider(x, startY + row * gap, halfW, 20,
			"Opacity: ", "%", 0, 100, ModConfig.getBackgroundOpacity(), v -> {
				ModConfig.setBackgroundOpacity(v); ModConfig.save();
			}));

		boolean isTexture = "texture".equals(ModConfig.getRenderMode());
		addDrawableChild(CyclingButtonWidget.<Boolean>builder(val ->
				val ? Text.literal("Texture").formatted(Formatting.AQUA)
					: Text.literal("Solid").formatted(Formatting.WHITE))
			.values(false, true)
			.initially(isTexture)
			.build(x + halfW + 2, startY + row * gap, halfW, 20, Text.literal("Render"), (btn, val) -> {
				ModConfig.setRenderMode(val ? "texture" : "solid"); ModConfig.save();
			}));
		row++;

		addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isShowIcon())
			.build(x, startY + row * gap, halfW, 20, Text.literal("Icon"), (btn, val) -> {
				ModConfig.setShowIcon(val); ModConfig.save();
			}));

		var iconField = addDrawableChild(new TextFieldWidget(
			textRenderer, x + halfW + 2, startY + row * gap, halfW, 20, Text.literal("Icon")));
		iconField.setMaxLength(64);
		iconField.setText(ModConfig.getDefaultIconItem());
		iconField.setChangedListener(text -> {
			ModConfig.setDefaultIconItem(text); ModConfig.save();
		});
		row++;

		addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isTextShadow())
			.build(x, startY + row * gap, halfW, 20, Text.literal("Shadow"), (btn, val) -> {
				ModConfig.setTextShadow(val); ModConfig.save();
			}));

		String currentFont = ModConfig.getFontType();
		int fontIdx = 0;
		for (int i = 0; i < InstructionRenderer.FONT_TYPES.length; i++) {
			if (InstructionRenderer.FONT_TYPES[i].equals(currentFont)) { fontIdx = i; break; }
		}
		addDrawableChild(CyclingButtonWidget.<Integer>builder(val ->
				Text.literal(InstructionRenderer.FONT_LABELS[val]).formatted(Formatting.AQUA))
			.values(buildFontIndices())
			.initially(fontIdx)
			.build(x + halfW + 2, startY + row * gap, halfW, 20, Text.literal("Font"), (btn, val) -> {
				ModConfig.setFontType(InstructionRenderer.FONT_TYPES[val]); ModConfig.save();
			}));
	}

	private static java.util.List<Integer> buildFontIndices() {
		java.util.List<Integer> list = new ArrayList<>();
		for (int i = 0; i < InstructionRenderer.FONT_TYPES.length; i++) list.add(i);
		return list;
	}

	private static final int[] COLOR_PRESETS = {
		0xFFFFFF, 0xDDDDDD, 0xAAAAAA, 0x777777, 0x555555, 0x333333, 0x111111, 0x000000, 0x000000,
		0xFFCCCC, 0xFF8888, 0xFF4444, 0xFF0000, 0xCC0000, 0x990000, 0x660000, 0x440000, 0x330000,
		0xFFDDAA, 0xFFBB66, 0xFF9933, 0xFF7700, 0xCC5500, 0x994400, 0x663300, 0x442200, 0x331100,
		0xFFFFAA, 0xFFFF66, 0xFFFF00, 0xFFDD00, 0xDDBB00, 0xAA8800, 0x886600, 0x665500, 0x443300,
		0xCCFFCC, 0x88FF88, 0x44FF44, 0x00FF00, 0x00CC00, 0x009900, 0x006600, 0x004400, 0x003300,
		0xCCFFFF, 0x88FFFF, 0x44FFFF, 0x00FFFF, 0x00CCCC, 0x009999, 0x006666, 0x004444, 0x003333,
		0xCCCCFF, 0x8888FF, 0x4444FF, 0x0000FF, 0x0000CC, 0x000099, 0x000066, 0x000044, 0x000033,
		0xEECCFF, 0xDD88FF, 0xCC44FF, 0xAA00FF, 0x8800CC, 0x660099, 0x440066, 0x330044, 0x220033,
		0xFFCCEE, 0xFF88DD, 0xFF44CC, 0xFF00FF, 0xCC00CC, 0x990099, 0x660066, 0x440044, 0x330033,
	};
	private static final int PRESETS_PER_ROW = 9;

	private void buildColorsTab(int centerX, int startY) {
		int w = 200, x = centerX - w / 2;

		boolean isTextureMode = "texture".equals(ModConfig.getRenderMode()) && !"clean".equals(ModConfig.getPlaqueStyle());
		String bgLabel = isTextureMode ? "Tint:" : "Fill:";
		String[] rowLabels = {"Title:", "Desc:", "Bar:", "Done:", bgLabel};
		int[] rowColors = {
			ModConfig.getTitleColor(), ModConfig.getDescriptionColor(),
			ModConfig.getBarColor(), ModConfig.getBarCompleteColor(), ModConfig.getBackgroundColor()
		};
		IntConsumer[] setters = {
			v -> { ModConfig.setTitleColor(v); ModConfig.save(); },
			v -> { ModConfig.setDescriptionColor(v); ModConfig.save(); },
			v -> { ModConfig.setBarColor(v); ModConfig.save(); },
			v -> { ModConfig.setBarCompleteColor(v); ModConfig.save(); },
			v -> { ModConfig.setBackgroundColor(v); ModConfig.save(); },
		};

		int rowH = 22;
		for (int i = 0; i < 5; i++) {
			int ry = startY + i * rowH;
			addCompactColorRow(x, ry, w, rowLabels[i], rowColors[i], setters[i], i);
		}

		int gridTop = startY + 5 * rowH + 6;
		swatchGridY = gridTop;
		int cellSize = 12;
		int gridW = PRESETS_PER_ROW * cellSize;
		int gridX = centerX - gridW / 2;
		int totalRows = COLOR_PRESETS.length / PRESETS_PER_ROW;
		int availableH = (height - 32) - gridTop;
		int visibleRows = Math.min(totalRows, Math.max(3, availableH / cellSize));

		for (int row = 0; row < visibleRows; row++) {
			for (int col = 0; col < PRESETS_PER_ROW; col++) {
				int idx = (row + swatchScroll) * PRESETS_PER_ROW + col;
				if (idx >= COLOR_PRESETS.length) break;
				int presetColor = COLOR_PRESETS[idx];
				int bx = gridX + col * cellSize;
				int by = gridTop + row * cellSize;
				addDrawableChild(ButtonWidget.builder(Text.literal(""), btn -> {
					IntConsumer setter = colorSetters[activeColorRow];
					TextFieldWidget field = colorFields[activeColorRow];
					if (setter != null) setter.accept(presetColor);
					if (field != null) field.setText(String.format("#%06X", presetColor & 0xFFFFFF));
				}).dimensions(bx, by, cellSize - 1, cellSize - 1).build());
			}
		}
	}

	private void addCompactColorRow(int x, int y, int totalWidth, String label, int color,
									IntConsumer onChange, int rowIndex) {
		int labelWidth = 36;
		int previewSize = 14;
		colorSetters[rowIndex] = onChange;

		addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
			activeColorRow = rowIndex;
		}).dimensions(x, y, labelWidth, 18).build());

		var field = addDrawableChild(new TextFieldWidget(
			textRenderer, x + labelWidth + previewSize + 6, y, 60, 18, Text.literal(label)));
		field.setMaxLength(7);
		field.setText(String.format("#%06X", color & 0xFFFFFF));
		field.setChangedListener(text -> {
			activeColorRow = rowIndex;
			try {
				String hex = text.startsWith("#") ? text.substring(1) : text;
				if (hex.length() == 6) {
					int parsed = Integer.parseInt(hex, 16) & 0xFFFFFF;
					onChange.accept(parsed);
					field.setEditableColor(0xE0E0E0);
				}
			} catch (NumberFormatException e) {
				field.setEditableColor(0xFF5555);
			}
		});
		colorFields[rowIndex] = field;
	}

	private void renderColorPresets(DrawContext context, int startY) {
		int x = width / 2 - 100;
		int labelWidth = 36;
		int previewSize = 14;
		int rowH = 22;

		int[] currentColors = {
			ModConfig.getTitleColor(), ModConfig.getDescriptionColor(),
			ModConfig.getBarColor(), ModConfig.getBarCompleteColor(), ModConfig.getBackgroundColor()
		};

		for (int i = 0; i < 5; i++) {
			int ry = startY + i * rowH;
			int px = x + labelWidth + 2;
			int py = ry + 2;

			if (i == activeColorRow) {
				context.fill(x - 2, ry - 1, x + 200 + 2, ry + 19, 0x33FFFF00);
			}

			context.fill(px - 1, py - 1, px + previewSize + 1, py + previewSize + 1, 0xFF000000);
			context.fill(px, py, px + previewSize, py + previewSize, 0xFF000000 | currentColors[i]);
		}

		int cellSize = 12;
		int gridW = PRESETS_PER_ROW * cellSize;
		int gridX = width / 2 - gridW / 2;
		int totalRows = COLOR_PRESETS.length / PRESETS_PER_ROW;
		int availableH = (height - 32) - swatchGridY;
		int visibleRows = Math.min(totalRows, Math.max(3, availableH / cellSize));

		for (int row = 0; row < visibleRows; row++) {
			for (int col = 0; col < PRESETS_PER_ROW; col++) {
				int idx = (row + swatchScroll) * PRESETS_PER_ROW + col;
				if (idx >= COLOR_PRESETS.length) break;
				int bx = gridX + col * cellSize;
				int by = swatchGridY + row * cellSize;
				context.fill(bx + 1, by + 1, bx + cellSize - 2, by + cellSize - 2, 0xFF000000 | COLOR_PRESETS[idx]);
			}
		}

		if (totalRows > visibleRows) {
			int barX = gridX + gridW + 2;
			int barTotalH = visibleRows * cellSize;
			int barH = Math.max(6, barTotalH * visibleRows / totalRows);
			int maxScr = totalRows - visibleRows;
			int barY = swatchGridY + (barTotalH - barH) * swatchScroll / Math.max(1, maxScr);
			context.fill(barX, barY, barX + 3, barY + barH, 0x66FFFFFF);
		}
	}

	private void buildEffectsTab(int centerX, int startY) {
		int w = 200, x = centerX - w / 2;
		int row = 0;

		var revealSlider = addDrawableChild(new IntSlider(x, startY + row * 24, w, 20,
			"Reveal: ", " ms", 200, 2000, ModConfig.getRevealDurationMs(), v -> {
				ModConfig.setRevealDurationMs(v); ModConfig.save();
			}));
		revealSlider.setTooltip(Tooltip.of(Text.literal(
			"Duration of the plaque appear animation.\nDefault: 1000 ms")));
		row++;

		var dismissSlider = addDrawableChild(new IntSlider(x, startY + row * 24, w, 20,
			"Dismiss: ", " ms", 200, 1000, ModConfig.getDismissDurationMs(), v -> {
				ModConfig.setDismissDurationMs(v); ModConfig.save();
			}));
		dismissSlider.setTooltip(Tooltip.of(Text.literal(
			"Duration of the plaque disappear animation.\nDefault: 400 ms")));
		row++;

		var completingSlider = addDrawableChild(new IntSlider(x, startY + row * 24, w, 20,
			"Complete: ", " ms", 500, 3000, ModConfig.getCompletingDurationMs(), v -> {
				ModConfig.setCompletingDurationMs(v); ModConfig.save();
			}));
		completingSlider.setTooltip(Tooltip.of(Text.literal(
			"How long the completion state shows before dismissing.\nDefault: 1000 ms")));
		row++;

		var flashBtn = addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isEnableFlash())
			.build(x, startY + row * 24, w, 20, Text.literal("Flash Effect"), (btn, val) -> {
				ModConfig.setEnableFlash(val); ModConfig.save();
			}));
		flashBtn.setTooltip(Tooltip.of(Text.literal(
			"White flash when the plaque appears.\nInspired by the Advancement Plaques mod.")));
		row++;

		var flashSlider = addDrawableChild(new IntSlider(x, startY + row * 24, w, 20,
			"Flash Power: ", "%", 0, 100, ModConfig.getFlashIntensity(), v -> {
				ModConfig.setFlashIntensity(v); ModConfig.save();
			}));
		flashSlider.setTooltip(Tooltip.of(Text.literal(
			"Brightness of the flash effect.\n0% = invisible, 100% = full white.")));
		row++;

		var soundBtn = addDrawableChild(CyclingButtonWidget.onOffBuilder(
				Text.literal("ON").formatted(Formatting.GREEN),
				Text.literal("OFF").formatted(Formatting.RED))
			.initially(ModConfig.isEnableCompletionSound())
			.build(x, startY + row * 24, w, 20, Text.literal("Sound"), (btn, val) -> {
				ModConfig.setEnableCompletionSound(val); ModConfig.save();
			}));
		soundBtn.setTooltip(Tooltip.of(Text.literal(
			"Play a sound when an instruction step is completed.")));
		row++;

		var volumeSlider = addDrawableChild(new IntSlider(x, startY + row * 24, w, 20,
			"Volume: ", "%", 0, 100, ModConfig.getSoundVolume(), v -> {
				ModConfig.setSoundVolume(v); ModConfig.save();
			}));
		volumeSlider.setTooltip(Tooltip.of(Text.literal(
			"Volume of the completion sound.\n0% = muted, 100% = full volume.")));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (currentTab == Tab.COLORS && mouseY >= swatchGridY) {
			int totalRows = COLOR_PRESETS.length / PRESETS_PER_ROW;
			int availableH = (height - 32) - swatchGridY;
			int visibleRows = Math.min(totalRows, Math.max(3, availableH / 12));
			int maxScroll = Math.max(0, totalRows - visibleRows);
			int newScroll = swatchScroll - (int) amount;
			if (newScroll >= 0 && newScroll <= maxScroll && newScroll != swatchScroll) {
				swatchScroll = newScroll;
				clearChildren();
				init();
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public void tick() {
		children().forEach(child -> {
			if (child instanceof TextFieldWidget tf) {
				tf.tick();
			}
		});

		long now = Util.getMeasuringTimeMs();
		timedMsgs.removeIf(tm -> {
			if (now >= tm.resetAt()) {
				tm.btn().setMessage(tm.original());
				return true;
			}
			return false;
		});
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15,
			0xFFFFDD00);

		for (LabelInfo label : labels) {
			context.drawTextWithShadow(textRenderer, label.text, label.x, label.y, 0xAAAAAA);
		}

		super.render(context, mouseX, mouseY, delta);

		if (currentTab == Tab.COLORS) {
			renderColorPresets(context, 60);
		}
	}

	private static class IntSlider extends SliderWidget {
		private final int min, max;
		private final String prefix, suffix;
		private final IntConsumer onChange;

		IntSlider(int x, int y, int w, int h, String prefix, String suffix,
				  int min, int max, int current, IntConsumer onChange) {
			super(x, y, w, h, Text.empty(), clampVal(current, min, max));
			this.min = min;
			this.max = max;
			this.prefix = prefix;
			this.suffix = suffix;
			this.onChange = onChange;
			updateMessage();
		}

		private static double clampVal(int current, int min, int max) {
			if (max <= min) return 0;
			return Math.max(0, Math.min(1, (double) (current - min) / (max - min)));
		}

		private int getVal() {
			return (int) Math.round(min + value * (max - min));
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(prefix + getVal() + suffix));
		}

		@Override
		protected void applyValue() {
			onChange.accept(getVal());
		}
	}
}
