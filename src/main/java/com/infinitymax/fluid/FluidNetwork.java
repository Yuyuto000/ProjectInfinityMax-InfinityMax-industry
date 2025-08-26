package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * 1つの連結グラフとしての流体ネットワーク。
 * - ノード(IPressureNode)の集合を保持
 * - tick時に流体を配分
 */
public class FluidNetwork {

    private final Set<BlockPos> nodes = new HashSet<>();

    public FluidNetwork(Set<BlockPos> nodes) {
        this.nodes.addAll(nodes);
    }

    /** ワールド内の全FluidNetworkを探索し直す */
    public static List<FluidNetwork> discoverAll(Level level) {
        Set<BlockPos> visited = new HashSet<>();
        List<FluidNetwork> result = new ArrayList<>();

        // 全BlockEntityを走査して IPressureNode のみ探索
        for (BlockPos pos : BlockPos.betweenClosed(
                level.getMinBuildHeight(),
                level.getMinBuildHeight(),
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                level.getMaxBuildHeight(),
                level.getMaxBuildHeight()
        )) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IPressureNode)) continue;
            if (visited.contains(pos)) continue;

            // flood fill で一つのネットワークを構築
            Set<BlockPos> comp = flood(level, pos);
            visited.addAll(comp);
            result.add(new FluidNetwork(comp));
        }
        return result;
    }

    /** floodfill: pos から繋がる IPressureNode 集合を取得 */
    private static Set<BlockPos> flood(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(start);

        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!visited.add(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IPressureNode)) continue;
            for (BlockPos n : neighbors(p)) {
                BlockEntity nbe = level.getBlockEntity(n);
                if (nbe instanceof IPressureNode && !visited.contains(n)) {
                    q.add(n);
                }
            }
        }
        return visited;
    }

    /** 6方向隣接 */
    private static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    /** 毎tick流体バランス処理 */
    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        // 同一ネットワーク内の全ノードを分類
        Medium medium = null;
        List<IPressureNode> sources = new ArrayList<>();
        List<IPressureNode> sinks = new ArrayList<>();

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IPressureNode n)) continue;
            if (medium == null) medium = n.getMedium();
            if (n.getMedium() != medium) continue; // 異なる流体は混ぜない

            if (n.getMaxFlowOutputPerTick() > 0 && n.getAmountmB() > 0) sources.add(n);
            if (n.getMaxFlowIntakePerTick() > 0 && n.getAmountmB() < n.getCapacitymB()) sinks.add(n);
        }
        if (medium == null || sources.isEmpty() || sinks.isEmpty()) return;

        // 差圧駆動の流量計算
        double pMax = sources.stream().mapToDouble(IPressureNode::getPressureKPa).max().orElse(0);
        double pMin = sinks.stream().mapToDouble(IPressureNode::getPressureKPa).min().orElse(pMax);
        if (pMax <= pMin) return;

        int totalOutCap = sources.stream().mapToInt(IPressureNode::getMaxFlowOutputPerTick).sum();
        int totalInCap  = sinks.stream().mapToInt(IPressureNode::getMaxFlowIntakePerTick).sum();
        int flow = Math.min(totalOutCap, totalInCap);
        if (flow <= 0) return;

        // 比例配分
        for (IPressureNode s : sources) {
            int share = (int) (flow * (s.getMaxFlowOutputPerTick() / (double) totalOutCap));
            s.flow(level, null, -share);
        }
        for (IPressureNode d : sinks) {
            int share = (int) (flow * (d.getMaxFlowIntakePerTick() / (double) totalInCap));
            d.flow(level, null, +share);
        }
    }
}