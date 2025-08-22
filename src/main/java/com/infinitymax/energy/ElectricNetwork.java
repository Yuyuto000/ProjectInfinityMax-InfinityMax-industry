package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * 近傍BFSで「同一グラフ」を1tickだけ集約評価。
 * 近似：同一グラフ内は目標電圧へ緩和、需要に応じて電流を配分し、導体抵抗で損失。
 * 超軽量：チャンク内 or 半径 N で切る。
 */
public final class ElectricNetwork {
    private ElectricNetwork() {}

    /** 隣接ノード探索（6方向のみ） */
    public static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    /** グラフ構築（半径制限） */
    public static Set<BlockPos> flood(Level level, BlockPos origin, int maxNodes) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(origin);
        while (!q.isEmpty() && visited.size() < maxNodes) {
            BlockPos pos = q.poll();
            if (!visited.add(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IElectricNode)) continue;
            for (BlockPos n : neighbors(pos)) {
                BlockEntity nbe = level.getBlockEntity(n);
                if (nbe instanceof IElectricNode) q.add(n);
            }
        }
        return visited;
    }

    /**
     * 1tick更新：同一グラフ内で
     *  - 電源ノードの目標電圧（高い方）に緩和
     *  - 需要ノードに I を配分
     *  - 損失 I^2R を見かけのエネルギー損失に計上
     */
    public static void tick(Level level, BlockPos anyNode, double tickDelta, int maxNodes) {
        Set<BlockPos> nodes = flood(level, anyNode, maxNodes);
        if (nodes.isEmpty()) return;

        // 収集
        double maxSourceVoltage = 0.0;
        List<IElectricNode> sources = new ArrayList<>();
        List<IElectricNode> loads = new ArrayList<>();
        List<IElectricNode> conductors = new ArrayList<>();

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            IElectricNode n = (IElectricNode) be;
            double v = n.getVoltageV();
            if (n.getMaxOutputA() > 0.0) { // 電源
                sources.add(n);
                if (v > maxSourceVoltage) maxSourceVoltage = v;
            } else if (n.getMaxIntakeA() > 0.0) { // 負荷
                loads.add(n);
            } else { // 純導体
                conductors.add(n);
            }
        }
        if (sources.isEmpty() && loads.isEmpty()) return;

        double totalAvailableA = sources.stream().mapToDouble(IElectricNode::getMaxOutputA).sum();
        double totalLoadNeedA = loads.stream().mapToDouble(IElectricNode::getMaxIntakeA).sum();
        double supplyA = Math.min(totalAvailableA, totalLoadNeedA);
        if (supplyA <= 0) return;

        // 配分（シンプル：負荷の MaxIntake 比例配分）
        for (IElectricNode load : loads) {
            double share = supplyA * (load.getMaxIntakeA() / Math.max(totalLoadNeedA, 1e-9));
            double flowed = load.pushPullCurrent(level, null, maxSourceVoltage, +share);
            // 損失は各ノードの内部抵抗に依存（ケーブルBE側で内部抵抗を持つ）
            double lossJ = PowerUnits.resistiveLossJ(flowed, load.getInternalResistanceOhm(), tickDelta);
            // ここでは結果を使わない（発熱表現など後で）
        }
        // 電源側からも実際に出力させて電流バランスを合わせる
        for (IElectricNode src : sources) {
            double outA = Math.min(src.getMaxOutputA(), supplyA * (src.getMaxOutputA() / Math.max(totalAvailableA, 1e-9)));
            double flowed = src.pushPullCurrent(level, null, maxSourceVoltage, -outA); // 供給＝負電流で
            double lossJ = PowerUnits.resistiveLossJ(Math.abs(flowed), src.getInternalResistanceOhm(), tickDelta);
        }
        // 導体ノードは見かけ電圧をソース電圧へ緩和（後述ケーブルBE側で反映）
    }
}
