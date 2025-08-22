package com.infinitymax.industry.energy;

import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricCableBlockEntity extends BlockEntity implements IElectricNode {

    public static net.minecraft.world.level.block.entity.BlockEntityType<ElectricCableBlockEntity> TYPE;

    private double internalResistanceOhm = 0.02;
    private double maxCurrentA = 200.0;
    private double voltageV = 0.0;

    public ElectricCableBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
    }

    @Override public double getVoltageV() { return voltageV; }
    @Override public double getInternalResistanceOhm() { return internalResistanceOhm; }
    @Override public double getMaxIntakeA() { return maxCurrentA; }
    @Override public double getMaxOutputA() { return maxCurrentA; }

    @Override
    public double pushPullCurrent(Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // 近似：緩和してクリップ
        double allowed = Math.min(Math.abs(requestedCurrentA), maxCurrentA);
        double sign = Math.signum(requestedCurrentA);
        voltageV += (requestedVoltageV - voltageV) * 0.2;
        return sign * allowed;
    }

    @Override public void markDirtyGraph() { /* no-op */ }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // approximate network balance using ElectricNetwork.tick in industry.energy
        ElectricNetwork.tick(level, worldPosition, 1.0, 512);
    }
}