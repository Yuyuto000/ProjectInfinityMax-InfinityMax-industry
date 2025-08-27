package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * FluidNetwork:
 * - 1つの連結コンポーネント（パイプ・タンクなど IPressureNode が繋がった集合）を表す
 * - discoverAll(level) でワールド内の全ネットワークを列挙できる
 * - tick(level) でネットワーク単位の流体バランスを計算する
 *
 * 注: discoverAll の全 BE スキャン部は環境依存。必要なら project の "all blockentity iterator" に合わせて置換してください。
 */
public final class FluidNetwork {

    private final Set<BlockPos> nodes;

    public FluidNetwork(Set<BlockPos> nodes) {
        this.nodes = new HashSet<>(nodes);
    }

    /** ワールド内の全 FluidNetwork を発見して List で返す（再構築用） */
    public static List<FluidNetwork> discoverAll(ServerLevel level) {
        List<FluidNetwork> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        // 重要: ここは「ワールドの全 BlockEntity を列挙できる API」に差し替えてください。
        // 例: level.blockEntityList (サンプル), あるいは level.getChunkSource().chunkMap... など
        // ここでは簡潔のため仮のイテレータを使っています。
        Iterable<BlockEntity> allBE = getAllBlockEntities(level);

        for (BlockEntity be : allBE) {
            BlockPos pos = be.getBlockPos();
            if (visited.contains(pos)) continue;
            if (!(be instanceof IPressureNode)) continue;

            // BFS flood
            Set<BlockPos> comp = flood(level, pos);
            visited.addAll(comp);
            out.add(new FluidNetwork(comp));
        }
        return out;
    }

    // ==== floodfill: start から繋がる IPressureNode の座標集合を返す ====
    private static Set<BlockPos> flood(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(start);

        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!visited.add(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IPressureNode)) continue;
            // 隣接を列挙して queue に追加
            for (BlockPos n : neighbors(p)) {
                if (!visited.contains(n)) {
                    BlockEntity nbe = level.getBlockEntity(n);
                    if (nbe instanceof IPressureNode) q.add(n);
                }
            }
        }
        return visited;
    }

    private static List<BlockPos> neighbors(BlockPos p) {
        return List.of(p.above(), p.below(), p.north(), p.south(), p.east(), p.west());
    }

    /** ネットワーク全体の tick（流体移送計算） */
    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        // ノードを収集し、同一媒体のソース／シンクを分類
        Medium medium = null;
        List<IPressureNode> sources = new ArrayList<>();
        List<IPressureNode> sinks = new ArrayList<>();

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IPressureNode n)) continue;
            if (medium == null) medium = n.getMedium();
            if (n.getMedium() != medium) continue; // 異なる流体は混ぜない
            if (n.getAmountmB() > 0 && n.getMaxFlowOutputPerTick() > 0) sources.add(n);
            if (n.getAmountmB() < n.getCapacitymB() && n.getMaxFlowIntakePerTick() > 0) sinks.add(n);
        }
        if (medium == null || sources.isEmpty() || sinks.isEmpty()) return;

        // 単純比例配分（差圧を使っても良いがまずは容量比）
        int totalOutCap = sources.stream().mapToInt(IPressureNode::getMaxFlowOutputPerTick).sum();
        int totalInCap  = sinks.stream().mapToInt(IPressureNode::getMaxFlowIntakePerTick).sum();
        int flow = Math.min(totalOutCap, totalInCap);
        if (flow <= 0) return;

        for (IPressureNode s : sources) {
            int share = (int)Math.round(flow * (s.getMaxFlowOutputPerTick() / (double)Math.max(1, totalOutCap)));
            s.flow(level, sBlockPos(s), -share); // 負＝供出
        }
        for (IPressureNode d : sinks) {
            int share = (int)Math.round(flow * (d.getMaxFlowIntakePerTick() / (double)Math.max(1, totalInCap)));
            d.flow(level, sBlockPos(d), +share); // 正＝受入
        }
    }

    /** onRebuilt フック（必要なら内部初期化を行う） */
    public void onRebuilt(Level level) {
        // ここでは何もしないが、ログや統計、初期均衡などが必要なら実装
    }

    // ==== ヘルパ: IPressureNode -> BlockPos (安全取得) ====
    private static BlockPos sBlockPos(IPressureNode n) {
        if (n instanceof net.minecraft.world.level.block.entity.BlockEntity be) return be.getBlockPos();
        throw new IllegalStateException("IPressureNode must be a BlockEntity in this implementation");
    }

    // ==== ここはプロジェクトごとの全BE取得に置換してください ====
    private static Iterable<BlockEntity> getAllBlockEntities(ServerLevel level) {
        // *** 注意 ***
        // ここは実際のプロジェクトの API に合わせて書き換えてください。
        // 例: level.blockEntityList (一部の環境) や level.getChunkSource().chunkMap... など。
        // ここではサンプルとして level.blockEntities.values() 相当を仮定します。
        return level.blockEntities.values();
    }
}