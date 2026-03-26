package com.yasha.simple_instructions.tutorial;

import com.yasha.simple_instructions.SimpleInstructions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Manages plaque texture presets — built-in (generated at runtime) and custom (loaded from config folder).
 * Built-in presets: "default", "dark", "wooden", "clean" (solid mode — no texture).
 * Custom presets: any PNG dropped into config/simple_instructions/textures/
 */
public class PlaqueTextures {
	public static final Path CUSTOM_DIR = FabricLoader.getInstance().getConfigDir()
		.resolve("simple_instructions").resolve("textures");

	private static final int DEFAULT_TEX_SIZE = 64;
	private static final int BORDER = 8;

	// All known preset names in display order
	private static final List<String> presetOrder = new ArrayList<>();
	// Map from preset name → texture Identifier
	private static final Map<String, Identifier> textures = new LinkedHashMap<>();
	// Map from preset name → texture dimensions [width, height]
	private static final Map<String, int[]> textureSizes = new HashMap<>();
	// Set of custom preset names (for UI display purposes)
	private static final Set<String> customNames = new HashSet<>();

	private static boolean initialized = false;

	/**
	 * Ensures textures are loaded. Safe to call multiple times — only runs once.
	 * Must be called from the render thread (after OpenGL context is ready),
	 * NOT during onInitializeClient().
	 */
	public static void init() {
		if (initialized) return;
		if (MinecraftClient.getInstance().getTextureManager() == null) return;
		initialized = true;

		// "default" — the shipped asset texture
		Identifier defaultId = new Identifier("simple_instructions", "textures/gui/instruction_plaque.png");
		textures.put("default", defaultId);
		textureSizes.put("default", new int[]{DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE});
		presetOrder.add("default");

		// Generate built-in variants
		registerGenerated("dark", PlaqueTextures::paintDark);
		registerGenerated("wooden", PlaqueTextures::paintWooden);

		// "clean" is a marker for solid mode — no texture file needed
		presetOrder.add("clean");

		// Scan custom folder
		scanCustomFolder();
	}

	/**
	 * Rescan custom textures folder. Unregisters old custom textures and loads new ones.
	 * Can be called at any time after init.
	 */
	public static void reload() {
		if (!initialized) {
			init();
			return;
		}

		var texMgr = MinecraftClient.getInstance().getTextureManager();

		// Save names before clearing so we can clean up
		Set<String> oldCustom = new HashSet<>(customNames);

		// Unregister old custom textures
		for (String name : oldCustom) {
			Identifier id = textures.remove(name);
			if (id != null) texMgr.destroyTexture(id);
			textureSizes.remove(name);
		}
		presetOrder.removeAll(oldCustom);
		customNames.clear();

		// Rescan
		scanCustomFolder();
	}

	public static List<String> getPresetNames() {
		init();
		return Collections.unmodifiableList(presetOrder);
	}

	public static boolean isCustom(String name) {
		return customNames.contains(name);
	}

	/**
	 * Returns the texture Identifier for the given preset name.
	 * Returns null for "clean" (solid mode) or unknown names.
	 */
	public static Identifier getTexture(String name) {
		init();
		if ("clean".equals(name)) return null;
		Identifier id = textures.get(name);
		if (id != null) return id;
		// Fallback to default
		return textures.get("default");
	}

	/**
	 * Returns the texture dimensions [width, height] for the given preset.
	 * Falls back to [64, 64] if unknown.
	 */
	public static int[] getTextureSize(String name) {
		init();
		int[] size = textureSizes.get(name);
		if (size != null) return size;
		// Fallback
		return new int[]{DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE};
	}

	/**
	 * Whether the given preset uses solid rendering (no texture).
	 */
	public static boolean isSolidMode(String name) {
		return "clean".equals(name);
	}

	public static String getDisplayName(String name) {
		return switch (name) {
			case "default" -> "Default (Gold)";
			case "dark" -> "Dark Stone";
			case "wooden" -> "Wooden";
			case "clean" -> "Clean (Flat Color)";
			default -> name;
		};
	}

	// --- Built-in texture generation ---

	private static void registerGenerated(String name, Consumer<NativeImage> painter) {
		NativeImage img = new NativeImage(DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE, false);
		painter.accept(img);
		NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
		Identifier id = MinecraftClient.getInstance().getTextureManager()
			.registerDynamicTexture("si_plaque_" + name, tex);
		textures.put(name, id);
		textureSizes.put(name, new int[]{DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE});
		presetOrder.add(name);
	}

	private static void paintDark(NativeImage img) {
		// Dark stone theme: charcoal border with dark gray fill
		int borderOuter = abgr(255, 0x10, 0x10, 0x10);
		int borderMid   = abgr(255, 0x30, 0x30, 0x35);
		int borderInner = abgr(255, 0x45, 0x45, 0x4A);
		int fill        = abgr(255, 0x20, 0x20, 0x25);

		paintNineSlice(img, borderOuter, borderMid, borderInner, fill);
	}

