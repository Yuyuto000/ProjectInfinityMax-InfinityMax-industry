package com.infinitymax.industry.fluid;

import com.infinitymax.industry.network.SmartNetworkManager;
import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 配管 (Pipe) の BlockEntity 実装（IPressureNode）
 *
 * - SmartNetworkManager を使って「起点座標つきデバウンス要求」を出す仕様
 * - serverTick() は軽量処理 + SmartNetworkManager.serverTick(level) を呼ぶ
 */
public class FluidPipeBlockEntity extends BlockEntity implements IPressureNode {

    public static BlockEntityType<FluidPipeBlockEntity> TYPE;

    // 基本パラメータ
    private int capacitymB = 2000;
    private int amountmB = 0;
    private double pressureKPa = 101.3;
    private Medium medium = Medium.WATER;
    private int maxInPerTick  = 250;
    private int maxOutPerTick = 250;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
        // 削除されたので「この位置を起点に」デバウンス再構築を要求
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    // ===== IPressureNode 実装 =====
    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return maxInPerTick; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(maxOutPerTick, amountmB); }

    /**
     * フロー処理：requestedmB > 0 => 受入 (in)、 requestedmB < 0 => 供出 (out)
     * 戻り値は実際に処理された量（受入なら +、供出なら -）。
     */
    @Override
    public int flow(Level level, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;

        if (requestedmB > 0) {
            int can = Math.min(requestedmB, Math.min(maxInPerTick, capacitymB - amountmB));
            if (can > 0) {
                amountmB += can;
                updatePressure();
            }
            return can;
        } else {
            int want = -requestedmB;
            int can = Math.min(want, Math.min(maxOutPerTick, amountmB));
            if (can > 0) {
                amountmB -= can;
                updatePressure();
            }
            return -can;
        }
    }

    private void updatePressure() {
        pressureKPa = 101.3 + (amountmB / (double)Math.max(1, capacitymB)) * 400.0;
    }

    /**
     * グラフ構造が変わった可能性を通知（Block の neighborChanged 等から呼ぶ）。
     * 起点座標付きで SmartNetworkManager にデバウンス要求を出す。
     */
    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    /**
     * サーバー側ティック処理。
     * - SmartNetworkManager.serverTick(level) を呼んでデバウンス再構築やネットワーク tick を回す
     * - 個体の軽量な緩和処理のみ行う
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        SmartNetworkManager.get().serverTick(level);
        // ローカル緩和（例）
        pressureKPa = Math.max(101.3, pressureKPa - 0.001);
    }

    // ===== 永続化 =====
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        amountmB = tag.getInt("amountmB");
        capacitymB = tag.contains("capacitymB") ? tag.getInt("capacitymB") : capacitymB;
        pressureKPa = tag.contains("pressureKPa") ? tag.getDouble("pressureKPa") : pressureKPa;
        if (tag.contains("medium")) {
            try { medium = Medium.valueOf(tag.getString("medium")); } catch (IllegalArgumentException ignored) {}
        }
        maxInPerTick = tag.contains("maxIn") ? tag.getInt("maxIn") : maxInPerTick;
        maxOutPerTick = tag.contains("maxOut") ? tag.getInt("maxOut") : maxOutPerTick;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("amountmB", amountmB);
        tag.putInt("capacitymB", capacitymB);
        tag.putDouble("pressureKPa", pressureKPa);
        tag.putString("medium", medium.name());
        tag.putInt("maxIn", maxInPerTick);
        tag.putInt("maxOut", maxOutPerTick);
    }

    // ライフサイクル補助
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // チャンク読み込み時は「この位置」を起点にデバウンス再構築を要求
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    /** Block 側の neighborChanged で呼ぶユーティリティ。 */
    public void onNeighborsChanged() {
        markDirtyGraph();
    }
}