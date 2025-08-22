package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.energy.IElectricNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 2ポート（簡易）変圧器
 * - HV 側入力を受け、LV 側へ電流を増やして電圧を下げる（電力保存近似）
 * - ここではブロックの向き/ポートは未実装。動作は ElectricNetwork の分配時に端子情報を使って近似。
 */
public class TransformerBlockEntity extends BlockEntity implements IElectricNode {

    public static BlockEntityType<TransformerBlockEntity> TYPE;

    // internal model: two terminals (A and B) approximated by voltages and max currents
    private double hvVoltage = 1000.0;
    private double lvVoltage = 240.0;
    private double hvMaxA = 50.0;
    private double lvMaxA = 200.0;
    private double internalR = 0.05;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // nothing active by itself; ElectricNetwork will call pushPullCurrent on it.
    }

    // IElectricNode: treat transformer as conductor with special internal behavior
    @Override public double getVoltageV() { return (hvVoltage + lvVoltage) / 2.0; } // approximate
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return Math.max(hvMaxA, lvMaxA); }
    @Override public double getMaxOutputA() { return Math.max(hvMaxA, lvMaxA); }

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // Simplified: if request is positive -> absorb, negative -> supply
        // We won't implement port differentiation here; ElectricNetwork should ensure proper distribution
        double allowed = Math.min(Math.abs(requestedCurrentA), Math.max(hvMaxA, lvMaxA));
        double sign = Math.signum(requestedCurrentA);
        // No internal storage in this simplified impl; just pass-through scaled by ratio
        // If supplying (negative requestedCurrentA), scale down voltage and increase current capacity
        return sign * allowed;
    }

    @Override public void markDirtyGraph() {}
}