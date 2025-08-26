package com.infinitymax.industry.util;

/**
 * 簡易過熱ユーティリティ
 * - 温度管理を行い、閾値超過で故障・爆発フラグを返す
 */
public final class HeatHelper {
    private HeatHelper(){}

    public static double heatFromJoules(double joules, double massKg, double specificHeat) {
        // Q = m c ΔT  => ΔT = Q / (m c)
        return joules / (Math.max(0.001, massKg * specificHeat));
    }

    public static boolean checkOverheat(double temperatureC, double thresholdC) {
        return temperatureC >= thresholdC;
    }
}