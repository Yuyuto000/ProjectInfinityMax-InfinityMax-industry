package com.infinitymax.industry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface INetworkNode {
    BlockPos getPosition();
    Level getLevel();

    /** 隣接ノードの座標を返す */
    List<BlockPos> getConnectedPositions();

    /** ネットワーク再構築時に呼ばれるフック */
    default void onGraphRebuild() {}
}