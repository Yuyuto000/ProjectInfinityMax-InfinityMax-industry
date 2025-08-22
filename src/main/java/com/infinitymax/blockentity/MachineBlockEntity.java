package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.infinitymax.industry.energy.IElectricPort;
import com.infinitymax.industry.energy.IElectricNode;

public class MachineBlockEntity extends BlockEntity implements IElectricNode, IElectricPort {
    // 電力ポートの定格（例）
    private double ratedV = 240.0;
    private double ratedA = 40.0;
    private double internalR = 0.5; // 機械の等価内部抵抗
    private double terminalV = 0.0;

    // === IElectricNode ===
    @Override public double getVoltageV() { return terminalV; }
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return ratedA; }
    @Override public double getMaxOutputA() { return 0.0; } // 機械は電源ではない

    @Override
    public double pushPullCurrent(Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // 機械は受電のみ（＋方向）、負は拒否
        if (requestedCurrentA < 0) return 0.0;
        double allow = Math.min(requestedCurrentA, ratedA);
        terminalV += (requestedVoltageV - terminalV) * 0.3;
        // 実作業：受け取った電力で progress を進める
        double watts = terminalV * allow; // 近似
        progress += (int)Math.max(0, Math.floor(watts / 100.0)); // 例：100Wで1progress/tick
        if (progress >= 200) progress = 0;
        return allow;
    }

    @Override public void markDirtyGraph() {}

    // === IElectricPort ===
    @Override public double requiredWorkJPerTick() { return 1200.0; } // 例：60W 相当
    @Override public double acceptPowerVA(double voltageV, double maxCurrentA) {
        return pushPullCurrent(level, worldPosition, voltageV, maxCurrentA);
    }
    @Override public double ratedVoltageV() { return ratedV; }
    @Override public double ratedCurrentA() { return ratedA; }
}
