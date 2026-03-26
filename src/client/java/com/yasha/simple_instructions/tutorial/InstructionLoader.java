package com.yasha.simple_instructions.tutorial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yasha.simple_instructions.SimpleInstructions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InstructionLoader {
	private static final Identifier DEFAULT_PATH =
			new Identifier(SimpleInstructions.MOD_ID, "instructions/default.json");

	public static List<InstructionStep> loadDefault() {
		try {
			Optional<Resource> resource = MinecraftClient.getInstance()
					.getResourceManager()
					.getResource(DEFAULT_PATH);
			if (resource.isEmpty()) {
				SimpleInstructions.LOGGER.warn("Default instruction file not found: {}", DEFAULT_PATH);
				return Collections.emptyList();
			}
			try (InputStream stream = resource.get().getInputStream();
				 InputStreamReader reader = new InputStreamReader(stream)) {
				return parseInstructions(reader);
			}
		} catch (Exception e) {
			SimpleInstructions.LOGGER.error("Failed to load instructions", e);
			return Collections.emptyList();
		}
	}

	public static String loadSetId() {
		try {
			Optional<Resource> resource = MinecraftClient.getInstance()
					.getResourceManager()
					.getResource(DEFAULT_PATH);
			if (resource.isEmpty()) return "default";
			try (InputStream stream = resource.get().getInputStream();
				 InputStreamReader reader = new InputStreamReader(stream)) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
				return obj.has("id") ? obj.get("id").getAsString() : "default";
			}
		} catch (Exception e) {
			return "default";
		}
	}

	private static List<InstructionStep> parseInstructions(InputStreamReader reader) {
		JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
		JsonArray instructions = root.getAsJsonArray("instructions");
		List<InstructionStep> steps = new ArrayList<>();
		for (JsonElement element : instructions) {
			JsonObject obj = element.getAsJsonObject();
			String icon = obj.has("icon") ? obj.get("icon").getAsString() : null;
			steps.add(new InstructionStep(
					obj.get("id").getAsString(),
					obj.get("title").getAsString(),
					obj.get("description").getAsString(),
					ActionType.valueOf(obj.get("actionType").getAsString()),
					obj.get("targetKey").getAsString(),
					obj.get("requiredCount").getAsInt(),
					icon
			));
		}
		return steps;
	}
}
