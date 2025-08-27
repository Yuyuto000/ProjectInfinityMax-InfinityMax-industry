package com.infinitymax.industry.energy;

import com.infinitymax.industry.network.NetworkManager; // ★ 追加：ネットワーク管理器
import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 電力ケーブル（導体ノード）
 *
 * 変更点：
 * - 以前は各BEが毎tick ElectricNetwork.tick(...) を呼んでいたが、負荷が高い。
 * - 本改修では、ネットワークの tick は NetworkManager が「ネットワーク単位」で行う。
 * - 本BEは onLoad / setRemoved / 近傍変化 時に NetworkManager へ「再構築要求」を出すだけにする。
 *
 * 備考：
 * - pushPullCurrent(...) は従来通り「導体としての緩和＋最大電流制限」のみ簡易対応。
 * - 実際の供給/需要の配分は ElectricNetwork 側で行う。
 */
public class ElectricCableBlockEntity extends BlockEntity implements IElectricNode {

    public static net.minecraft.world.level.block.entity.BlockEntityType<ElectricCableBlockEntity> TYPE;

    // ケーブル特性（簡易モデル）
    private double internalResistanceOhm = 0.02; // 内部抵抗（損失に影響）
    private double maxCurrentA = 200.0;          // 許容電流
    private double voltageV = 0.0;               // 表示用の見かけ電圧

    public ElectricCableBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        // ★ TickDispatcher に登録していても、本BE serverTick は「ネットワークtickを呼ばない」軽い処理のみ。
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
        // ★ ノード消滅時：ネットワーク再構築を要求
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildElectricNetworks(level);
        }
    }

    // ====== IElectricNode 実装 ======
    @Override public double getVoltageV() { return voltageV; }
    @Override public double getInternalResistanceOhm() { return internalResistanceOhm; }
    @Override public double getMaxIntakeA() { return maxCurrentA; } // 負荷としても導体としても「受け取り可能」
    @Override public double getMaxOutputA() { return maxCurrentA; } // 供給としても「出力可能」

    /**
     * ネットワークからの電流プッシュ/プル要求に対する応答。
     * requestedCurrentA の符号で方向を表現（+ は受電、- は送電という扱い）。
     * ここでは「電流のクリップ」と「電圧の緩和」のみを行い、エネルギー計算はネットワーク側に任せる。
     */
    @Override
    public double pushPullCurrent(Level lvl, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        double allowed = Math.min(Math.abs(requestedCurrentA), maxCurrentA);
        double sign = Math.signum(requestedCurrentA);
        // 見かけ電圧をソース電圧へ緩和（視覚・表示用途）
        voltageV += (requestedVoltageV - voltageV) * 0.2;
        return sign * allowed;
    }

    /** グラフ（ネットワーク）構造の更新要求。 */
    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().markDirty(level);
        }
    }

    /** BEのサーバtick（ネットワークtickは呼ばない）。 */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // ここではケーブル自体のローカルな緩和・発熱カウンタ等を入れるなら記述
        NetworkManager.get().serverTick(level); // デバウンス付き再構築
        voltageV *= 0.999; // わずかな減衰
    }

    // ====== ライフサイクル ======

    /** チャンク読み込み完了時：ネットワーク再構築を要求。 */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildElectricNetworks(level);
        }
    }

    /** Block 側から近傍変化で呼ばせるフック。 */
    public void onNeighborsChanged() {
        if (level != null && !level.isClientSide) {
            NetworkManager.get().rebuildElectricNetworks(level);
        }
    }
}

/* ──────────────────────────────────────────────────────────────
 * ★ Block 側での呼び出しサンプル（ElectricCableBlock など）
 *
 * @Override
 * public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
 *     super.neighborChanged(state, level, pos, block, fromPos, isMoving);
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof ElectricCableBlockEntity cable) {
 *         cable.onNeighborsChanged(); // 近傍が変わったのでネットワーク再構築を要求
 *     }
 * }
 *
 * @Override
 * public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
 *     super.onPlace(state, level, pos, oldState, isMoving);
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof ElectricCableBlockEntity cable) {
 *         cable.onNeighborsChanged(); // 設置時も要求
 *     }
 * }
 *
 * @Override
 * public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
 *     super.onRemove(state, level, pos, newState, isMoving);
 *     BlockEntity be = level.getBlockEntity(pos);
 *     if (be instanceof ElectricCableBlockEntity cable) {
 *         cable.onNeighborsChanged(); // 保険
 *     }
 * }
 * ────────────────────────────────────────────────────────────── */