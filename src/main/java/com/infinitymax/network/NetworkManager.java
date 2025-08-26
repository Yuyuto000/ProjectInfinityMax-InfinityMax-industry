package com.infinitymax.industry.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * 全ワールドに1つ存在するネットワーク管理器。
 * - FluidNetwork, ElectricNetwork を登録・保持する
 * - 各tickで全ネットワークを更新
 * - onNeighborChanged 等で再構築要求を受け取る
 */
public class NetworkManager {

    private final Map<Level, List<FluidNetwork>> fluidNetworks = new HashMap<>();
    private final Map<Level, List<ElectricNetwork>> electricNetworks = new HashMap<>();

    private static final NetworkManager INSTANCE = new NetworkManager();
    public static NetworkManager get() { return INSTANCE; }

    private NetworkManager() {}

    /** 1tick処理: ワールド内の全ネットワークを更新 */
    public void tick(Level level) {
        if (!(level instanceof ServerLevel)) return;

        fluidNetworks.computeIfAbsent(level, l -> new ArrayList<>())
                .forEach(net -> net.tick(level));
        electricNetworks.computeIfAbsent(level, l -> new ArrayList<>())
                .forEach(net -> net.tick(level));
    }

    /** ワールド内のFluidNetworkを全リセット・再構築 */
    public void rebuildFluidNetworks(Level level) {
        List<FluidNetwork> nets = FluidNetwork.discoverAll(level);
        fluidNetworks.put(level, nets);
    }

    /** ワールド内のElectricNetworkを全リセット・再構築 */
    public void rebuildElectricNetworks(Level level) {
        List<ElectricNetwork> nets = ElectricNetwork.discoverAll(level);
        electricNetworks.put(level, nets);
    }
}