package com.infinitymax.industry;

import com.infinitymax.api.ProjectInfinityMaxAPI;
import com.infinitymax.industry.registry.RegistryManager;

public class InfinityMaxIndustry {
    public static final String MOD_ID = "infinitymax_industry";

    public InfinityMaxIndustry() {
        // API初期化（CompatLoader など）
        ProjectInfinityMaxAPI.init();
        // in InfinityMaxIndustry constructor, after RegistryManager.init() etc.
        try {
            com.infinitymax.industry.compat.neoforge.NeoForgeCompatInitializer.init();
        } catch (Throwable t) {
            // ignore on Fabric-only environments
        }

        try {
            com.infinitymax.industry.compat.fabric.FabricCompatInitializer.init();
        } catch (Throwable t) {
            // ignore on Forge-only environments
        }
        // 本MODの一括登録
        RegistryManager.init();
        System.out.println("[InfinityMax-Industry] init OK");
    }
}
