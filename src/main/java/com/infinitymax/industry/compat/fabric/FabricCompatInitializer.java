package com.infinitymax.industry.compat.fabric;

import com.infinitymax.industry.tick.TickDispatcher;

/**
 * Fabric 用互換初期化器
 * - Fabric API が存在する場合は ServerTickEvents.END_SERVER_TICK に登録する
 *
 * 使い方: Mod コンストラクタで FabricCompatInitializer.init() を呼ぶ（try/catch して安全に）
 */
public final class FabricCompatInitializer {

    private FabricCompatInitializer() {}

    public static void init() {
        try {
            // dynamic load to avoid NoClassDefFoundError when fabric-api is not present
            Class<?> serverTickEvents = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents");
            java.lang.reflect.Method reg = serverTickEvents.getMethod("END_SERVER_TICK");
            Object obj = reg.invoke(null); // got the Event instance
            // Event has method register(ServerTickEvents.EndTick), we'll find it
            Class<?> endTickClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick");
            java.lang.reflect.Method registerMethod = obj.getClass().getMethod("register", endTickClass);
            // build lambda implementing EndTick: (server) -> TickDispatcher.tickAll()
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    FabricCompatInitializer.class.getClassLoader(),
                    new Class<?>[]{endTickClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("onEndTick")) {
                            TickDispatcher.tickAll();
                        }
                        return null;
                    });
            registerMethod.invoke(obj, listener);
            System.out.println("[InfinityMax-Compat][Fabric] initialized (using reflection)");
        } catch (ClassNotFoundException cnf) {
            // Fabric API not present — ignore silently
            System.out.println("[InfinityMax-Compat][Fabric] fabric API not found, skipping");
        } catch (Throwable t) {
            System.err.println("[InfinityMax-Compat][Fabric] init failed: " + t.getMessage());
            t.printStackTrace();
        }
    }
}