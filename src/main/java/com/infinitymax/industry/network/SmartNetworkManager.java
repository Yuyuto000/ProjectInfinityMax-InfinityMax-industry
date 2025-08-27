package com.infinitymax.industry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * SmartNetworkManager
 *
 * 特徴:
 * - Fluid/Electric を分離管理
 * - markXxxDirty(level, originPos) で「変更があったワールド＋起点座標」をキュー登録
 * - デバウンス（DEBOUNCE_TICKS）でまとめ、再構築を行う
 * - 1tick あたりの再構築数を制限（MAX_REBUILDS_PER_TICK）してスパイク抑制
 * - rebuild 時は「origin set」から局所探索 (discoverFromOrigins)。origin が無い場合は discoverAll を実行
 *
 * 使い方:
 * - 各 BE の onNeighborsChanged / onLoad / setRemoved 等で
 *      NetworkManager.get().markFluidDirty(level, pos);
 *   または
 *      NetworkManager.get().markElectricDirty(level, pos);
 *
 * - サーバワールド tick から毎 tick:
 *      NetworkManager.get().serverTick(level);
 */
public final class SmartNetworkManager {

    private static final SmartNetworkManager INSTANCE = new SmartNetworkManager();

    public static SmartNetworkManager get() { return INSTANCE; }

    // デバウンス設定（調整可）
    private static final int DEBOUNCE_TICKS = 4;
    // 1 tick あたりの最大再構築数（過負荷防止）
    private static final int MAX_REBUILDS_PER_TICK = 2;

    // ワールド -> (origin positionsが入ったキュー)
    private final Map<Level, Deque<BlockPos>> fluidDirtyOrigins = new HashMap<>();
    private final Map<Level, Deque<BlockPos>> electricDirtyOrigins = new HashMap<>();

    // ワールド -> 残りデバウンスカウント（全 origin 集合に対して共通）
    private final Map<Level, Integer> fluidDebounce = new HashMap<>();
    private final Map<Level, Integer> electricDebounce = new HashMap<>();

    // 構築済みネットワーク保持（サーバ再起動しない限り）
    private final Map<Level, List<com.infinitymax.industry.fluid.FluidNetwork>> fluidNetworks = new HashMap<>();
    private final Map<Level, List<com.infinitymax.industry.energy.ElectricNetwork>> electricNetworks = new HashMap<>();

    private SmartNetworkManager() {}

    // -------------------------
    // 外部呼び出し API
    // -------------------------
    public void markFluidDirty(Level level, BlockPos origin) {
        if (level == null || level.isClientSide) return;
        fluidDirtyOrigins.computeIfAbsent(level, k -> new ArrayDeque<>()).add(origin);
        fluidDebounce.put(level, DEBOUNCE_TICKS);
    }

    public void markElectricDirty(Level level, BlockPos origin) {
        if (level == null || level.isClientSide) return;
        electricDirtyOrigins.computeIfAbsent(level, k -> new ArrayDeque<>()).add(origin);
        electricDebounce.put(level, DEBOUNCE_TICKS);
    }

    // -------------------------
    // serverTick (ワールド毎に呼ぶ)
    // -------------------------
    public void serverTick(Level level) {
        if (level == null || level.isClientSide) return;
        // Fluid side
        if (fluidDebounce.containsKey(level)) {
            int left = fluidDebounce.get(level) - 1;
            fluidDebounce.put(level, left);
            if (left <= 0) {
                // process up to MAX_REBUILDS_PER_TICK origins this tick
                Deque<BlockPos> q = fluidDirtyOrigins.getOrDefault(level, new ArrayDeque<>());
                int cnt = 0;
                while (!q.isEmpty() && cnt < MAX_REBUILDS_PER_TICK) {
                    BlockPos origin = q.poll();
                    rebuildFluidForOrigin((ServerLevel) level, origin);
                    cnt++;
                }
                // if leftover origins remain, keep a debounce so they will process next tick
                if (!q.isEmpty()) {
                    fluidDebounce.put(level, DEBOUNCE_TICKS);
                    fluidDirtyOrigins.put(level, q);
                } else {
                    fluidDebounce.remove(level);
                    fluidDirtyOrigins.remove(level);
                }
            }
        }

        // Electric side (同様)
        if (electricDebounce.containsKey(level)) {
            int left = electricDebounce.get(level) - 1;
            electricDebounce.put(level, left);
            if (left <= 0) {
                Deque<BlockPos> q = electricDirtyOrigins.getOrDefault(level, new ArrayDeque<>());
                int cnt = 0;
                while (!q.isEmpty() && cnt < MAX_REBUILDS_PER_TICK) {
                    BlockPos origin = q.poll();
                    rebuildElectricForOrigin((ServerLevel) level, origin);
                    cnt++;
                }
                if (!q.isEmpty()) {
                    electricDebounce.put(level, DEBOUNCE_TICKS);
                    electricDirtyOrigins.put(level, q);
                } else {
                    electricDebounce.remove(level);
                    electricDirtyOrigins.remove(level);
                }
            }
        }

        // そして既存のネットワークを tick（軽量）
        List<com.infinitymax.industry.fluid.FluidNetwork> fns = fluidNetworks.get(level);
        if (fns != null) for (var fn : fns) fn.tick(level);

        List<com.infinitymax.industry.energy.ElectricNetwork> ens = electricNetworks.get(level);
        if (ens != null) for (var en : ens) en.tick(level);
    }

