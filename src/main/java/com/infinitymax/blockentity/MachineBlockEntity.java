package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.energy.IElectricNode;
import com.infinitymax.industry.energy.IElectricPort;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

public class MachineBlockEntity extends BlockEntity implements IElectricNode, IElectricPort {

    public static BlockEntityType<MachineBlockEntity> TYPE; // RegistryManager が注入

    public final MachineBlock.Kind kind;
    private int progress;

    // Energy internal (Joules)
    private double storedJ = 0.0;
    private double capacityJ = 200000.0; // 200kJ default capacity

    // electrical port / node params
    private double ratedV = 240.0;
    private double ratedA = 40.0;
    private double internalR = 0.5;
    private double terminalV = 0.0;

    public MachineBlockEntity(BlockPos pos, BlockState state, MachineBlock.Kind kind) {
        super(TYPE, pos, state);
        this.kind = kind;
        this.progress = 0;
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
    }

    // ---------- serverTick (public, called by TickDispatcher) ----------
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        // simple passive drain to simulate work
        // If stored energy is available, consume some per tick and advance progress
        double requiredJ = requiredWorkJPerTick();
        if (storedJ >= requiredJ) {
            storedJ -= requiredJ;
            progress++;
            // do finishing action per kind
            if (progress >= 200) {
                progress = 0;
                // TODO: do output production (craft result) — implement later
            }
        }
    }

    // ===== IElectricNode =====
    @Override public double getVoltageV() { return terminalV; }
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return ratedA; }
    @Override public double getMaxOutputA() { return 0.0; } // not a source

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // Accept positive current (supply to machine)
        if (requestedCurrentA <= 0.0) return 0.0;
        double allowA = Math.min(requestedCurrentA, ratedA);
        // calc delivered joules for one tick: V * A * (1/20)
        double deliveredJ = requestedVoltageV * allowA / 20.0;
        storedJ = Math.min(capacityJ, storedJ + deliveredJ);
        terminalV += (requestedVoltageV - terminalV) * 0.3;
        return allowA; // actual current accepted (A)
    }

    @Override
    public void markDirtyGraph() {
        // no-op for now
    }

    // ===== IElectricPort =====
    @Override public double requiredWorkJPerTick() { return 240.0 / 20.0; } // example: 240W -> 12J/tick
    @Override
    public double acceptPowerVA(double voltageV, double maxCurrentA) {
        double acceptedA = pushPullCurrent(level, worldPosition, voltageV, maxCurrentA);
        return acceptedA;
    }
    @Override public double ratedVoltageV() { return ratedV; }
    @Override public double ratedCurrentA() { return ratedA; }

    // Convenience for API adapter
    public double receiveJoules(double j, boolean simulate) {
        if (!simulate) storedJ = Math.min(capacityJ, storedJ + j);
        return j;
    }
    public double extractJoules(double j, boolean simulate) {
        double can = Math.min(storedJ, j);
        if (!simulate) storedJ -= can;
        return can;
    }
    public double getStoredJoules() { return storedJ; }
    public double getCapacityJoules() { return capacityJ; }
}