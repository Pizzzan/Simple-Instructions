package com.krimitt.simple_instructions.tutorial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.krimitt.simple_instructions.SimpleInstructions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player tutorial progress and completion persistence.
 * Stores data under config/simple_instructions/progress/{uuid}.json
 *
 * File format:
 * {
 *   "completedSets": ["default"],
 *   "currentStep": 2
 * }
 */
public class CompletionPersistence {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PROGRESS_DIR = FabricLoader.getInstance().getConfigDir()
			.resolve("simple_instructions").resolve("progress");

	private static Path getPlayerFile() {
		MinecraftClient client = MinecraftClient.getInstance();
		UUID uuid = null;
		if (client.getSession() != null) {
			uuid = client.getSession().getUuidOrNull();
		}
		String filename = (uuid != null) ? uuid.toString() : "unknown";
		return PROGRESS_DIR.resolve(filename + ".json");
	}

	private static JsonObject readFile() {
		Path file = getPlayerFile();
		if (!Files.exists(file)) return new JsonObject();
		try {
			String json = Files.readString(file);
			return JsonParser.parseString(json).getAsJsonObject();
		} catch (Exception e) {
			SimpleInstructions.LOGGER.warn("Failed to read progress file", e);
			return new JsonObject();
		}
	}

	private static void writeFile(JsonObject obj) {
		try {
			Files.createDirectories(PROGRESS_DIR);
			Files.writeString(getPlayerFile(), GSON.toJson(obj));
		} catch (IOException e) {
			SimpleInstructions.LOGGER.error("Failed to save progress file", e);
		}
	}

	public static Set<String> loadCompleted() {
		Set<String> completed = new HashSet<>();
		JsonObject obj = readFile();
		JsonArray arr = obj.getAsJsonArray("completedSets");
		if (arr != null) {
			arr.forEach(e -> completed.add(e.getAsString()));
		}
		return completed;
	}

	public static void markCompleted(String setId) {
		JsonObject obj = readFile();
		Set<String> completed = new HashSet<>();
		JsonArray existing = obj.getAsJsonArray("completedSets");
		if (existing != null) {
			existing.forEach(e -> completed.add(e.getAsString()));
		}
		completed.add(setId);
		JsonArray arr = new JsonArray();
		completed.forEach(arr::add);
		obj.add("completedSets", arr);
		writeFile(obj);
	}

	public static int loadCurrentStep() {
		JsonObject obj = readFile();
		if (obj.has("currentStep")) {
			return obj.get("currentStep").getAsInt();
		}
		return 0;
	}

	public static void saveCurrentStep(int stepIndex) {
		JsonObject obj = readFile();
		obj.addProperty("currentStep", stepIndex);
		writeFile(obj);
	}

	public static void resetAll() {
		try {
			Files.deleteIfExists(getPlayerFile());
		} catch (IOException e) {
			SimpleInstructions.LOGGER.error("Failed to reset completion data", e);
		}
	}
}
