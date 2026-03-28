package com.krimitt.simple_instructions.tutorial;

import com.google.gson.*;
import com.krimitt.simple_instructions.SimpleInstructions;
import com.krimitt.simple_instructions.tutorial.ActionType;
import com.krimitt.simple_instructions.tutorial.InstructionStep;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("simple_instructions");
	private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
	private static final int CONFIG_VERSION = 3;

	private static boolean tutorialEnabled = true;
	private static boolean showOnWorldJoin = true;
	private static boolean skippable = true;

	private static int plaqueWidth = 240;
	private static int plaqueScale = 100;
	private static int positionXPercent = 50;
	private static int positionY = 12;
	private static boolean showIcon = true;
	private static String defaultIconItem = "minecraft:book";
	private static int backgroundOpacity = 100;
	private static boolean textShadow = true;
	private static String fontType = "default";

	private static int titleColor = 0xFFFFFF;
	private static int descriptionColor = 0xFFDD00;
	private static int barColor = 0x55AA22;
	private static int barCompleteColor = 0x77CC44;
	private static int backgroundColor = 0xC6A050;

	private static String renderMode = "texture";
	private static int nineSliceBorder = 8;
	private static String plaqueStyle = "default";

	private static int revealDurationMs = 1000;
	private static int dismissDurationMs = 400;
	private static int completingDurationMs = 1000;
	private static boolean enableFlash = true;
	private static int flashIntensity = 90;

	private static boolean enableCompletionSound = true;
	private static int soundVolume = 100;

	private static Map<String, StepVisualOverrides> visualOverrides = new HashMap<>();

	private static List<InstructionStep> steps = new ArrayList<>();

	public static void load() {
		if (!Files.exists(CONFIG_FILE)) return;
		try {
			String json = Files.readString(CONFIG_FILE);
			JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

			tutorialEnabled = getBool(obj, "tutorialEnabled", tutorialEnabled);
			showOnWorldJoin = getBool(obj, "showOnWorldJoin", showOnWorldJoin);
			skippable = getBool(obj, "skippable", skippable);
			plaqueWidth = getInt(obj, "plaqueWidth", plaqueWidth);
			plaqueScale = getInt(obj, "plaqueScale", plaqueScale);
			positionXPercent = getInt(obj, "positionXPercent", positionXPercent);
			positionY = getInt(obj, "positionY", positionY);
			showIcon = getBool(obj, "showIcon", showIcon);
			defaultIconItem = getStr(obj, "defaultIconItem", defaultIconItem);
			backgroundOpacity = getInt(obj, "backgroundOpacity", backgroundOpacity);
			textShadow = getBool(obj, "textShadow", textShadow);
			fontType = getStr(obj, "fontType", fontType);
			titleColor = getInt(obj, "titleColor", titleColor);
			descriptionColor = getInt(obj, "descriptionColor", descriptionColor);
			barColor = getInt(obj, "barColor", barColor);
			barCompleteColor = getInt(obj, "barCompleteColor", barCompleteColor);
			backgroundColor = getInt(obj, "backgroundColor", backgroundColor);
			revealDurationMs = getInt(obj, "revealDurationMs", revealDurationMs);
			dismissDurationMs = getInt(obj, "dismissDurationMs", dismissDurationMs);
			completingDurationMs = getInt(obj, "completingDurationMs", completingDurationMs);
			enableFlash = getBool(obj, "enableFlash", enableFlash);
			flashIntensity = getInt(obj, "flashIntensity", flashIntensity);
			enableCompletionSound = getBool(obj, "enableCompletionSound", enableCompletionSound);
			soundVolume = getInt(obj, "soundVolume", soundVolume);
			renderMode = getStr(obj, "renderMode", renderMode);
			nineSliceBorder = getInt(obj, "nineSliceBorder", nineSliceBorder);
			plaqueStyle = getStr(obj, "plaqueStyle", plaqueStyle);

			if (obj.has("visualOverrides")) {
				visualOverrides.clear();
				JsonObject vo = obj.getAsJsonObject("visualOverrides");
				for (String key : vo.keySet()) {
					visualOverrides.put(key, StepVisualOverrides.fromJson(vo.getAsJsonObject(key)));
				}
			}

			if (obj.has("steps")) {
				steps.clear();
				JsonArray arr = obj.getAsJsonArray("steps");
				for (JsonElement el : arr) {
					JsonObject s = el.getAsJsonObject();
					String icon = s.has("icon") ? s.get("icon").getAsString() : null;
					steps.add(new InstructionStep(
							getStr(s, "id", "step"),
							getStr(s, "title", "Title"),
							getStr(s, "description", "Description"),
							ActionType.valueOf(getStr(s, "actionType", "KEY_PRESS")),
							getStr(s, "targetKey", "forward"),
							getInt(s, "requiredCount", 1),
							icon
					));
				}
			}

			save();
		} catch (Exception e) {
			SimpleInstructions.LOGGER.warn("Failed to load config", e);
		}
	}

	public static void save() {
		JsonObject obj = new JsonObject();
		obj.addProperty("_comment", "Simple Instructions config. Edit values and relaunch to apply. Colors are decimal RGB (use a hex converter).");
		obj.addProperty("configVersion", CONFIG_VERSION);

		obj.addProperty("_comment_general", "--- General ---");
		obj.addProperty("tutorialEnabled", tutorialEnabled);
		obj.addProperty("showOnWorldJoin", showOnWorldJoin);
		obj.addProperty("skippable", skippable);

		obj.addProperty("_comment_appearance", "--- Appearance --- (plaqueWidth: 160-320, plaqueScale: 50-200%, positionXPercent: 0-100, positionY: pixels from top)");
		obj.addProperty("plaqueWidth", plaqueWidth);
		obj.addProperty("plaqueScale", plaqueScale);
		obj.addProperty("positionXPercent", positionXPercent);
		obj.addProperty("positionY", positionY);
		obj.addProperty("showIcon", showIcon);
		obj.addProperty("defaultIconItem", defaultIconItem);
		obj.addProperty("backgroundOpacity", backgroundOpacity);
		obj.addProperty("textShadow", textShadow);
		obj.addProperty("_comment_font", "fontType: 'default' (pixel), 'uniform' (smooth), 'alt' (enchant)");
		obj.addProperty("fontType", fontType);

		obj.addProperty("_comment_colors", "--- Colors --- (decimal RGB values, e.g. 16777215 = #FFFFFF white)");
		obj.addProperty("titleColor", titleColor);
		obj.addProperty("descriptionColor", descriptionColor);
		obj.addProperty("barColor", barColor);
		obj.addProperty("barCompleteColor", barCompleteColor);
		obj.addProperty("backgroundColor", backgroundColor);

		obj.addProperty("_comment_render", "--- Render --- renderMode: 'texture' (nine-slice PNG, resource-pack overridable) or 'solid' (flat color with 3D bevel)");
		obj.addProperty("renderMode", renderMode);
		obj.addProperty("nineSliceBorder", nineSliceBorder);
		obj.addProperty("_comment_plaqueStyle", "plaqueStyle: preset name — 'default', 'dark', 'wooden', 'clean', or a custom PNG filename (without .png)");
		obj.addProperty("plaqueStyle", plaqueStyle);

		obj.addProperty("_comment_animation", "--- Animation --- (durations in milliseconds)");
		obj.addProperty("revealDurationMs", revealDurationMs);
		obj.addProperty("dismissDurationMs", dismissDurationMs);
		obj.addProperty("completingDurationMs", completingDurationMs);
		obj.addProperty("enableFlash", enableFlash);
		obj.addProperty("flashIntensity", flashIntensity);

		obj.addProperty("_comment_sound", "--- Sound ---");
		obj.addProperty("enableCompletionSound", enableCompletionSound);
		obj.addProperty("soundVolume", soundVolume);

		obj.addProperty("_comment_overrides", "--- Per-Step Visual Overrides --- (keyed by step ID; null/missing = use global default)");
		JsonObject voObj = new JsonObject();
		for (Map.Entry<String, StepVisualOverrides> entry : visualOverrides.entrySet()) {
			if (entry.getValue().hasAnyOverride()) {
				voObj.add(entry.getKey(), entry.getValue().toJson());
			}
		}
		obj.add("visualOverrides", voObj);

		obj.addProperty("_comment_steps", "--- Instruction Steps --- actionType: KEY_PRESS or KEY_HOLD. targetKey: named key (forward/back/jump/sneak/sprint/inventory/attack/use/drop/chat) or raw key (key.keyboard.x / key.mouse.0).");
		JsonArray arr = new JsonArray();
		for (InstructionStep step : steps) {
			JsonObject s = new JsonObject();
			s.addProperty("id", step.getId());
			s.addProperty("title", step.getTitle());
			s.addProperty("description", step.getDescription());
			s.addProperty("actionType", step.getActionType().name());
			s.addProperty("targetKey", step.getTargetKey());
			s.addProperty("requiredCount", step.getRequiredCount());
			if (step.getIcon() != null) {
				s.addProperty("icon", step.getIcon());
			}
			arr.add(s);
		}
		obj.add("steps", arr);

		try {
			Files.createDirectories(CONFIG_DIR);
			Files.writeString(CONFIG_FILE, GSON.toJson(obj));
		} catch (IOException e) {
			SimpleInstructions.LOGGER.error("Failed to save config", e);
		}
	}

	private static boolean getBool(JsonObject obj, String key, boolean def) {
		return obj.has(key) ? obj.get(key).getAsBoolean() : def;
	}

	private static int getInt(JsonObject obj, String key, int def) {
		return obj.has(key) ? obj.get(key).getAsInt() : def;
	}

	private static String getStr(JsonObject obj, String key, String def) {
		return obj.has(key) ? obj.get(key).getAsString() : def;
	}

	public static List<InstructionStep> getSteps() {
		if (steps.isEmpty()) {
			//lazy-load from default.json on first access
			List<InstructionStep> defaults = InstructionLoader.loadDefault();
			steps.addAll(defaults);
		}
		return steps;
	}

	public static void setSteps(List<InstructionStep> newSteps) {
		steps = new ArrayList<>(newSteps);
	}

	public static void addStep(InstructionStep step) {
		steps.add(step);
	}

	public static void removeStep(int index) {
		if (index >= 0 && index < steps.size()) {
			steps.remove(index);
		}
	}

	public static void moveStepUp(int index) {
		if (index > 0 && index < steps.size()) {
			Collections.swap(steps, index, index - 1);
		}
	}

	public static void moveStepDown(int index) {
		if (index >= 0 && index < steps.size() - 1) {
			Collections.swap(steps, index, index + 1);
		}
	}

	public static boolean isTutorialEnabled() { return tutorialEnabled; }
	public static boolean isShowOnWorldJoin() { return showOnWorldJoin; }
	public static boolean isSkippable() { return skippable; }
	public static int getPlaqueWidth() { return plaqueWidth; }
	public static int getPlaqueScale() { return plaqueScale; }
	public static int getPositionXPercent() { return positionXPercent; }
	public static int getPositionY() { return positionY; }
	public static boolean isShowIcon() { return showIcon; }
	public static String getDefaultIconItem() { return defaultIconItem; }
	public static int getBackgroundOpacity() { return backgroundOpacity; }
	public static boolean isTextShadow() { return textShadow; }
	public static String getFontType() { return fontType; }
	public static int getTitleColor() { return titleColor; }
	public static int getDescriptionColor() { return descriptionColor; }
	public static int getBarColor() { return barColor; }
	public static int getBarCompleteColor() { return barCompleteColor; }
	public static int getBackgroundColor() { return backgroundColor; }
	public static int getRevealDurationMs() { return revealDurationMs; }
	public static int getDismissDurationMs() { return dismissDurationMs; }
	public static int getCompletingDurationMs() { return completingDurationMs; }
	public static boolean isEnableFlash() { return enableFlash; }
	public static int getFlashIntensity() { return flashIntensity; }
	public static boolean isEnableCompletionSound() { return enableCompletionSound; }
	public static int getSoundVolume() { return soundVolume; }
	public static String getRenderMode() { return renderMode; }
	public static int getNineSliceBorder() { return nineSliceBorder; }
	public static String getPlaqueStyle() { return plaqueStyle; }

	public static void setTutorialEnabled(boolean v) { tutorialEnabled = v; }
	public static void setShowOnWorldJoin(boolean v) { showOnWorldJoin = v; }
	public static void setSkippable(boolean v) { skippable = v; }
	public static void setPlaqueWidth(int v) { plaqueWidth = v; clearOverrideField(o -> o.setPlaqueWidth(null)); }
	public static void setPlaqueScale(int v) { plaqueScale = v; clearOverrideField(o -> o.setPlaqueScale(null)); }
	public static void setPositionXPercent(int v) { positionXPercent = v; clearOverrideField(o -> o.setPositionXPercent(null)); }
	public static void setPositionY(int v) { positionY = v; clearOverrideField(o -> o.setPositionY(null)); }
	public static void setShowIcon(boolean v) { showIcon = v; }
	public static void setDefaultIconItem(String v) { defaultIconItem = v; }
	public static void setBackgroundOpacity(int v) { backgroundOpacity = v; }
	public static void setTextShadow(boolean v) { textShadow = v; clearOverrideField(o -> o.setTextShadow(null)); }
	public static void setFontType(String v) { fontType = v; clearOverrideField(o -> o.setFontType(null)); }
	public static void setTitleColor(int v) { titleColor = v; clearOverrideField(o -> o.setTitleColor(null)); }
	public static void setDescriptionColor(int v) { descriptionColor = v; clearOverrideField(o -> o.setDescriptionColor(null)); }
	public static void setBarColor(int v) { barColor = v; }
	public static void setBarCompleteColor(int v) { barCompleteColor = v; }
	public static void setBackgroundColor(int v) { backgroundColor = v; clearOverrideField(o -> o.setBackgroundColor(null)); }
	public static void setRevealDurationMs(int v) { revealDurationMs = v; }
	public static void setDismissDurationMs(int v) { dismissDurationMs = v; }
	public static void setCompletingDurationMs(int v) { completingDurationMs = v; }
	public static void setEnableFlash(boolean v) { enableFlash = v; }
	public static void setFlashIntensity(int v) { flashIntensity = v; }
	public static void setEnableCompletionSound(boolean v) { enableCompletionSound = v; }
	public static void setSoundVolume(int v) { soundVolume = v; }
	public static void setRenderMode(String v) { renderMode = v; }
	public static void setNineSliceBorder(int v) { nineSliceBorder = v; }
	public static void setPlaqueStyle(String v) { plaqueStyle = v; clearOverrideField(o -> o.setPlaqueStyle(null)); }

	public static void resetToDefaults() {
		tutorialEnabled = true;
		showOnWorldJoin = true;
		skippable = true;
		plaqueWidth = 240;
		plaqueScale = 100;
		positionXPercent = 50;
		positionY = 12;
		showIcon = true;
		defaultIconItem = "minecraft:book";
		backgroundOpacity = 100;
		textShadow = true;
		fontType = "default";
		titleColor = 0xFFFFFF;
		descriptionColor = 0xFFDD00;
		barColor = 0x55AA22;
		barCompleteColor = 0x77CC44;
		backgroundColor = 0xC6A050;
		revealDurationMs = 1000;
		dismissDurationMs = 400;
		completingDurationMs = 1000;
		enableFlash = true;
		flashIntensity = 90;
		enableCompletionSound = true;
		soundVolume = 100;
		renderMode = "texture";
		nineSliceBorder = 8;
		plaqueStyle = "default";
		visualOverrides.clear();
		save();
	}

	public static void clearOverrideField(java.util.function.Consumer<StepVisualOverrides> clearer) {
		for (StepVisualOverrides o : visualOverrides.values()) {
			clearer.accept(o);
		}
		visualOverrides.values().removeIf(o -> !o.hasAnyOverride());
	}

	public static StepVisualOverrides getOverridesForStep(String stepId) {
		return visualOverrides.computeIfAbsent(stepId, k -> new StepVisualOverrides());
	}

	public static void clearOverridesForStep(String stepId) {
		visualOverrides.remove(stepId);
	}

	public static void resetStepsToDefaults() {
		steps.clear();
		visualOverrides.clear();
		List<InstructionStep> defaults = InstructionLoader.loadDefault();
		steps.addAll(defaults);
		save();
	}
}
