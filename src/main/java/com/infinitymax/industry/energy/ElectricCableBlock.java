package com.infinitymax.industry.energy;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 電力ケーブルのブロック実装
 *
 * - 近傍変化 (neighborChanged) や設置/破壊で BE の onNeighborsChanged() を呼び、
 *   ネットワーク再構築要求を投げる仕様にしています。
 * - serverTick を回すための getTicker を実装（サーバのみ）。
 */
public class ElectricCableBlock extends Block implements EntityBlock {

    public ElectricCableBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricCableBlockEntity(pos, state);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    // 右クリックで GUI を開く可能性があるならここで処理する（ケーブルは通常不要）
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // ケーブルは GUI が無い想定 → デフォルト処理
        return InteractionResult.PASS;
    }

    // チャンク読み込み完了時。BlockEntity の onLoad で再構築要求を行っているのでここは不要だが保険として呼ぶ。
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricCableBlockEntity cable) cable.onNeighborsChanged();
        }
    }

    // ブロックが破壊・置換された場合に呼ばれる
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide) {
            // setRemoved() 側でも再構築要求を出すようにしているためここは保険
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricCableBlockEntity cable) cable.onNeighborsChanged();
        }
    }

    // 近傍のブロックが変化したときに呼ばれる。ここで BE に知らせる。
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricCableBlockEntity cable) cable.onNeighborsChanged();
        }
    }

    // サーバ側で BE の serverTick を呼び出すためのティッカーを返す
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, p, s, be) -> {
            if (be instanceof ElectricCableBlockEntity cable) cable.serverTick();
        };
    }
}