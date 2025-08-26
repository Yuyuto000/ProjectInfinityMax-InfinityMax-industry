package com.infinitymax.industry.energy;

/** 物理単位の保持・ユーティリティ */
public final class PowerUnits {
    private PowerUnits() {}

    // 時間変換
    public static final double TICK_PER_SECOND = 20.0;

    // 送電の基本：P[W] = V[V] * I[A]
    public static double powerW(double voltageV, double currentA) {
        return voltageV * currentA;
    }

    // 損失（簡易）：I^2 * R * dt
    public static double resistiveLossJ(double currentA, double resistanceOhm, double tickDelta) {
        return (currentA * currentA) * resistanceOhm * (tickDelta / TICK_PER_SECOND);
    }
}
