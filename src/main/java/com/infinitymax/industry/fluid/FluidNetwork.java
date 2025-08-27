package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * FluidNetwork (改良版)
 *
 * - discoverAll(level): ワールド内の全ネットワークを発見（チャンク走査を試み、できなければフォールバック） 
 * - discoverFromOrigins(level, origins): 起点集合から局所的に flood を行い、そのコンポーネントのみ返す（高速）
 * - tick(level): ネットワーク単位での流体配分（既存ロジックの踏襲）
 *
 * 注意:
 * - getAllBlockEntities(ServerLevel) は環境依存なので「チャンク走査最速経路」を試み、失敗時は level.blockEntities.values() のようなフォールバックを使います。
 */
public final class FluidNetwork {

    private final Set<BlockPos> nodes;

    public FluidNetwork(Set<BlockPos> nodes) {
        this.nodes = new HashSet<>(nodes);
    }

    public Set<BlockPos> getNodePositions() { return Collections.unmodifiableSet(nodes); }

    // -----------------------
    // discoverFromOrigins: origins を起点に局所探索してネットワーク群を返す
    // -----------------------
    public static List<FluidNetwork> discoverFromOrigins(ServerLevel level, Set<BlockPos> origins) {
        List<FluidNetwork> out = new ArrayList<>();
        if (origins == null || origins.isEmpty()) {
            // fallback to full discover
            return discoverAll(level);
        }

        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos origin : origins) {
            if (visited.contains(origin)) continue;
            BlockEntity be = level.getBlockEntity(origin);
            if (!(be instanceof IPressureNode)) continue;
            // flood from origin
            Set<BlockPos> comp = flood(level, origin);
            if (!comp.isEmpty()) {
                visited.addAll(comp);
                out.add(new FluidNetwork(comp));
            }
        }
        return out;
    }

    // -----------------------
    // discoverAll: ワールド内の全ネットワークを探す（チャンク走査優先、失敗時フォールバック）
    // -----------------------
    public static List<FluidNetwork> discoverAll(ServerLevel level) {
        List<FluidNetwork> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        Iterable<BlockEntity> allBE = getAllBlockEntities(level);

        for (BlockEntity be : allBE) {
            BlockPos pos = be.getBlockPos();
            if (visited.contains(pos)) continue;
            if (!(be instanceof IPressureNode)) continue;

            Set<BlockPos> comp = flood(level, pos);
            if (!comp.isEmpty()) {
                visited.addAll(comp);
                out.add(new FluidNetwork(comp));
            }
        }
        return out;
    }

    // flood: start から連結 IPressureNode を BFS 収集
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

    // -----------------------
    // tick: ネットワーク単位の流体計算（既存ロジックの簡潔な再現）
    // -----------------------
    public void tick(Level level) {
        if (nodes.isEmpty()) return;

        Medium medium = null;
        List<IPressureNode> sources = new ArrayList<>();
        List<IPressureNode> sinks = new ArrayList<>();

        for (BlockPos p : nodes) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof IPressureNode n)) continue;
            if (medium == null) medium = n.getMedium();
            if (n.getMedium() != medium) continue;
            if (n.getAmountmB() > 0 && n.getMaxFlowOutputPerTick() > 0) sources.add(n);
            if (n.getAmountmB() < n.getCapacitymB() && n.getMaxFlowIntakePerTick() > 0) sinks.add(n);
        }
        if (medium == null || sources.isEmpty() || sinks.isEmpty()) return;

        int totalOutCap = sources.stream().mapToInt(IPressureNode::getMaxFlowOutputPerTick).sum();
        int totalInCap  = sinks.stream().mapToInt(IPressureNode::getMaxFlowIntakePerTick).sum();
        int flow = Math.min(totalOutCap, totalInCap);
        if (flow <= 0) return;

        for (IPressureNode s : sources) {
            int share = (int)Math.round(flow * (s.getMaxFlowOutputPerTick() / (double)Math.max(1, totalOutCap)));
            s.flow(level, asBlockPos(s), -share);
        }
        for (IPressureNode d : sinks) {
            int share = (int)Math.round(flow * (d.getMaxFlowIntakePerTick() / (double)Math.max(1, totalInCap)));
            d.flow(level, asBlockPos(d), +share);
        }
    }

    public void onRebuilt(Level level) {
        // optional hook
    }

    // -----------------------
    // Helpers
    // -----------------------
    private static BlockPos asBlockPos(IPressureNode n) {
        if (n instanceof BlockEntity be) return be.getBlockPos();
        throw new IllegalStateException("IPressureNode must be BlockEntity");
    }

    /**
     * できるだけ高速に「ロード済み BlockEntity」を列挙する。
     *  - まずチャンク走査で得る方法を試みる（高速）
     *  - 失敗する/アクセスできない場合は level.blockEntities.values() 相当を使うフォールバック
     *
     *  *注意* : 各環境（NeoForge/Forge/Fabric）でチャンク管理 API の名前は異なります。
     * もしコンパイルエラーが出たら、ここだけあなたの環境の「loaded chunk iterator」に置き換えてください。
     */
    @SuppressWarnings("unchecked")
    private static Iterable<BlockEntity> getAllBlockEntities(ServerLevel level) {
        try {
            // --- 試行 1: ServerLevel のチャンクプロバイダ経由で loaded chunk を列挙する高速経路 ---
            // ここでは一般的な実装名を試すが、環境により異なるため try/catch で保護。
            var chunkSource = level.getChunkSource();
            // chunkSource.chunkMap (Fabric/modern) など異なる実装がある。
            // 下のリフレクションは安全策：取得できればチャンクの block entities を返す。
            java.lang.reflect.Field f = chunkSource.getClass().getDeclaredField("chunkMap");
            f.setAccessible(true);
            Object chunkMap = f.get(chunkSource);
            if (chunkMap != null) {
                // chunkMap の loadedChunks などを探す（環境に依存）
                // 試しに "chunks" フィールドを探す
                java.lang.reflect.Field cf = chunkMap.getClass().getDeclaredField("chunks");
                cf.setAccessible(true);
                Object chunksObj = cf.get(chunkMap);
                if (chunksObj instanceof java.util.Collection<?> coll) {
                    // collect block entities from each chunk
                    List<BlockEntity> out = new ArrayList<>();
                    for (Object c : coll) {
                        try {
                            // try common possibilities: chunk.blockEntities, chunk.getBlockEntities()
                            java.lang.reflect.Field beField = c.getClass().getDeclaredField("blockEntities");
                            beField.setAccessible(true);
                            Object beMap = beField.get(c);
                            if (beMap instanceof java.util.Map<?, ?> map) {
                                for (Object v : map.values()) if (v instanceof BlockEntity be) out.add(be);
                            }
                        } catch (NoSuchFieldException ignored) {
                            // fallback: try method getBlockEntities()
                            try {
                                var method = c.getClass().getMethod("getBlockEntities");
                                Object beCol = method.invoke(c);
                                if (beCol instanceof java.util.Collection<?> bc) {
                                    for (Object o : bc) if (o instanceof BlockEntity be) out.add(be);
                                }
                            } catch (Exception ignored2) {}
                        } catch (Exception ex) {
                            // ignore chunk-level errors
                        }
                    }
                    return out;
                }
            }
        } catch (Throwable ignored) {
            // reflection path failed -> fallback below
        }

        // --- フォールバック: level.blockEntities.values() のような実装を試す ---
        try {
            java.lang.reflect.Field beField = level.getClass().getDeclaredField("blockEntities");
            beField.setAccessible(true);
            Object map = beField.get(level);
            if (map instanceof java.util.Map<?, ?> m) {
                List<BlockEntity> out = new ArrayList<>();
                for (Object v : m.values()) if (v instanceof BlockEntity be) out.add(be);
                return out;
            }
        } catch (Throwable ignored) {}

        // 最終フォールバック: 空コレクション（※ 本来は何らかの列挙が必要）
        return Collections.emptyList();
    }
}