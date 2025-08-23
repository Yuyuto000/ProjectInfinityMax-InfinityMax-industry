package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.fluid.IPressureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 圧力逃がし弁：閾値を超えたら一定 mB を放出（周囲への flow 呼び出しを使う）
 */
public class PressureReliefValveBlockEntity extends BlockEntity implements IPressureNode {

    public static net.minecraft.world.level.block.entity.BlockEntityType<PressureReliefValveBlockEntity> TYPE;
    private double thresholdKPa = 250.0;
    private int releaseAmountmB = 200;
    private int capacitymB = 2000;
    private int amountmB = 0;
    private double pressureKPa = 101.3;
    // Medium fixed for simplicity
    private com.infinitymax.industry.fluid.Medium medium = com.infinitymax.industry.fluid.Medium.WATER;

    public PressureReliefValveBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override public void setRemoved() { super.setRemoved(); TickDispatcher.unregister(this); }

    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public com.infinitymax.industry.fluid.Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return 200; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(200, amountmB); }

    @Override
    public int flow(net.minecraft.world.level.Level level, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;
        if (requestedmB > 0) {
            int can = Math.min(requestedmB, capacitymB - amountmB);
            amountmB += can;
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return can;
        } else {
            int want = -requestedmB;
            int can = Math.min(want, Math.min(200, amountmB));
            amountmB -= can;
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return -can;
        }
    }

    @Override public void markDirtyGraph() {}

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (pressureKPa > thresholdKPa && amountmB > 0) {
            // release into neighbors: attempt to flow releaseAmountmB to each neighbor using FluidNetwork.tick consumer model
            // For simplicity, we try to directly call neighbor flow()
            for (var npos : com.infinitymax.industry.fluid.FluidNetwork.neighbors(worldPosition)) {
                var be = level.getBlockEntity(npos);
                if (be instanceof IPressureNode) {
                    int pushed = ((IPressureNode)be).flow(level, npos, Math.min(releaseAmountmB, amountmB));
                    amountmB -= Math.abs(pushed);
                    if (amountmB <= 0) break;
                }
            }
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
        }
    }
}