package com.infinitymax.industry.block;

import com.infinitymax.industry.blockentity.MachineBlockEntity;
import com.infinitymax.industry.compat.OpenMenuHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * すべての機械の見た目・右クリック・BE生成を受け持つ汎用ブロック。
 * 種別は Kind で分岐し、BlockEntity に渡します。
 */
public class MachineBlock extends Block implements EntityBlock {

    public enum Kind {
        CRUSHER, INDUSTRIAL_FURNACE, ROLLING_MILL, ASSEMBLER,
        ADDITIVE_FABRICATOR, ASSEMBLY_LINE, PACKING_MACHINE,
        BLAST_FURNACE, ELECTRIC_FURNACE, SMELTER, COMPRESSOR,
        GAS_REFINER, EXCAVATOR, CONVEYOR_BELT, INDUSTRIAL_CRANE, ROBOTIC_ARM,
        CHEM_REACTOR, DISTILLATION_TOWER, ELECTROLYZER, GAS_SEPARATOR, POLYMERIZER,
        FERTILIZER_SYNTH, WASTEWATER_PROCESSOR, NUCLEAR_WASTE_PROCESSOR,
        COAL_GEN, OIL_GEN, GAS_TURBINE, NUCLEAR_REACTOR, FAST_BREEDER,
        SOLAR_PANEL, WIND_TURBINE, HYDRO_TURBINE, FUEL_CELL, BATTERY_BANK,
        TRANSFORMER, SUPERCONDUCTOR_STORAGE, POWER_TRANSMISSION_ANCHOR
    }

    public final Kind kind;

    public MachineBlock(Kind kind) {
        super(BlockBehaviour.Properties.of().strength(3.5F));
        this.kind = kind;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    // 右クリックでGUIを開く（BEがMenuProviderを実装）
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
            OpenMenuHelper.open(sp, provider, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // BE 生成
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state, kind);
    }

    // サーバーTickを回す
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, p, s, be) -> {
            if (be instanceof MachineBlockEntity m) m.serverTick();
        };
    }
}