package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * ElectricNetwork (改良版)
 *
 * - discoverFromOrigins(level, origins) を実装して局所再構築を可能にした
 * - discoverAll(level) はチャンク走査優先 / フォールバックを用いる
 * - tick(level) は既存の単純配分アルゴリズムのまま（負荷低減のためネットワーク単位で実行）
 */
public final class ElectricNetwork {

    private final Set<BlockPos> nodes;

    public ElectricNetwork(Set<BlockPos> nodes) {
        this.nodes = new HashSet<>(nodes);
    }

    public Set<BlockPos> getNodePositions() { return Collections.unmodifiableSet(nodes); }

    public static List<ElectricNetwork> discoverFromOrigins(ServerLevel level, Set<BlockPos> origins) {
        List<ElectricNetwork> out = new ArrayList<>();
        if (origins == null || origins.isEmpty()) return discoverAll(level);

        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos origin : origins) {
            if (visited.contains(origin)) continue;
            BlockEntity be = level.getBlockEntity(origin);
            if (!(be instanceof IElectricNode)) continue;
            Set<BlockPos> comp = flood(level, origin);
            if (!comp.isEmpty()) {
                visited.addAll(comp);
                out.add(new ElectricNetwork(comp));
            }
        }
        return out;
    }

    public static List<ElectricNetwork> discoverAll(ServerLevel level) {
        List<ElectricNetwork> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Iterable<BlockEntity> allBE = getAllBlockEntities(level);
        for (BlockEntity be : allBE) {
            BlockPos pos = be.getBlockPos();
            if (visited.contains(pos)) continue;
            if (!(be instanceof IElectricNode)) continue;
            Set<BlockPos> comp = flood(level, pos);
            if (!comp.isEmpty()) {
                visited.addAll(comp);
                out.add(new ElectricNetwork(comp));
            }
        }
        return out;
    }

    private static Set<BlockPos> flood(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!visited.add(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IElectricNode)) continue;
            for (BlockPos n : neighbors(p)) {
                if (!visited.contains(n)) {
                    BlockEntity nb = level.getBlockEntity(n);
                    if (nb instanceof IElectricNode) q.add(n);
                }
            }
        }
        return visited;
    }

    private static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        List<IElectricNode> sources = new ArrayList<>();
        List<IElectricNode> loads = new ArrayList<>();
        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IElectricNode n)) continue;
            if (n.getMaxOutputA() > 0.0) sources.add(n);
            if (n.getMaxIntakeA() > 0.0) loads.add(n);
        }
        if (sources.isEmpty() || loads.isEmpty()) return;

        double maxVoltage = sources.stream().mapToDouble(IElectricNode::getVoltageV).max().orElse(0.0);
        double totalSupplyA = sources.stream().mapToDouble(IElectricNode::getMaxOutputA).sum();
        double totalDemandA = loads.stream().mapToDouble(IElectricNode::getMaxIntakeA).sum();
        double supplyA = Math.min(totalSupplyA, totalDemandA);
        if (supplyA <= 0.0) return;

        for (IElectricNode load : loads) {
            double share = supplyA * (load.getMaxIntakeA() / Math.max(1e-9, totalDemandA));
            load.pushPullCurrent(level, asBlockPos(load), maxVoltage, +share);
        }
        for (IElectricNode src : sources) {
            double share = supplyA * (src.getMaxOutputA() / Math.max(1e-9, totalSupplyA));
            src.pushPullCurrent(level, asBlockPos(src), maxVoltage, -share);
        }
    }

    public void onRebuilt(Level level) {}

    private static BlockPos asBlockPos(IElectricNode n) {
        if (n instanceof BlockEntity be) return be.getBlockPos();
        throw new IllegalStateException("IElectricNode must be BlockEntity");
    }

    // getAllBlockEntities: FluidNetwork と共通の高速経路/フォールバックを使う
    private static Iterable<BlockEntity> getAllBlockEntities(ServerLevel level) {
        return com.infinitymax.industry.fluid.FluidNetwork.getAllBlockEntities(level);
    }
}