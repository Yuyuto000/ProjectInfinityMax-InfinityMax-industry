package com.infinitymax.industry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricCableBlockEntity extends BlockEntity implements IElectricNode {

    // 素材に応じた値（例：銅ケーブル）
    private double internalResistanceOhm = 0.02;  // 1ノード等価抵抗（距離近似）
    private double maxCurrentA = 200.0;           // 許容電流
    private double voltageV = 0.0;                // 見かけ電圧（近似）
    private boolean dirtyGraph = true;

    public static net.minecraft.world.level.block.entity.BlockEntityType<ElectricCableBlockEntity> TYPE;

    public ElectricCableBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // ===== IElectricNode =====
    @Override public double getVoltageV() { return voltageV; }
    @Override public double getInternalResistanceOhm() { return internalResistanceOhm; }
    @Override public double getMaxIntakeA() { return maxCurrentA; }
    @Override public double getMaxOutputA() { return maxCurrentA; }

    @Override
    public double pushPullCurrent(Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // 近似：見かけ電圧を目標に緩和、電流はクリップ
        double allowed = Math.min(Math.abs(requestedCurrentA), maxCurrentA);
        double sign = Math.signum(requestedCurrentA);
        voltageV += (requestedVoltageV - voltageV) * 0.2; // 緩和
        return sign * allowed;
    }

    @Override
    public void markDirtyGraph() { dirtyGraph = true; }

    // ===== Tick（ローダ別のTickerから呼ぶ想定） =====
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // 同一ネットを軽く回す（最大512ノード等）
        ElectricNetwork.tick(level, worldPosition, 1.0, 512);
        dirtyGraph = false;
    }
}
