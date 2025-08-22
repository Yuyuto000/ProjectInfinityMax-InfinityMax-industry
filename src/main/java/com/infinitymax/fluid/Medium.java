package com.infinitymax.industry.fluid;

/** 流体・ガスの物性（シンプル版） */
public class Medium {
    public enum Phase { LIQUID, GAS }

    public final String id;
    public final Phase phase;
    /** 密度[kg/m3]（液体は高め、ガスは低め） */
    public final double density;
    /** 動粘度[Pa·s]（大きいほど流れにくい） */
    public final double viscosity;
    /** 圧縮性係数Z（1=理想気体、液体は≪1とみなす） */
    public final double compressibilityZ;

    public Medium(String id, Phase phase, double density, double viscosity, double compressibilityZ) {
        this.id = id; this.phase = phase; this.density = density; this.viscosity = viscosity; this.compressibilityZ = compressibilityZ;
    }

    // 代表的媒体（必要に応じ拡張）
    public static final Medium WATER   = new Medium("water", Phase.LIQUID, 1000.0, 0.001, 0.02);
    public static final Medium OIL     = new Medium("oil", Phase.LIQUID,   850.0, 0.050, 0.02);
    public static final Medium STEAM   = new Medium("steam", Phase.GAS,      0.6, 0.00002, 1.0);
    public static final Medium OXYGEN  = new Medium("oxygen", Phase.GAS,     1.4, 0.00002, 1.0);
    public static final Medium HYDROGEN= new Medium("hydrogen", Phase.GAS,   0.09,0.000009,1.0);
    public static final Medium NITROGEN= new Medium("nitrogen", Phase.GAS,   1.2, 0.000018,1.0);
}
