package com.infinitymax.industry.compat.neoforge;

import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NeoForge / Forge 用互換初期化器
 * - ServerTickEvent の END フェーズで TickDispatcher.tickAll を呼ぶ
 *
 * 使い方: Mod コンストラクタで NeoForgeCompatInitializer.init() を呼ぶ
 */
public final class NeoForgeCompatInitializer {

    private NeoForgeCompatInitializer() {}

    public static void init() {
        try {
            MinecraftForge.EVENT_BUS.register(new NeoForgeCompatInitializer());
            System.out.println("[InfinityMax-Compat][NeoForge] initialized");
        } catch (Throwable t) {
            System.err.println("[InfinityMax-Compat][NeoForge] init failed: " + t.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            TickDispatcher.tickAll();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}