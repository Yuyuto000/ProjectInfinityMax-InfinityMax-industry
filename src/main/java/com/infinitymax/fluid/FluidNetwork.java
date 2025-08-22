package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public final class FluidNetwork {
    private FluidNetwork() {}

    public static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    public static Set<BlockPos> flood(Level level, BlockPos origin, int maxNodes) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(origin);
        while (!q.isEmpty() && visited.size() < maxNodes) {
            BlockPos pos = q.poll();
            if (!visited.add(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IPressureNode)) continue;
            for (BlockPos n : neighbors(pos)) {
                BlockEntity nbe = level.getBlockEntity(n);
                if (nbe instanceof IPressureNode) q.add(n);
            }
        }
        return visited;
    }

    /** 差圧に基づき mB を移送（非常にシンプルな近似） */
    public static void tick(Level level, BlockPos anyNode, int maxNodes) {
        Set<BlockPos> nodes = flood(level, anyNode, maxNodes);
        if (nodes.isEmpty()) return;

        // 同一媒体のみを混合可とする（最初のノード基準）
        Medium medium = null;
        List<IPressureNode> sources = new ArrayList<>();
        List<IPressureNode> sinks = new ArrayList<>();

        for (BlockPos p : nodes) {
            IPressureNode n = (IPressureNode) level.getBlockEntity(p);
            if (n.getAmountmB() <= 0 && n.getMaxFlowIntakePerTick() > 0) {
                sinks.add(n);
            } else {
                if (medium == null) medium = n.getMedium();
                if (n.getMedium() == medium) {
                    if (n.getMaxFlowOutputPerTick() > 0 && n.getAmountmB() > 0) sources.add(n);
                    if (n.getMaxFlowIntakePerTick() > 0) sinks.add(n);
                }
            }
        }
        if (medium == null) return;

        // 全体圧力バランス：高圧→低圧へ配分（配管抵抗などはノード側で表現可能）
        double pMax = sources.stream().mapToDouble(IPressureNode::getPressureKPa).max().orElse(0);
        double pMin = sinks.stream().mapToDouble(IPressureNode::getPressureKPa).min().orElse(pMax);

        if (pMax <= pMin) return; // 差圧なし

        int totalOutCap = sources.stream().mapToInt(IPressureNode::getMaxFlowOutputPerTick).sum();
        int totalInCap  = sinks.stream().mapToInt(IPressureNode::getMaxFlowIntakePerTick).sum();
        int flow = Math.min(totalOutCap, totalInCap);
        if (flow <= 0) return;

        // 比例配分
        for (IPressureNode s : sources) {
            int share = (int)Math.round(flow * (s.getMaxFlowOutputPerTick() / (double)Math.max(totalOutCap, 1)));
            int moved = s.flow(level, null, -share); // 供給は負
        }
        for (IPressureNode d : sinks) {
            int share = (int)Math.round(flow * (d.getMaxFlowIntakePerTick() / (double)Math.max(totalInCap, 1)));
            int moved = d.flow(level, null, +share); // 受入は正
        }
    }
}
