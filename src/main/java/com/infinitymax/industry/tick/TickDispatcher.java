package com.infinitymax.industry.tick;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TickDispatcher
 * - BlockEntity が自身を register/unregister しておくと、ここで一括 tick を回せる。
 * - NeoForge/Fabric の server tick ハンドラから TickDispatcher.tickAll(server) を呼ぶ。
 */
public final class TickDispatcher {
    private TickDispatcher() {}

    private static final Set<BlockEntity> TICKABLES = ConcurrentHashMap.newKeySet();

    public static void register(BlockEntity be) {
        if (be == null) return;
        TICKABLES.add(be);
    }

    public static void unregister(BlockEntity be) {
        if (be == null) return;
        TICKABLES.remove(be);
    }

    /**
     * 呼び出し元（プラットフォーム互換初期化）から server を渡して呼ぶ。
     * ここでは Client/Server 判定を行わず、各 BE の serverTick() を呼ぶ形にしている。
     */
    public static void tickAll() {
        for (BlockEntity be : TICKABLES) {
            try {
                if (be == null) continue;
                // 各 BE に serverTick メソッドを実装しておくこと（本実装の BE は実装済）
                if (be instanceof net.minecraft.world.level.block.entity.BlockEntity) {
                    // Reflection-less dispatch by instanceof checks in concrete classes (they provide serverTick())
                    // We'll do safe casts to known types in their classes' serverTick methods.
                    // Just attempt to call known method via interface - but we didn't define an interface here to keep it simple.
                    // Concrete BE classes in this project implement serverTick() as a public method.
                    try {
                        // If class has public serverTick(), invoke it:
                        be.getClass().getMethod("serverTick").invoke(be);
                    } catch (NoSuchMethodException ignore) {
                        // no serverTick - skip
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
