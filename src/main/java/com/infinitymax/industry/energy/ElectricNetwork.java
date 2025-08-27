package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * ElectricNetwork:
 * - IElectricNode の連結コンポーネントを表す
 * - discoverAll(level) でワールド内の全電気ネットワークを返す
 * - tick(level) で電力配分（単純モデル）を実行
 */
public final class ElectricNetwork {

    private final Set<BlockPos> nodes;

    public ElectricNetwork(Set<BlockPos> nodes) {
        this.nodes = new HashSet<>(nodes);
    }

    /** 全ネットワーク発見 */
    public static List<ElectricNetwork> discoverAll(ServerLevel level) {
        List<ElectricNetwork> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        Iterable<BlockEntity> allBE = getAllBlockEntities(level);

        for (BlockEntity be : allBE) {
            BlockPos pos = be.getBlockPos();
            if (visited.contains(pos)) continue;
            if (!(be instanceof IElectricNode)) continue;

            Set<BlockPos> comp = flood(level, pos);
            visited.addAll(comp);
            out.add(new ElectricNetwork(comp));
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
                    BlockEntity nbe = level.getBlockEntity(n);
                    if (nbe instanceof IElectricNode) q.add(n);
                }
            }
        }
        return visited;
    }

    private static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    /** ネットワーク内の電力バランスを単純に配分する */
    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        List<IElectricNode> sources = new ArrayList<>();
        List<IElectricNode> loads = new ArrayList<>();

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IElectricNode n)) continue;
            if (n.getMaxOutputA() > 0.0) sources.add(n); // 供給可能
            if (n.getMaxIntakeA() > 0.0) loads.add(n);   // 需要
        }
        if (sources.isEmpty() || loads.isEmpty()) return;

        double maxVoltage = sources.stream().mapToDouble(IElectricNode::getVoltageV).max().orElse(0.0);
        double totalSupplyA = sources.stream().mapToDouble(IElectricNode::getMaxOutputA).sum();
        double totalDemandA = loads.stream().mapToDouble(IElectricNode::getMaxIntakeA).sum();
        double supplyA = Math.min(totalSupplyA, totalDemandA);
        if (supplyA <= 0.0) return;

        // 負荷に比例配分
        for (IElectricNode load : loads) {
            double share = supplyA * (load.getMaxIntakeA() / Math.max(1e-9, totalDemandA));
            load.pushPullCurrent(level, sBlockPos(load), maxVoltage, +share);
        }

        // 電源から引く（供給＝負の current）
        for (IElectricNode src : sources) {
            double share = supplyA * (src.getMaxOutputA() / Math.max(1e-9, totalSupplyA));
            src.pushPullCurrent(level, sBlockPos(src), maxVoltage, -share);
        }
    }

    public void onRebuilt(Level level) {
        // 初期化・ログ等が必要ならここに
    }

    // ==== ヘルパ ====
    private static BlockPos sBlockPos(IElectricNode n) {
        if (n instanceof net.minecraft.world.level.block.entity.BlockEntity be) return be.getBlockPos();
        throw new IllegalStateException("IElectricNode must be a BlockEntity in this implementation");
    }

    // ==== ここはプロジェクト向けに差し替えてください ====
    private static Iterable<BlockEntity> getAllBlockEntities(ServerLevel level) {
        // 実環境に合わせて置換すること
        return level.blockEntities.values();
    }
}