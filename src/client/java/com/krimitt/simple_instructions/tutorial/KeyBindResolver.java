package com.krimitt.simple_instructions.tutorial;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

public class KeyBindResolver {

    public static KeyBinding resolve(String name) {
        GameOptions opts = MinecraftClient.getInstance().options;
        return switch (name) {
            case "forward" -> opts.forwardKey;
            case "back" -> opts.backKey;
            case "left" -> opts.leftKey;
            case "right" -> opts.rightKey;
            case "jump" -> opts.jumpKey;
            case "sneak" -> opts.sneakKey;
            case "sprint" -> opts.sprintKey;
            case "inventory" -> opts.inventoryKey;
            case "attack" -> opts.attackKey;
            case "use" -> opts.useKey;
            case "drop" -> opts.dropKey;
            case "chat" -> opts.chatKey;
            case "swap_hands" -> opts.swapHandsKey;
            default -> null; // Unknown names — caller should handle raw key detection
        };
    }
}
