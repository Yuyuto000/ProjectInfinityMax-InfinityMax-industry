package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IPressureNode {
    /** 現在の圧力[kPa] */
    double getPressureKPa();

    /** 内部容積[mB]（=milliBucket） */
    int getCapacitymB();

    /** 現在の流体量[mB] */
    int getAmountmB();

    /** 媒体 */
    Medium getMedium();

    /** 受入可能な最大流量[mB/tick] */
    int getMaxFlowIntakePerTick();

    /** 供給可能な最大流量[mB/tick] */
    int getMaxFlowOutputPerTick();

    /** 圧力差に応じて流入（＋）／流出（－）。戻り値＝実移動量[mB]（符号付き） */
    int flow(Level level, BlockPos pos, int requestedmB);

    /** グラフ変更 */
    void markDirtyGraph();
}
