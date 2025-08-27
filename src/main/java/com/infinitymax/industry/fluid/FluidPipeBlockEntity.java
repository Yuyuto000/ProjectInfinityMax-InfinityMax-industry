package com.infinitymax.industry.fluid;

import com.infinitymax.industry.network.NetworkManager;
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
 * 変更点／仕様：
 * - 以前は各 Pipe が毎tickで FluidNetwork.tick を呼んでいた → 負荷が大きい。
 * - 今回はネットワークの再構築要求のみ行い、NetworkManager がネットワーク単位で tick を実行。
 * - Block 側で neighborChanged / onPlace / onRemove 等のタイミングで onNeighborsChanged() を呼んでください。
 *
 * 流量の符号：
 * - flow(level, pos, requestedmB) の requestedmB は、
 *     >  正 (例 +500) で「受入 (in)」を要求
 *     >  負 (例 -200) で「供出 (out)」を要求
 * - 戻り値は実際に処理した mB（受入は + 、供出は -）。
 */
public class FluidPipeBlockEntity extends BlockEntity implements IPressureNode {

    public static BlockEntityType<FluidPipeBlockEntity> TYPE;

    // 基本パラメータ（パイプはタンクより小容量）
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
        // 削除時はネットワーク構造が変わったので再構築を要求
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
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
     * 戻り値は実際に処理された量（受入なら +、供出なら -）
     */
    @Override
    public int flow(Level level, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;

        if (requestedmB > 0) {
            // 受入
            int can = Math.min(requestedmB, Math.min(maxInPerTick, capacitymB - amountmB));
            if (can > 0) {
                amountmB += can;
                updatePressure();
            }
            return can;
        } else {
            // 供出
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
        // 簡易モデル：満タン時に高圧化（定数は調整可）
        pressureKPa = 101.3 + (amountmB / (double)Math.max(1, capacitymB)) * 400.0;
    }

    /**
     * グラフ構造が変わった可能性を通知する（Block 側の neighborChanged から呼ばれる）。
     * NetworkManager に再構築要求を投げる。
     */
    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markDirty(level);
        }
    }

    /**
     * サーバー側ティック処理。ネットワーク全体の流体移動は NetworkManager が行うため、
     * ここではローカルの緩和処理やアニメーション用値更新などの軽量処理のみ行う。
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        NetworkManager.get().serverTick(level); // デバウンス付き再構築
        // 例: 微小な自然漏れや圧力緩和を入れたいならここに
        pressureKPa = Math.max(101.3, pressureKPa - 0.001);
    }

    // ====== 永続化 ======
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        amountmB = tag.getInt("amountmB");
        capacitymB = tag.contains("capacitymB") ? tag.getInt("capacitymB") : capacitymB;
        pressureKPa = tag.contains("pressureKPa") ? tag.getDouble("pressureKPa") : pressureKPa;
        if (tag.contains("medium")) {
            try {
                medium = Medium.valueOf(tag.getString("medium"));
            } catch (IllegalArgumentException ignored) {}
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

    // ===== ライフサイクル補助 =====
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
        }
    }

    /**
     * Block 側の neighborChanged で呼ぶユーティリティ。
     * 例: TankBlock / PipeBlock の neighborChanged 内で BE を取得して呼び出す。
     */
    public void onNeighborsChanged() {
        markDirtyGraph();
    }
}