package com.example.damageboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class DamageBoostMod implements ClientModInitializer {

    private static final int MULTIPLIER = 3; // ~250% damage
    private static long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN_MS = 250;

    private static boolean enabled = false;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.damageboost.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.damageboostmod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                client.player.sendMessage(
                    net.minecraft.text.Text.of("Damage Boost " + (enabled ? "Enabled" : "Disabled")),
                    true
                );
            }

            if (!enabled) return;

            if (client.player != null && client.world != null && client.options.attackKey.isPressed()) {
                long now = System.currentTimeMillis();
                if (now - lastAttackTime > ATTACK_COOLDOWN_MS) {
                    attemptMultiHit(client);
                    lastAttackTime = now;
                }
            }
        });
    }

    private void attemptMultiHit(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            ClientPlayerEntity player = client.player;

            for (int i = 0; i < MULTIPLIER; i++) {
                PlayerInteractEntityC2SPacket packet =
                        PlayerInteractEntityC2SPacket.attack(target, player.isSneaking());
                player.networkHandler.sendPacket(packet);
            }
        }
    }
}