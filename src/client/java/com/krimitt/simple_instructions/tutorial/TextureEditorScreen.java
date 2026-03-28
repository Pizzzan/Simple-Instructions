package com.krimitt.simple_instructions.tutorial;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class TextureEditorScreen extends Screen {
	private final Screen parent;
	private String selectedStyle;
	private int presetScroll = 0;

	private int previewX, previewW, controlX, controlW, panelTop, panelBottom;
	private boolean didReload = false;

	private final List<TimedMessage> timedMessages = new ArrayList<>();
	private record TimedMessage(ButtonWidget btn, Text original, long resetAt) {}

	private void flashButton(ButtonWidget btn, String msg, int durationMs) {
		Text original = btn.getMessage();
		btn.setMessage(Text.literal(msg));
		timedMessages.add(new TimedMessage(btn, original, Util.getMeasuringTimeMs() + durationMs));
	}

	public TextureEditorScreen(Screen parent) {
		super(Text.literal("Texture Editor"));
		this.parent = parent;
		this.selectedStyle = ModConfig.getPlaqueStyle();
	}

	@Override
	protected void init() {
		if (!didReload) {
			didReload = true;
			PlaqueTextures.reload();
		}

		panelTop = 18;
		panelBottom = height - 28;
		int divider = (int) (width * 0.55);
		previewX = 0;
		previewW = divider;
		controlX = divider;
		controlW = width - divider;

		List<String> presets = PlaqueTextures.getPresetNames();
		int btnW = controlW - 16;
		int btnH = 20;
		int gap = 2;
		int listTop = panelTop + 16;
		int listBottom = panelBottom - 4;
		int maxVisible = (listBottom - listTop) / (btnH + gap);

		for (int i = 0; i < maxVisible && i + presetScroll < presets.size(); i++) {
			int idx = i + presetScroll;
			String preset = presets.get(idx);
			String label = PlaqueTextures.getDisplayName(preset);
			if (PlaqueTextures.isCustom(preset)) label = "\u2605 " + label;
			boolean isSelected = preset.equals(selectedStyle);
			if (isSelected) label = "> " + label;

			String presetCapture = preset;
			addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
				selectedStyle = presetCapture;
				clearAndInit();
			}).dimensions(controlX + 8, listTop + i * (btnH + gap), btnW, btnH).build());
		}

		if (!PlaqueTextures.isSolidMode(selectedStyle)) {
			int sliderW = Math.min(160, previewW - 40);
			int sliderX = previewX + previewW / 2 - sliderW / 2;
			int sliderY = panelBottom - 24;
			addDrawableChild(new IntSlider(sliderX, sliderY, sliderW, 16,
				"Border: ", "px", 1, 24, ModConfig.getNineSliceBorder(), v -> {
					ModConfig.setNineSliceBorder(v);
					ModConfig.save();
				}));
		}

		int bbY = height - 24;
		int bbBtnW = 80;
		int bbGap = 4;
		int totalW = bbBtnW * 4 + bbGap * 3;
		int bbX = width / 2 - totalW / 2;

		addDrawableChild(ButtonWidget.builder(Text.literal("Apply to All"), btn -> {
			ModConfig.setPlaqueStyle(selectedStyle);
			if (PlaqueTextures.isSolidMode(selectedStyle)) {
				ModConfig.setRenderMode("solid");
			} else {
				ModConfig.setRenderMode("texture");
			}
			ModConfig.save();
			flashButton(btn, "\u00a7aApplied!", 2000);
		}).dimensions(bbX, bbY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Reset All"), btn -> {
			if (btn.getMessage().getString().contains("sure")) {
				selectedStyle = "default";
				ModConfig.setPlaqueStyle("default");
				ModConfig.setRenderMode("texture");
				ModConfig.save();
				clearAndInit();
			} else {
				btn.setMessage(Text.literal("\u00a7cAre you sure?"));
				timedMessages.add(new TimedMessage(btn, Text.literal("Reset All"), Util.getMeasuringTimeMs() + 3000));
			}
		}).dimensions(bbX + bbBtnW + bbGap, bbY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Guide"), btn -> {
			client.setScreen(new TextureGuideScreen(this));
		}).dimensions(bbX + (bbBtnW + bbGap) * 2, bbY, bbBtnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
			.dimensions(bbX + (bbBtnW + bbGap) * 3, bbY, bbBtnW, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);

		TextRenderer tr = client.textRenderer;

		ctx.fill(previewX, panelTop, previewX + previewW, panelBottom, 0x44000000);

		int plaqueW = Math.min(280, previewW - 20);
		int plaqueH = InstructionRenderer.getBaseHeight();
		int px = previewX + previewW / 2 - plaqueW / 2;
		int py = panelTop + (panelBottom - panelTop) / 2 - plaqueH / 2 - 10;

		InstructionRenderer.drawParchmentBackground(ctx, px, py, plaqueW, plaqueH, 1.0f,
			ModConfig.getBackgroundColor(), selectedStyle);

		String sampleTitle = "Sample Title";
		String sampleDesc = "This is how it looks";
		int textCx = px + plaqueW / 2;
		ctx.drawCenteredTextWithShadow(tr, sampleTitle, textCx, py + 8, 0xFFFFFFFF);
		ctx.drawCenteredTextWithShadow(tr, sampleDesc, textCx, py + 21, 0xFFFFDD00);

		int barX = px + 6;
		int barY = py + plaqueH - 6;
		int barW = plaqueW - 12;
		ctx.fill(barX, barY, barX + barW, barY + 2, 0xFF302410);
		ctx.fill(barX, barY, barX + (int)(barW * 0.6f), barY + 2, 0xFF000000 | ModConfig.getBarColor());

		String styleName = PlaqueTextures.getDisplayName(selectedStyle);
		ctx.drawCenteredTextWithShadow(tr, "Current: " + styleName, previewX + previewW / 2, py + plaqueH + 14, 0xFFCCCCCC);

		ctx.fill(controlX, panelTop, width, panelBottom, 0x44000000);
		ctx.drawTextWithShadow(tr, "Presets", controlX + 8, panelTop + 4, 0xFFFFDD00);

		List<String> presets = PlaqueTextures.getPresetNames();
		int btnH = 20;
		int gap = 2;
		int listTop = panelTop + 16;
		int listBottom = panelBottom - 4;
		int maxVisible = (listBottom - listTop) / (btnH + gap);
		if (presets.size() > maxVisible) {
			int barTotalH = listBottom - listTop;
			int scrollBarH = Math.max(6, barTotalH * maxVisible / presets.size());
			int maxScr = Math.max(1, presets.size() - maxVisible);
			int scrollBarY = listTop + (barTotalH - scrollBarH) * presetScroll / maxScr;
			ctx.fill(width - 4, scrollBarY, width - 1, scrollBarY + scrollBarH, 0x66FFFFFF);
		}

		ctx.drawTextWithShadow(tr, "Drop PNGs in textures folder", controlX + 8, panelBottom - 14, 0x666666);

		ctx.drawCenteredTextWithShadow(tr, title, width / 2, 4, 0xFFFFDD00);

		ctx.fill(0, height - 28, width, height, 0xFF1A1A1A);

		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (mouseX >= controlX) {
			List<String> presets = PlaqueTextures.getPresetNames();
			int btnH = 20;
			int gap = 2;
			int listTop = panelTop + 16;
			int listBottom = panelBottom - 4;
			int maxVisible = (listBottom - listTop) / (btnH + gap);
			int maxScroll = Math.max(0, presets.size() - maxVisible);
			int newScroll = presetScroll - (int) amount;
			if (newScroll >= 0 && newScroll <= maxScroll) {
				presetScroll = newScroll;
				clearAndInit();
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void tick() {
		long now = Util.getMeasuringTimeMs();
		timedMessages.removeIf(tm -> {
			if (now >= tm.resetAt()) {
				tm.btn().setMessage(tm.original());
				return true;
			}
			return false;
		});
	}

	@Override
	public void close() {
		client.setScreen(parent);
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
