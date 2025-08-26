package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.fluid.IPressureNode;
import com.infinitymax.industry.fluid.Medium;
import com.infinitymax.industry.network.NetworkManager; // ★ 追加：ネットワーク管理器
import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * タンク (流体貯蔵のノード)
 *
 * 変更点：
 * - 以前は各BEが毎tick FluidNetwork.tick(...) を呼んでいたが、負荷が高い。
 * - 本改修では、ネットワークの tick は NetworkManager が「ネットワーク単位」で行う。
 * - 本BEは onLoad / setRemoved / 近傍変化 時に NetworkManager へ「再構築要求」を出すだけにする。
 *
 * 使い方の注意：
 * - 対応する Block クラス側（例：TankBlock）で neighborChanged / onPlace / onRemove などのタイミングで
 *   本BEの onNeighborsChanged() を呼び出してください（サンプルは下方コメント参照）。
 */
public class FluidTankBlockEntity extends BlockEntity implements IPressureNode {

    public static BlockEntityType<FluidTankBlockEntity> TYPE;

    // ====== タンク基本パラメータ ======
    private int capacitymB = 16000;     // 総容量
    private int amountmB = 0;           // 現在量
    private Medium medium = Medium.WATER; // 流体種類（簡易 enum 想定）
    private double pressureKPa = 101.3; // 圧力の簡易モデル
    private int maxIn = 1000;           // 1tick 受入上限
    private int maxOut = 1000;          // 1tick 供出上限

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        // ★ TickDispatcher を使っている場合でも、本BEの serverTick は「ネットワークtickを呼ばない」軽量処理のみ。
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
        // ★ ノード消滅時：ネットワーク再構築を要求
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
        }
    }

    // ====== IPressureNode 実装 ======
    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return maxIn; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(maxOut, amountmB); }

    /**
     * フローの要求に応じて入出力する。
     * requestedmB > 0 なら「受入」、requestedmB < 0 なら「供出」。
     * 返り値は実際に動いた mB（受入は+ / 供出は-）。
     */
    @Override
    public int flow(Level lvl, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;

        if (requestedmB > 0) {
            // 受入
            int can = Math.min(requestedmB, Math.min(maxIn, capacitymB - amountmB));
            if (can > 0) {
                // ★ first-fill 時に medium をセットするようにする場合はここで調整しても良い
                amountmB += can;
                // 圧力の簡易更新（充填率に比例）
                pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            }
            return can;
        } else {
            // 供出
            int want = -requestedmB;
            int can = Math.min(want, Math.min(maxOut, amountmB));
            if (can > 0) {
                amountmB -= can;
                pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            }
            return -can;
        }
    }

    /**
     * グラフ（ネットワーク）構造が変わった可能性を上位へ通知。
     * 今回は即時で NetworkManager に再構築を依頼（スロットルや遅延を入れたい場合はここで調整）。
     */
    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
        }
    }

    /**
     * BEのサーバtick。
     * 以前のようにネットワーク伝播計算は行わない（NetworkManager がネットワーク単位で実行）。
     * ここではタンク自身の自然減衰や内部温度/圧力の緩和など、超軽量なローカル更新のみを行う想定。
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // ここでは何もしない（必要なら微小な圧力緩和などを書いてOK）
        // 例: pressureKPa = Math.max(101.3, pressureKPa - 0.02);
    }

    // ====== ライフサイクル ======

    /** チャンク読み込み完了時。最初にネットワーク再構築を要求しておく。 */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
        }
    }

    /** Block から呼ばれる近傍変化フック。Block 側で neighborChanged -> ここを呼び出すこと。 */
    public void onNeighborsChanged() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildFluidNetworks(level);
        }
    }

    // ====== 永続化 ======
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        amountmB = tag.getInt("amountmB");
        capacitymB = tag.getInt("capacitymB");
        pressureKPa = tag.getDouble("pressureKPa");
        // medium は簡易 enum を前提。必要に応じて name を保存/読込してください。
        if (tag.contains("medium")) {
            try {
                medium = Medium.valueOf(tag.getString("medium"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("amountmB", amountmB);
        tag.putInt("capacitymB", capacitymB);
        tag.putDouble("pressureKPa", pressureKPa);
        tag.putString("medium", medium.name());
    }
}

/* ──────────────────────────────────────────────────────────────
 * ★ Block 側での呼び出しサンプル（TankBlock など）
 *
 * @Override
 * public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
 *     super.neighborChanged(state, level, pos, block, fromPos, isMoving);
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof FluidTankBlockEntity tank) {
 *         tank.onNeighborsChanged(); // 近傍が変わったのでネットワーク再構築を要求
 *     }
 * }
 *
 * @Override
 * public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
 *     super.onPlace(state, level, pos, oldState, isMoving);
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof FluidTankBlockEntity tank) {
 *         tank.onNeighborsChanged(); // 設置時も要求
 *     }
 * }
 *
 * @Override
 * public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
 *     super.onRemove(state, level, pos, newState, isMoving);
 *     // BE は setRemoved() 側で再構築要求を出すので基本不要だが、保険で呼んでもよい
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof FluidTankBlockEntity tank) {
 *         tank.onNeighborsChanged();
 *     }
 * }
 * ────────────────────────────────────────────────────────────── */
