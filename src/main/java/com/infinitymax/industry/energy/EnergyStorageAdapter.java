package com.infinitymax.industry.energy;

import com.infinitymax.api.energy.IEnergyStorage;

/**
 * IEnergyStorage (API) -> MachineBlockEntity (Jouleベース) のアダプタ
 *
 * 単純変換ルール:
 *  - API の int 単位は "Joule" とみなす（1 int = 1 J）。このルールはプロジェクト内で統一してください。
 *  - receiveEnergy/extractEnergy は MachineBlockEntity.receiveJoules / extractJoules を呼ぶだけ。
 */
public class EnergyStorageAdapter implements IEnergyStorage {

    private final MachineBlockEntity machine;

    public EnergyStorageAdapter(MachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        double accepted = machine.receiveJoules((double) maxReceive, simulate);
        return (int) Math.round(accepted);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        double extracted = machine.extractJoules((double) maxExtract, simulate);
        return (int) Math.round(extracted);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.round(machine.getStoredJoules());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.round(machine.getCapacityJoules());
    }

    @Override
    public boolean canExtract() {
        return false; // machines are consumers by default
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}