package com.infinitymax.industry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

public class NetworkManager {

    private static final NetworkManager INSTANCE = new NetworkManager();

    // デバウンス用
    private final Map<Level, Integer> dirtyTicks = new HashMap<>();
    private final Map<Level, NetworkGraph> graphs = new HashMap<>();

    private static final int DEBOUNCE_TICKS = 5; // 5tick遅延で合成処理

    public static NetworkManager get() {
        return INSTANCE;
    }

    private NetworkManager() {}

    /** ノード変更があったときに呼び出す */
    public void markDirty(Level level) {
        if (level.isClientSide) return;
        dirtyTicks.put(level, DEBOUNCE_TICKS);
    }

    /** サーバーTickごとに呼び出す */
    public void serverTick(Level level) {
        if (level.isClientSide) return;
        if (!dirtyTicks.containsKey(level)) return;

        int left = dirtyTicks.get(level) - 1;
        if (left <= 0) {
            rebuildGraph((ServerLevel) level);
            dirtyTicks.remove(level);
        } else {
            dirtyTicks.put(level, left);
        }
    }

    /** グラフ再構築 */
    private void rebuildGraph(ServerLevel level) {
        NetworkGraph graph = new NetworkGraph(level);
        graph.rebuild();
        graphs.put(level, graph);
    }

    public Optional<NetworkGraph> getGraph(Level level) {
        return Optional.ofNullable(graphs.get(level));
    }
}