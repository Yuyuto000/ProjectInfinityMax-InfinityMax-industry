package com.infinitymax.industry.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/**
 * 右クリックでGUIを開く際のローダー差分を吸収
 */
public final class OpenMenuHelper {
    private OpenMenuHelper() {}

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        // 1) NeoForge
        try {
            Class<?> hooks = Class.forName("net.neoforged.neoforge.network.NetworkHooks");
            hooks.getMethod("openScreen", ServerPlayer.class, MenuProvider.class, BlockPos.class)
                 .invoke(null, player, provider, pos);
            return;
        } catch (Throwable ignored) {}

        // 2) Forge (旧 / 併存環境向け)
        try {
            Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
            hooks.getMethod("openScreen", ServerPlayer.class, MenuProvider.class, BlockPos.class)
                 .invoke(null, player, provider, pos);
            return;
        } catch (Throwable ignored) {}

        // 3) Fabric
        try {
            // Fabric は ServerPlayer#openMenu(MenuProvider) で十分
            player.openMenu(provider);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to open menu on this platform", t);
        }
    }
}