package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.energy.IElectricNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ブレーカー（定格を越える電流でトリップ）
 */
public class CircuitBreakerBlockEntity extends BlockEntity implements IElectricNode {

    public static net.minecraft.world.level.block.entity.BlockEntityType<CircuitBreakerBlockEntity> TYPE;

    private double ratedA = 200.0;
    private boolean tripped = false;

    public CircuitBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override public void setRemoved() { super.setRemoved(); TickDispatcher.unregister(this); }

    @Override public void serverTick() {
        if (level == null || level.isClientSide) return;
        // Nothing active; ElectricNetwork must call pushPullCurrent and we can trip based on requestedCurrent
    }

    @Override public double getVoltageV() { return 0.0; }
    @Override public double getInternalResistanceOhm() { return tripped ? 1e9 : 0.001; } // open=very high R
    @Override public double getMaxIntakeA() { return tripped ? 0.0 : ratedA; }
    @Override public double getMaxOutputA() { return tripped ? 0.0 : ratedA; }

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        double reqA = Math.abs(requestedCurrentA);
        if (reqA > ratedA && !tripped) {
            tripped = true;
            // optionally: spawn particles / play sound / log
            System.out.println("[Breaker] tripped at " + worldPosition + " requestedA=" + reqA);
            return 0.0;
        }
        // pass-through
        return requestedCurrentA;
    }

    @Override public void markDirtyGraph() {}

    @Override
    public void load(CompoundTag tag) { super.load(tag); tripped = tag.getBoolean("tripped"); }
    @Override
    public void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putBoolean("tripped", tripped); }

    public void resetBreaker() { tripped = false; markDirty(); }
}