    // -------------------------
    // 再構築ロジック
    //   - origin が指定されれば局所 discover (discoverFromOrigins)
    //   - origin が null なら全再構築 discoverAll
    // -------------------------
    private void rebuildFluidForOrigin(ServerLevel level, BlockPos origin) {
        // origin を中心に discover を行う（局所再構築）
        Set<BlockPos> origins = new HashSet<>();
        if (origin != null) origins.add(origin);
        List<com.infinitymax.industry.fluid.FluidNetwork> found = com.infinitymax.industry.fluid.FluidNetwork.discoverFromOrigins(level, origins);
        // Merge: replace or append networks into stored list. simplest: recompute full list by discoverAll fallback
        if (found.isEmpty()) {
            // fallback 全探索（安全）
            List<com.infinitymax.industry.fluid.FluidNetwork> all = com.infinitymax.industry.fluid.FluidNetwork.discoverAll(level);
            fluidNetworks.put(level, all);
        } else {
            // integrate: replace networks that overlap with found origins
            List<com.infinitymax.industry.fluid.FluidNetwork> existing = fluidNetworks.getOrDefault(level, new ArrayList<>());
            // remove any existing networks that intersect found nodes
            Set<BlockPos> touched = new HashSet<>();
            for (var fn : found) touched.addAll(fn.getNodePositions());
            List<com.infinitymax.industry.fluid.FluidNetwork> remaining = new ArrayList<>();
            for (var ex : existing) {
                if (Collections.disjoint(ex.getNodePositions(), touched)) remaining.add(ex);
            }
            // combine
            remaining.addAll(found);
            fluidNetworks.put(level, remaining);
        }
    }

    private void rebuildElectricForOrigin(ServerLevel level, BlockPos origin) {
        Set<BlockPos> origins = new HashSet<>();
        if (origin != null) origins.add(origin);
        List<com.infinitymax.industry.energy.ElectricNetwork> found = com.infinitymax.industry.energy.ElectricNetwork.discoverFromOrigins(level, origins);
        if (found.isEmpty()) {
            List<com.infinitymax.industry.energy.ElectricNetwork> all = com.infinitymax.industry.energy.ElectricNetwork.discoverAll(level);
            electricNetworks.put(level, all);
        } else {
            List<com.infinitymax.industry.energy.ElectricNetwork> existing = electricNetworks.getOrDefault(level, new ArrayList<>());
            Set<BlockPos> touched = new HashSet<>();
            for (var en : found) touched.addAll(en.getNodePositions());
            List<com.infinitymax.industry.energy.ElectricNetwork> remaining = new ArrayList<>();
            for (var ex : existing) {
                if (Collections.disjoint(ex.getNodePositions(), touched)) remaining.add(ex);
            }
            remaining.addAll(found);
            electricNetworks.put(level, remaining);
        }
    }

    // -------------------------
    // 外からネットワークリストを参照したい場合の getter
    // -------------------------
    public List<com.infinitymax.industry.fluid.FluidNetwork> getFluidNetworks(Level level) {
        return fluidNetworks.getOrDefault(level, Collections.emptyList());
    }

    public List<com.infinitymax.industry.energy.ElectricNetwork> getElectricNetworks(Level level) {
        return electricNetworks.getOrDefault(level, Collections.emptyList());
    }
}
