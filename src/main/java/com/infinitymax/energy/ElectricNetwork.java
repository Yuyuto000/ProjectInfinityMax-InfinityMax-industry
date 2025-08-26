package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * 1つの連結グラフとしての電気ネットワーク。
 * - 電源/負荷/導体ノードを保持
 * - 毎tick電流配分を実行
 */
public class ElectricNetwork {

    private final Set<BlockPos> nodes = new HashSet<>();

    public ElectricNetwork(Set<BlockPos> nodes) {
        this.nodes.addAll(nodes);
    }

    /** ワールド内の全ElectricNetworkを探索 */
    public static List<ElectricNetwork> discoverAll(Level level) {
        Set<BlockPos> visited = new HashSet<>();
        List<ElectricNetwork> result = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                level.getMinBuildHeight(),
                level.getMinBuildHeight(),
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                level.getMaxBuildHeight(),
                level.getMaxBuildHeight()
        )) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IElectricNode)) continue;
            if (visited.contains(pos)) continue;

            Set<BlockPos> comp = flood(level, pos);
            visited.addAll(comp);
            result.add(new ElectricNetwork(comp));
        }
        return result;
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
                BlockEntity nbe = level.getBlockEntity(n);
                if (nbe instanceof IElectricNode && !visited.contains(n)) {
                    q.add(n);
                }
            }
        }
        return visited;
    }

    private static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    /** 毎tickで電力バランスを計算 */
    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        List<IElectricNode> sources = new ArrayList<>();
        List<IElectricNode> loads   = new ArrayList<>();
        double maxVoltage = 0;

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IElectricNode n)) continue;

            if (n.getMaxOutputA() > 0) {
                sources.add(n);
                if (n.getVoltageV() > maxVoltage) maxVoltage = n.getVoltageV();
            } else if (n.getMaxIntakeA() > 0) {
                loads.add(n);
            }
        }
        if (sources.isEmpty() || loads.isEmpty()) return;

        double totalSupply = sources.stream().mapToDouble(IElectricNode::getMaxOutputA).sum();
        double totalDemand = loads.stream().mapToDouble(IElectricNode::getMaxIntakeA).sum();
        double currentA = Math.min(totalSupply, totalDemand);

        // 負荷へ配分
        for (IElectricNode load : loads) {
            double share = currentA * (load.getMaxIntakeA() / totalDemand);
            load.pushPullCurrent(level, null, maxVoltage, +share);
        }
        // 電源から引く
        for (IElectricNode src : sources) {
            double share = currentA * (src.getMaxOutputA() / totalSupply);
            src.pushPullCurrent(level, null, maxVoltage, -share);
        }
    }
}