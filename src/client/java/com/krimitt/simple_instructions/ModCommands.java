package com.krimitt.simple_instructions;

import com.mojang.brigadier.CommandDispatcher;
import com.krimitt.simple_instructions.tutorial.CompletionPersistence;
import com.krimitt.simple_instructions.tutorial.InstructionManager;
import com.krimitt.simple_instructions.tutorial.ModConfig;
import com.krimitt.simple_instructions.tutorial.PlaqueEditorScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class ModCommands {
	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
	}

	private static void registerCommands(
			CommandDispatcher<FabricClientCommandSource> dispatcher,
			CommandRegistryAccess registryAccess) {

		dispatcher.register(ClientCommandManager.literal("simpleinstructions")
			.then(ClientCommandManager.literal("editor")
				.executes(ctx -> {
					MinecraftClient client = MinecraftClient.getInstance();
					if (!hasPermission(ctx.getSource())) {
						ctx.getSource().sendFeedback(Text.literal("\u00a7cYou need operator permissions to open the editor."));
						return 0;
					}
					client.send(() -> client.setScreen(new PlaqueEditorScreen(null)));
					return 1;
				})
			)
			.then(ClientCommandManager.literal("reset")
				.executes(ctx -> {
					InstructionManager.INSTANCE.abort();
					CompletionPersistence.resetAll();
					ctx.getSource().sendFeedback(Text.literal("\u00a7aTutorial progress has been reset."));
					return 1;
				})
			)
			.then(ClientCommandManager.literal("test")
				.executes(ctx -> {
					InstructionManager mgr = InstructionManager.INSTANCE;
					if (mgr.isActive()) {
						ctx.getSource().sendFeedback(Text.literal("\u00a7eA tutorial is already running. Use ESC to abort first."));
						return 0;
					}
					mgr.startTest(0, false, null);
					ctx.getSource().sendFeedback(Text.literal("\u00a7aStarting quick test from step 1..."));
					return 1;
				})
			)
			.then(ClientCommandManager.literal("reload")
				.executes(ctx -> {
					ModConfig.load();
					ctx.getSource().sendFeedback(Text.literal("\u00a7aConfig reloaded from file."));
					return 1;
				})
			)
		);
	}

	private static boolean hasPermission(FabricClientCommandSource source) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.isInSingleplayer()) return true;
		return client.player != null && client.player.hasPermissionLevel(2);
	}
}
