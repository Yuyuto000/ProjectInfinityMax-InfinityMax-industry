package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.energy.IElectricNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 簡易石炭発電：燃料(石炭/木炭)を消費してJouleを生産する BE
 * - Inventory: slot0 = fuel
 * - 出力は ElectricNetwork を通じて配電される（pushPullCurrent が呼ばれる想定）
 */
public class CoalGeneratorBlockEntity extends BlockEntity implements IElectricNode {

    public static BlockEntityType<CoalGeneratorBlockEntity> TYPE;

    private int fuelTime = 0;
    private double storedJ = 0.0;
    private double maxJ = 500000.0; // 500kJ buffer

    private double outputVoltage = 240.0;
    private double maxOutputA = 200.0;
    private double internalR = 0.1;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
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

        // 燃料消費
        if (fuelTime <= 0) {
            // try to consume coal from slot0 if present
            if (level.getBlockEntity(worldPosition) instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                // we keep it simple: read block inventory via world block entity var isn't standardized here
                // In real mod, create dedicated inventory slot in BE class; here, demo: pretend infinite fuel
                fuelTime = 20 * 8; // 8 sec burn per 'unit' for demo
            }
        } else {
            fuelTime--;
            // produce Joules per tick
            double produceJ = 240.0 * 10.0 / 20.0; // 240V * 10A => 2400W => 120J/tick
            storedJ = Math.min(maxJ, storedJ + produceJ);
        }

        // try to supply network by being IElectricNode: ElectricNetwork.tick will call pushPullCurrent on us when distributing
    }

    // IElectricNode
    @Override public double getVoltageV() { return outputVoltage; }
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return 0.0; } // not consumer
    @Override public double getMaxOutputA() { return maxOutputA; }

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level level, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // If requestedCurrentA is negative => network asks us to supply (we interpret negative as supply request in ElectricNetwork impl)
        double want = Math.abs(requestedCurrentA);
        double can = Math.min(want, maxOutputA);
        // reduce storedJ by provided energy: J = V * A / 20
        double neededJ = outputVoltage * can / 20.0;
        if (storedJ >= neededJ) {
            storedJ -= neededJ;
            return -can; // negative means we supplied A (per ElectricNetwork convention earlier)
        } else {
            double possibleA = (storedJ * 20.0) / outputVoltage;
            double supplied = Math.min(possibleA, can);
            double consumedJ = outputVoltage * supplied / 20.0;
            storedJ -= consumedJ;
            return -supplied;
        }
    }

    @Override public void markDirtyGraph() {}

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedJ = tag.getDouble("storedJ");
        fuelTime = tag.getInt("fuelTime");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("storedJ", storedJ);
        tag.putInt("fuelTime", fuelTime);
    }
}