package com.infinitymax.industry.energy;

import com.infinitymax.industry.network.NetworkManager;
import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 電力ケーブル（導体ノード）
 *
 * - ネットワーク再構築要求は NetworkManager.markElectricDirty(level) へ
 * - serverTick() 内で NetworkManager.serverTick(level) を呼ぶ
 */
public class ElectricCableBlockEntity extends BlockEntity implements IElectricNode {

    public static BlockEntityType<ElectricCableBlockEntity> TYPE;

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
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markElectricDirty(level);
        }
    }

    // ===== IElectricNode =====
    @Override public double getVoltageV() { return voltageV; }
    @Override public double getInternalResistanceOhm() { return internalResistanceOhm; }
    @Override public double getMaxIntakeA() { return maxCurrentA; }
    @Override public double getMaxOutputA() { return maxCurrentA; }

    @Override
    public double pushPullCurrent(Level lvl, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        double allowed = Math.min(Math.abs(requestedCurrentA), maxCurrentA);
        double sign = Math.signum(requestedCurrentA);
        voltageV += (requestedVoltageV - voltageV) * 0.2;
        return sign * allowed;
    }

    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markElectricDirty(level);
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        NetworkManager.get().serverTick(level);
        // 少しずつ減衰させる（表示用）
        voltageV *= 0.999;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markElectricDirty(level);
        }
    }

    public void onNeighborsChanged() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markElectricDirty(level);
        }
    }

    // 永続化（必要なら拡張）
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("voltageV")) voltageV = tag.getDouble("voltageV");
        if (tag.contains("internalR")) internalResistanceOhm = tag.getDouble("internalR");
        if (tag.contains("maxA")) maxCurrentA = tag.getDouble("maxA");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("voltageV", voltageV);
        tag.putDouble("internalR", internalResistanceOhm);
        tag.putDouble("maxA", maxCurrentA);
    }
}