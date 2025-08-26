package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IElectricNode {
    /** ノードの内部状態（電圧[V], 内部抵抗[Ω], 受給能力など）をネットワークから参照するためのIF */

    /** 現在の端子電圧[V]（見かけ） */
    double getVoltageV();

    /** このノードの内部抵抗[Ω]（0 近いほど理想電源／導体） */
    double getInternalResistanceOhm();

    /** このノードが受電に使える最大電流[A]（保護目的） */
    double getMaxIntakeA();

    /** このノードが供給できる最大電流[A]（電源側なら >0） */
    double getMaxOutputA();

    /** 指定電圧で電流を供給（＋）／吸い込み（－）要求。戻り値は実際に流れた電流[A] */
    double pushPullCurrent(Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA);

    /** ネットワーク再スキャン要求（隣接が変化した時に呼ぶ） */
    void markDirtyGraph();
}
