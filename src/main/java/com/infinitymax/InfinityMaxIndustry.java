package com.infinitymax.industry;

import com.infinitymax.api.ProjectInfinityMaxAPI;
import com.infinitymax.industry.registry.RegistryManager;

public class InfinityMaxIndustry {
    public static final String MOD_ID = "infinitymax_industry";

    public InfinityMaxIndustry() {
        // API初期化（CompatLoader など）
        ProjectInfinityMaxAPI.init();
        // 本MODの一括登録
        RegistryManager.init();
        System.out.println("[InfinityMax-Industry] init OK");
    }
}
