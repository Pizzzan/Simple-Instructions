package com.yasha.simple_instructions.tutorial;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class StepVisualOverrides {
	private @Nullable Integer positionXPercent;
	private @Nullable Integer positionY;
	private @Nullable Integer plaqueWidth;
	private @Nullable Integer plaqueScale;
	private @Nullable Integer titleColor;
	private @Nullable Integer descriptionColor;
	private @Nullable Integer backgroundColor;
	private @Nullable String fontType;
	private @Nullable Boolean textShadow;
	private @Nullable String plaqueStyle;

	public StepVisualOverrides() {}

	// --- Getters (nullable — null means "use global default") ---

	public @Nullable Integer getPositionXPercent() { return positionXPercent; }
	public @Nullable Integer getPositionY() { return positionY; }
	public @Nullable Integer getPlaqueWidth() { return plaqueWidth; }
	public @Nullable Integer getPlaqueScale() { return plaqueScale; }
	public @Nullable Integer getTitleColor() { return titleColor; }
	public @Nullable Integer getDescriptionColor() { return descriptionColor; }
	public @Nullable Integer getBackgroundColor() { return backgroundColor; }
	public @Nullable String getFontType() { return fontType; }
	public @Nullable Boolean getTextShadow() { return textShadow; }
	public @Nullable String getPlaqueStyle() { return plaqueStyle; }

	// --- Setters ---

	public void setPositionXPercent(@Nullable Integer v) { positionXPercent = v; }
	public void setPositionY(@Nullable Integer v) { positionY = v; }
	public void setPlaqueWidth(@Nullable Integer v) { plaqueWidth = v; }
	public void setPlaqueScale(@Nullable Integer v) { plaqueScale = v; }
	public void setTitleColor(@Nullable Integer v) { titleColor = v; }
	public void setDescriptionColor(@Nullable Integer v) { descriptionColor = v; }
	public void setBackgroundColor(@Nullable Integer v) { backgroundColor = v; }
	public void setFontType(@Nullable String v) { fontType = v; }
	public void setTextShadow(@Nullable Boolean v) { textShadow = v; }
	public void setPlaqueStyle(@Nullable String v) { plaqueStyle = v; }

	// --- Resolved getters (with global fallback) ---

	public int resolvePositionXPercent() {
		return positionXPercent != null ? positionXPercent : ModConfig.getPositionXPercent();
	}

	public int resolvePositionY() {
		return positionY != null ? positionY : ModConfig.getPositionY();
	}

	public int resolvePlaqueWidth() {
		return plaqueWidth != null ? plaqueWidth : ModConfig.getPlaqueWidth();
	}

	public int resolvePlaqueScale() {
		return plaqueScale != null ? plaqueScale : ModConfig.getPlaqueScale();
	}

	public int resolveTitleColor() {
		return titleColor != null ? titleColor : ModConfig.getTitleColor();
	}

	public int resolveDescriptionColor() {
		return descriptionColor != null ? descriptionColor : ModConfig.getDescriptionColor();
	}

	public int resolveBackgroundColor() {
		return backgroundColor != null ? backgroundColor : ModConfig.getBackgroundColor();
	}

	public String resolveFontType() {
		return fontType != null ? fontType : ModConfig.getFontType();
	}

	public boolean resolveTextShadow() {
		return textShadow != null ? textShadow : ModConfig.isTextShadow();
	}

	public String resolvePlaqueStyle() {
		return plaqueStyle != null ? plaqueStyle : ModConfig.getPlaqueStyle();
	}

	public boolean hasAnyOverride() {
		return positionXPercent != null || positionY != null
			|| plaqueWidth != null || plaqueScale != null
			|| titleColor != null || descriptionColor != null || backgroundColor != null
			|| fontType != null || textShadow != null || plaqueStyle != null;
	}

	public void clearAll() {
		positionXPercent = null;
		positionY = null;
		plaqueWidth = null;
		plaqueScale = null;
		titleColor = null;
		descriptionColor = null;
		backgroundColor = null;
		fontType = null;
		textShadow = null;
		plaqueStyle = null;
	}

	// --- JSON serialization ---

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		if (positionXPercent != null) obj.addProperty("positionXPercent", positionXPercent);
		if (positionY != null) obj.addProperty("positionY", positionY);
		if (plaqueWidth != null) obj.addProperty("plaqueWidth", plaqueWidth);
		if (plaqueScale != null) obj.addProperty("plaqueScale", plaqueScale);
		if (titleColor != null) obj.addProperty("titleColor", titleColor);
		if (descriptionColor != null) obj.addProperty("descriptionColor", descriptionColor);
		if (backgroundColor != null) obj.addProperty("backgroundColor", backgroundColor);
		if (fontType != null) obj.addProperty("fontType", fontType);
		if (textShadow != null) obj.addProperty("textShadow", textShadow);
		if (plaqueStyle != null) obj.addProperty("plaqueStyle", plaqueStyle);
		return obj;
	}

	public static StepVisualOverrides fromJson(JsonObject obj) {
		StepVisualOverrides o = new StepVisualOverrides();
		if (obj.has("positionXPercent")) o.positionXPercent = obj.get("positionXPercent").getAsInt();
		if (obj.has("positionY")) o.positionY = obj.get("positionY").getAsInt();
		if (obj.has("plaqueWidth")) o.plaqueWidth = obj.get("plaqueWidth").getAsInt();
		if (obj.has("plaqueScale")) o.plaqueScale = obj.get("plaqueScale").getAsInt();
		if (obj.has("titleColor")) o.titleColor = obj.get("titleColor").getAsInt();
		if (obj.has("descriptionColor")) o.descriptionColor = obj.get("descriptionColor").getAsInt();
		if (obj.has("backgroundColor")) o.backgroundColor = obj.get("backgroundColor").getAsInt();
		if (obj.has("fontType")) o.fontType = obj.get("fontType").getAsString();
		if (obj.has("textShadow")) o.textShadow = obj.get("textShadow").getAsBoolean();
		if (obj.has("plaqueStyle")) o.plaqueStyle = obj.get("plaqueStyle").getAsString();
		return o;
	}
}
