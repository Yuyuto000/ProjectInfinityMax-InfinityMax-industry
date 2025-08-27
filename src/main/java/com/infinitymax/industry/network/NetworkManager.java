package com.infinitymax.industry.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * NetworkManager（シングルトン）
 *
 * - Fluid ネットワークと Electric ネットワークを別々に管理
 * - markFluidDirty / markElectricDirty を呼ぶとデバウンスフラグを立てる
 * - serverTick(Level) をワールド毎のサーバティックから呼ぶと、
 *   DEBOUNCE_TICKS を経て discoverAll() -> rebuild を行う
 *
 * 使用例:
 * - 各 BE の onNeighborsChanged() / onLoad() / setRemoved() などで
 *     NetworkManager.get().markFluidDirty(level);
 *     NetworkManager.get().markElectricDirty(level);
 *   を呼ぶ（該当するタイプのみ）。
 *
 * - サーバワールドの tick ループで毎 tick:
 *     NetworkManager.get().serverTick(serverLevel);
 */
public final class NetworkManager {

    private static final NetworkManager INSTANCE = new NetworkManager();

    public static NetworkManager get() { return INSTANCE; }

    // デバウンス設定（調整可）
    private static final int DEBOUNCE_TICKS = 5;

    // ワールド -> 残りデバウンスカウント
    private final Map<Level, Integer> fluidDirty = new HashMap<>();
    private final Map<Level, Integer> electricDirty = new HashMap<>();

    // ワールド -> 構築済みネットワークリスト
    private final Map<Level, List<FluidNetwork>> fluidNetworks = new HashMap<>();
    private final Map<Level, List<ElectricNetwork>> electricNetworks = new HashMap<>();

    private NetworkManager() {}

    // ========== 外部呼出（BE から） ==========
    /** Fluid グラフが変化した可能性があるワールドをマークする */
    public void markFluidDirty(Level level) {
        if (level == null || level.isClientSide) return;
        fluidDirty.put(level, DEBOUNCE_TICKS);
    }

    /** Electric グラフが変化した可能性があるワールドをマークする */
    public void markElectricDirty(Level level) {
        if (level == null || level.isClientSide) return;
        electricDirty.put(level, DEBOUNCE_TICKS);
    }

    // ========== 毎ワールド tick で呼ぶ ========
    /** ワールド毎にサーバTick から呼んでください */
    public void serverTick(Level level) {
        if (level == null || level.isClientSide) return;

        // まず fluid side
        if (fluidDirty.containsKey(level)) {
            int left = fluidDirty.get(level) - 1;
            if (left <= 0) {
                rebuildFluidNetworks((ServerLevel) level);
                fluidDirty.remove(level);
            } else {
                fluidDirty.put(level, left);
            }
        }

        // electric side
        if (electricDirty.containsKey(level)) {
            int left = electricDirty.get(level) - 1;
            if (left <= 0) {
                rebuildElectricNetworks((ServerLevel) level);
                electricDirty.remove(level);
            } else {
                electricDirty.put(level, left);
            }
        }

        // tick each network once (軽量処理)
        List<FluidNetwork> fns = fluidNetworks.get(level);
        if (fns != null) {
            for (FluidNetwork fn : fns) fn.tick(level);
        }
        List<ElectricNetwork> ens = electricNetworks.get(level);
        if (ens != null) {
            for (ElectricNetwork en : ens) en.tick(level);
        }
    }

    // ========== 再構築処理 ==========
    private void rebuildFluidNetworks(ServerLevel level) {
        List<FluidNetwork> nets = FluidNetwork.discoverAll(level);
        fluidNetworks.put(level, nets);
        // オプション: 各ネットワークに onRebuilt フックを渡す
        for (FluidNetwork n : nets) n.onRebuilt(level);
    }

    private void rebuildElectricNetworks(ServerLevel level) {
        List<ElectricNetwork> nets = ElectricNetwork.discoverAll(level);
        electricNetworks.put(level, nets);
        for (ElectricNetwork n : nets) n.onRebuilt(level);
    }

    // ========== 取得 API ==========
    public List<FluidNetwork> getFluidNetworks(Level level) {
        return fluidNetworks.getOrDefault(level, Collections.emptyList());
    }

    public List<ElectricNetwork> getElectricNetworks(Level level) {
        return electricNetworks.getOrDefault(level, Collections.emptyList());
    }
}