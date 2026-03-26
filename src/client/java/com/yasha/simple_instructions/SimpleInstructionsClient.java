package com.yasha.simple_instructions;

import com.yasha.simple_instructions.tutorial.InstructionManager;
import com.yasha.simple_instructions.tutorial.InstructionRenderer;
import com.yasha.simple_instructions.tutorial.ModConfig;
import com.yasha.simple_instructions.tutorial.PlaqueEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SimpleInstructionsClient implements ClientModInitializer {
	public static KeyBinding skipTutorialKey;
	private static boolean skipKeyWasDown = false;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModCommands.register();

		skipTutorialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.simple_instructions.skip_tutorial",
			InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X,
			"key.categories.simple_instructions"
		));
		HudRenderCallback.EVENT.register(new InstructionRenderer());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			InstructionManager mgr = InstructionManager.INSTANCE;
			mgr.tick();

			// ESC during test mode → abort and return to editor
			// Intercept both: ESC with no screen open, and ESC that already opened the pause menu
			if (mgr.isTestMode() && mgr.isActive()) {
				if (client.currentScreen instanceof GameMenuScreen) {
					client.setScreen(null);
					mgr.abort();
					return;
				}
				if (client.currentScreen == null) {
					long window = client.getWindow().getHandle();
					if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_ESCAPE)) {
						mgr.abort();
						return;
					}
				}
			}

			// Skip tutorial keybind (only when active, not test mode, skippable)
			if (!mgr.isTestMode() && mgr.isActive() && ModConfig.isSkippable()
					&& client.currentScreen == null) {
				// Try registered keybind first
				boolean skip = false;
				while (skipTutorialKey.wasPressed()) {
					skip = true;
				}
				// Fallback: raw GLFW key check with rising-edge detection
				if (!skip) {
					long window = client.getWindow().getHandle();
					boolean isDown = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_X);
					if (isDown && !skipKeyWasDown) {
						skip = true;
					}
					skipKeyWasDown = isDown;
				}
				if (skip) {
					mgr.skipAll();
				}
			} else {
				// Drain any queued presses when skip isn't available
				while (skipTutorialKey.wasPressed()) { /* consume */ }
				skipKeyWasDown = false;
			}
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			client.execute(() -> {
				InstructionManager.INSTANCE.onWorldJoin();
			});
		});

		// Add "SI" button to pause screen
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof GameMenuScreen) {
				Screens.getButtons(screen).add(
					ButtonWidget.builder(Text.literal("SI"), btn -> {
						client.setScreen(new PlaqueEditorScreen(screen));
					})
					.dimensions(scaledWidth - 24, 4, 20, 20)
					.build()
				);
			}
		});
	}
}