	private static void paintWooden(NativeImage img) {
		// Wooden theme: brown border with lighter wood fill
		int borderOuter = abgr(255, 0x30, 0x1E, 0x0A);
		int borderMid   = abgr(255, 0x5A, 0x3A, 0x1A);
		int borderInner = abgr(255, 0x7A, 0x55, 0x30);
		int fill        = abgr(255, 0x8B, 0x6B, 0x42);

		paintNineSlice(img, borderOuter, borderMid, borderInner, fill);

		// Add simple wood grain lines
		int grain = abgr(255, 0x78, 0x5A, 0x35);
		for (int y = BORDER + 2; y < DEFAULT_TEX_SIZE - BORDER - 2; y += 4) {
			for (int x = BORDER; x < DEFAULT_TEX_SIZE - BORDER; x++) {
				img.setColor(x, y, grain);
			}
		}
	}

	private static void paintNineSlice(NativeImage img, int outer, int mid, int inner, int fill) {
		int size = DEFAULT_TEX_SIZE;
		// Fill entire image with outer border color
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				img.setColor(x, y, outer);
			}
		}

		// Mid border (2px inset)
		for (int y = 2; y < size - 2; y++) {
			for (int x = 2; x < size - 2; x++) {
				img.setColor(x, y, mid);
			}
		}

		// Inner border (5px inset)
		for (int y = 5; y < size - 5; y++) {
			for (int x = 5; x < size - 5; x++) {
				img.setColor(x, y, inner);
			}
		}

		// Fill (border px inset)
		for (int y = BORDER; y < size - BORDER; y++) {
			for (int x = BORDER; x < size - BORDER; x++) {
				img.setColor(x, y, fill);
			}
		}
	}

	// NativeImage uses ABGR format
	private static int abgr(int a, int r, int g, int b) {
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

	// --- Custom texture scanning ---

	private static void scanCustomFolder() {
		if (!Files.isDirectory(CUSTOM_DIR)) {
			try { Files.createDirectories(CUSTOM_DIR); } catch (IOException ignored) {}
			return;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(CUSTOM_DIR, "*.png")) {
			for (Path file : stream) {
				String fileName = file.getFileName().toString();
				String name = fileName.substring(0, fileName.length() - 4); // strip .png
				if (name.startsWith("_")) continue; // skip template files
				if (textures.containsKey(name)) continue; // don't override built-in

				try (InputStream is = Files.newInputStream(file)) {
					NativeImage img = NativeImage.read(is);
					int imgW = img.getWidth();
					int imgH = img.getHeight();
					NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
					Identifier id = MinecraftClient.getInstance().getTextureManager()
						.registerDynamicTexture("si_custom_" + name.toLowerCase(Locale.ROOT), tex);
					textures.put(name, id);
					textureSizes.put(name, new int[]{imgW, imgH});
					presetOrder.add(name);
					customNames.add(name);
				} catch (Exception e) {
					SimpleInstructions.LOGGER.warn("Failed to load custom plaque texture: {}", fileName, e);
				}
			}
		} catch (IOException e) {
			SimpleInstructions.LOGGER.warn("Failed to scan custom textures folder", e);
		}
	}

	/**
	 * Exports a template PNG with labeled regions to the custom textures folder.
	 * Returns the path to the exported file.
	 */
	public static Path exportTemplate() throws IOException {
		Files.createDirectories(CUSTOM_DIR);
		Path out = CUSTOM_DIR.resolve("_template.png");

		NativeImage img = new NativeImage(DEFAULT_TEX_SIZE, DEFAULT_TEX_SIZE, false);

		// Border region — red tint
		int borderColor = abgr(255, 0xCC, 0x44, 0x44);
		for (int y = 0; y < DEFAULT_TEX_SIZE; y++) {
			for (int x = 0; x < DEFAULT_TEX_SIZE; x++) {
				img.setColor(x, y, borderColor);
			}
		}

		// Fill region — green tint
		int fillColor = abgr(255, 0x44, 0xCC, 0x44);
		for (int y = BORDER; y < DEFAULT_TEX_SIZE - BORDER; y++) {
			for (int x = BORDER; x < DEFAULT_TEX_SIZE - BORDER; x++) {
				img.setColor(x, y, fillColor);
			}
		}

		// Corner markers — blue
		int cornerColor = abgr(255, 0x44, 0x44, 0xCC);
		for (int y = 0; y < BORDER; y++) {
			for (int x = 0; x < BORDER; x++) {
				img.setColor(x, y, cornerColor);
				img.setColor(DEFAULT_TEX_SIZE - 1 - x, y, cornerColor);
				img.setColor(x, DEFAULT_TEX_SIZE - 1 - y, cornerColor);
				img.setColor(DEFAULT_TEX_SIZE - 1 - x, DEFAULT_TEX_SIZE - 1 - y, cornerColor);
			}
		}

		img.writeTo(out);
		img.close();
		return out;
	}
}
