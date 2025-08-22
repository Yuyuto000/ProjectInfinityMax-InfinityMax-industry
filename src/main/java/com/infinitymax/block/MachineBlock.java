package com.infinitymax.industry.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import com.infinitymax.industry.blockentity.MachineBlockEntity;

public class MachineBlock extends Block implements EntityBlock {

    public enum Kind {
        // 軽工業
        CRUSHER, INDUSTRIAL_FURNACE, ROLLING_MILL, ASSEMBLER, ADDITIVE_FABRICATOR,
        ASSEMBLY_LINE, PACKING_MACHINE,
        // 重工業
        BLAST_FURNACE, ELECTRIC_FURNACE, SMELTER, COMPRESSOR, GAS_REFINER,
        EXCAVATOR, CONVEYOR_BELT, INDUSTRIAL_CRANE, ROBOTIC_ARM,
        // 化学
        CHEM_REACTOR, DISTILLATION_TOWER, ELECTROLYZER, GAS_SEPARATOR,
        POLYMERIZER, FERTILIZER_SYNTH, WASTEWATER_PROCESSOR, NUCLEAR_WASTE_PROCESSOR,
        // エネルギー
        COAL_GEN, OIL_GEN, GAS_TURBINE, NUCLEAR_REACTOR, FAST_BREEDER,
        SOLAR_PANEL, WIND_TURBINE, HYDRO_TURBINE, FUEL_CELL, BATTERY_BANK,
        TRANSFORMER, SUPERCONDUCTOR_STORAGE, POWER_TRANSMISSION_ANCHOR
    }

    public final Kind kind;

    public MachineBlock(Kind kind) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 12.0f).requiresCorrectToolForDrops());
        this.kind = kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state, this.kind);
    }
}
