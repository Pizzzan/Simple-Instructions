package com.krimitt.simple_instructions.tutorial;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class ColorPickerPopup extends Screen {
	private static final int PANEL_W = 200;
	private static final int PANEL_H = 210;

	private static final int[] GRID_COLORS = generateColorGrid();
	private static final int GRID_COLS = 10;
	private static final int GRID_CELL = 14;

	private float hue = 0f, sat = 1f, val = 1f;
	private int currentColor;
	private boolean hsvMode = false;
	private boolean draggingHue = false;
	private boolean draggingSV = false;

	private final Screen parent;
	private final IntConsumer onColorPicked;
	private TextFieldWidget hexField;

	
	private int px, py;

	public ColorPickerPopup(Screen parent, int initialColor, IntConsumer onColorPicked) {
		super(Text.literal("Color Picker"));
		this.parent = parent;
		this.currentColor = initialColor & 0xFFFFFF;
		this.onColorPicked = onColorPicked;
		float[] hsv = rgbToHsv(currentColor);
		hue = hsv[0]; sat = hsv[1]; val = hsv[2];
	}

	@Override
	protected void init() {
		px = width / 2 - PANEL_W / 2;
		py = height / 2 - PANEL_H / 2;

		hexField = addDrawableChild(new TextFieldWidget(
			textRenderer, px + 4, py + PANEL_H - 50, 70, 16, Text.literal("Hex")));
		hexField.setMaxLength(7);
		hexField.setText(String.format("#%06X", currentColor));
		hexField.setChangedListener(text -> {
			try {
				String hex = text.startsWith("#") ? text.substring(1) : text;
				if (hex.length() == 6) {
					currentColor = Integer.parseInt(hex, 16) & 0xFFFFFF;
					float[] hsv = rgbToHsv(currentColor);
					hue = hsv[0]; sat = hsv[1]; val = hsv[2];
					hexField.setEditableColor(0xE0E0E0);
				}
			} catch (NumberFormatException e) {
				hexField.setEditableColor(0xFF5555);
			}
		});

		addDrawableChild(ButtonWidget.builder(
			Text.literal(hsvMode ? "Grid" : "HSV"), btn -> {
				hsvMode = !hsvMode;
				btn.setMessage(Text.literal(hsvMode ? "Grid" : "HSV"));
			}).dimensions(px + 78, py + PANEL_H - 50, 36, 16).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
			onColorPicked.accept(currentColor);
			client.setScreen(parent);
		}).dimensions(px + 118, py + PANEL_H - 50, 36, 16).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
			client.setScreen(parent);
		}).dimensions(px + 158, py + PANEL_H - 50, 20, 16).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		
		ctx.fill(0, 0, width, height, 0xCC000000);

		
		ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, 0xFF555555);
		ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF1A1A1A);

		
		ctx.drawCenteredTextWithShadow(textRenderer, "Pick a Color", px + PANEL_W / 2, py + 4, 0xFFFFDD00);

		int prevX = px + PANEL_W - 24, prevY = py + PANEL_H - 48;
		ctx.fill(prevX - 1, prevY - 1, prevX + 15, prevY + 15, 0xFFFFFFFF);
		ctx.fill(prevX, prevY, prevX + 14, prevY + 14, 0xFF000000 | currentColor);

		if (hsvMode) {
			renderHSV(ctx, mouseX, mouseY);
		} else {
			renderGrid(ctx, mouseX, mouseY);
		}

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void renderGrid(DrawContext ctx, int mouseX, int mouseY) {
		int gridX = px + 10;
		int gridY = py + 18;

		for (int i = 0; i < GRID_COLORS.length; i++) {
			int col = i % GRID_COLS;
			int row = i / GRID_COLS;
			int cx = gridX + col * (GRID_CELL + 1);
			int cy = gridY + row * (GRID_CELL + 1);

			boolean hovered = mouseX >= cx && mouseX < cx + GRID_CELL
				&& mouseY >= cy && mouseY < cy + GRID_CELL;

			if (hovered) {
				ctx.fill(cx - 1, cy - 1, cx + GRID_CELL + 1, cy + GRID_CELL + 1, 0xFFFFFFFF);
			}
			ctx.fill(cx, cy, cx + GRID_CELL, cy + GRID_CELL, 0xFF000000 | GRID_COLORS[i]);
		}
	}

	private void renderHSV(DrawContext ctx, int mouseX, int mouseY) {
		int hueX = px + 10, hueY = py + 18, hueW = 180, hueH = 14;
		int svX = px + 10, svY = py + 38, svW = 180, svH = 110;

		
		for (int i = 0; i < hueW; i++) {
			float h = (float) i / hueW;
			int c = hsvToRgb(h, 1f, 1f);
			ctx.fill(hueX + i, hueY, hueX + i + 1, hueY + hueH, 0xFF000000 | c);
		}
		
		int hueMarkerX = hueX + (int) (hue * hueW);
		ctx.fill(hueMarkerX - 1, hueY - 1, hueMarkerX + 1, hueY + hueH + 1, 0xFFFFFFFF);

		
		int blockSize = 4;
		for (int sy = 0; sy < svH; sy += blockSize) {
			for (int sx = 0; sx < svW; sx += blockSize) {
				float s = (float) sx / svW;
				float v = 1f - (float) sy / svH;
				int c = hsvToRgb(hue, s, v);
				int bx = svX + sx, by = svY + sy;
				int bx2 = Math.min(bx + blockSize, svX + svW);
				int by2 = Math.min(by + blockSize, svY + svH);
				ctx.fill(bx, by, bx2, by2, 0xFF000000 | c);
			}
		}
		
		int svMarkerX = svX + (int) (sat * svW);
		int svMarkerY = svY + (int) ((1f - val) * svH);
		ctx.fill(svMarkerX - 4, svMarkerY, svMarkerX + 4, svMarkerY + 1, 0xFFFFFFFF);
		ctx.fill(svMarkerX, svMarkerY - 4, svMarkerX + 1, svMarkerY + 4, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

		if (hsvMode) {
			int hueX = px + 10, hueY = py + 18, hueW = 180, hueH = 14;
			int svX = px + 10, svY = py + 38, svW = 180, svH = 110;

			if (mouseX >= hueX && mouseX < hueX + hueW && mouseY >= hueY && mouseY < hueY + hueH) {
				draggingHue = true;
				hue = clampF((float) (mouseX - hueX) / hueW, 0f, 1f);
				updateColorFromHSV();
				return true;
			}
			if (mouseX >= svX && mouseX < svX + svW && mouseY >= svY && mouseY < svY + svH) {
				draggingSV = true;
				sat = clampF((float) (mouseX - svX) / svW, 0f, 1f);
				val = clampF(1f - (float) (mouseY - svY) / svH, 0f, 1f);
				updateColorFromHSV();
				return true;
			}
		} else {
			int gridX = px + 10, gridY = py + 18;
			for (int i = 0; i < GRID_COLORS.length; i++) {
				int col = i % GRID_COLS;
				int row = i / GRID_COLS;
				int cx = gridX + col * (GRID_CELL + 1);
				int cy = gridY + row * (GRID_CELL + 1);
				if (mouseX >= cx && mouseX < cx + GRID_CELL && mouseY >= cy && mouseY < cy + GRID_CELL) {
					currentColor = GRID_COLORS[i];
					float[] hsv = rgbToHsv(currentColor);
					hue = hsv[0]; sat = hsv[1]; val = hsv[2];
					hexField.setText(String.format("#%06X", currentColor));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
		if (draggingHue) {
			int hueX = px + 10, hueW = 180;
			hue = clampF((float) (mouseX - hueX) / hueW, 0f, 1f);
			updateColorFromHSV();
			return true;
		}
		if (draggingSV) {
			int svX = px + 10, svY = py + 38, svW = 180, svH = 110;
			sat = clampF((float) (mouseX - svX) / svW, 0f, 1f);
			val = clampF(1f - (float) (mouseY - svY) / svH, 0f, 1f);
			updateColorFromHSV();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dX, dY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingHue = false;
		draggingSV = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void updateColorFromHSV() {
		currentColor = hsvToRgb(hue, sat, val);
		hexField.setText(String.format("#%06X", currentColor));
	}

	static int hsvToRgb(float h, float s, float v) {
		float c = v * s;
		float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
		float m = v - c;
		float r, g, b;
		int hi = (int) (h * 6f) % 6;
		switch (hi) {
			case 0 -> { r = c; g = x; b = 0; }
			case 1 -> { r = x; g = c; b = 0; }
			case 2 -> { r = 0; g = c; b = x; }
			case 3 -> { r = 0; g = x; b = c; }
			case 4 -> { r = x; g = 0; b = c; }
			default -> { r = c; g = 0; b = x; }
		}
		int ri = (int) ((r + m) * 255);
		int gi = (int) ((g + m) * 255);
		int bi = (int) ((b + m) * 255);
		return (ri << 16) | (gi << 8) | bi;
	}

	static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h = 0, s = max == 0 ? 0 : d / max, v = max;
		if (d > 0) {
			if (max == r) h = ((g - b) / d + 6f) % 6f / 6f;
			else if (max == g) h = ((b - r) / d + 2f) / 6f;
			else h = ((r - g) / d + 4f) / 6f;
		}
		return new float[]{h, s, v};
	}

	private static float clampF(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}

	
	private static int[] generateColorGrid() {
		int[] colors = new int[100];
		for (int i = 0; i < 10; i++) {
			int v = (int) (i / 9f * 255);
			colors[i] = (v << 16) | (v << 8) | v;
		}
		
		for (int row = 1; row < 10; row++) {
			for (int col = 0; col < 10; col++) {
				float h = col / 10f;
				float s, v;
				if (row <= 3) {
					
					s = (row) / 3f * 0.6f;
					v = 1f;
				} else if (row <= 6) {
					
					s = 0.8f + (row - 4) * 0.067f;
					v = 1f - (row - 4) * 0.1f;
				} else {
					
					s = 0.9f;
					v = 0.7f - (row - 7) * 0.15f;
				}
				colors[row * 10 + col] = hsvToRgb(h, s, v);
			}
		}
		return colors;
	}
}
