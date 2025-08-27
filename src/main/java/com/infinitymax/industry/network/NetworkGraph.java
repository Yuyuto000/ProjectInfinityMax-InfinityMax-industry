package com.infinitymax.industry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class NetworkGraph {

    private final ServerLevel level;
    private final Map<BlockPos, INetworkNode> nodes = new HashMap<>();
    private final List<Set<INetworkNode>> components = new ArrayList<>();

    public NetworkGraph(ServerLevel level) {
        this.level = level;
    }

    /** ノード探索してグラフを構築 */
    public void rebuild() {
        nodes.clear();
        components.clear();

        // レベル内の BlockEntity を走査（効率を考えて専用リストにしても良い）
        for (var be : level.blockEntityList) {
            if (be instanceof INetworkNode node) {
                nodes.put(be.getBlockPos(), node);
            }
        }

        // コンポーネント分割（BFS）
        Set<BlockPos> visited = new HashSet<>();
        for (var entry : nodes.entrySet()) {
            if (visited.contains(entry.getKey())) continue;

            Set<INetworkNode> comp = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(entry.getKey());

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                if (!visited.add(pos)) continue;

                INetworkNode node = nodes.get(pos);
                if (node == null) continue;
                comp.add(node);

                for (BlockPos npos : node.getConnectedPositions()) {
                    if (!visited.contains(npos) && nodes.containsKey(npos)) {
                        queue.add(npos);
                    }
                }
            }
            components.add(comp);
        }
    }

    public List<Set<INetworkNode>> getComponents() {
        return components;
    }
}