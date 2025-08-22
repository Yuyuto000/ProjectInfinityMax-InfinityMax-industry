package com.infinitymax.industry.energy;

/** 機械側の「電力ポート」。MachineBlockEntity が実装or委譲 */
public interface IElectricPort {
    /** この tick で必要な仕事量[J]（=W/tick 換算）。0 なら停止中 */
    double requiredWorkJPerTick();

    /** 指定された電圧[V]／電流[A] で受電できる分だけ受け取り、実際に消費した[A] を返却 */
    double acceptPowerVA(double voltageV, double maxCurrentA);

    /** 内部の定格情報（デバッグ用途） */
    double ratedVoltageV();
    double ratedCurrentA();
}
