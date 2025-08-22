package com.infinitymax.industry.registry;

import com.infinitymax.api.ProjectInfinityMaxAPI;
import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.blockentity.MachineBlockEntity;
import com.infinitymax.industry.energy.ElectricCableBlock;
import com.infinitymax.industry.energy.ElectricCableBlockEntity;
import com.infinitymax.industry.fluid.FluidPipeBlock;
import com.infinitymax.industry.fluid.FluidPipeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.*;

public class RegistryManager {

    // ====== 保持用 ======
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    // ====== 公開エントリポイント ======
    public static void init() {
        registerItems();
        registerBlocks();
        registerBlockEntities();
        System.out.println("[InfinityMax-Industry] Registry done: items=" + ITEMS.size() + ", blocks=" + BLOCKS.size());
    }

    // ====== アイテム登録 ======
    private static void registerItems() {
        // 1) 基礎素材（素材系）
        addItem("iron_plate");
        addItem("sheet_copper"); // copper plate
        addItem("steel_ingot");
        addItem("aluminum_plate");
        addItem("titanium_plate");
        addItem("nickel_plate");
        addItem("tin_plate");
        addItem("lead_plate");
        addItem("zinc_plate");
        addItem("carbon_fiber");
        addItem("alloy_ingot");
        addItem("copper_wire");
        addItem("gold_wire");
        addItem("fiber_optic_cable");
        addItem("lubricant_oil");
        addItem("plastic_resin");
        addItem("rubber");
        addItem("polymer");

        // 10) 消耗品・資材
        addItem("fuel_coal_chunk"); // 燃料は後でFluid/Container化予定、暫定アイテムで表現
        addItem("fuel_oil_can");
        addItem("fuel_natural_gas_cell");
        addItem("fuel_diesel_can");
        addItem("fuel_gasoline_can");
        addItem("nuclear_fuel_rod"); // 原子燃料棒
        addItem("hydrogen_cell");
        addItem("catalyst_industrial");
        addItem("chemical_acid");
        addItem("chemical_alkali");
        addItem("explosive_compound");
        addItem("gear");
        addItem("motor");
        addItem("bearing");
        addItem("circuit_board");

        // 8) 武器・兵器（工業産物） → とりあえずプレースホルダ
        addItem("rifle");
        addItem("shotgun");
        addItem("handgun");
        addItem("machine_gun");
        addItem("rocket_launcher");
        addItem("gas_bomb");
        addItem("flamethrower");
        addItem("turret_module");
        addItem("combat_drone_controller");
        addItem("ammo_bullet");
        addItem("ammo_grenade");
        addItem("ammo_laser_cell");

        // 7) ロボット・自動化（コントローラ/ユニット）
        addItem("logistics_robot_core");
        addItem("assembler_robot_core");
        addItem("welder_robot_core");
        addItem("mining_drone_core");
        addItem("construction_bot_core");
        addItem("combat_drone_core");

        // 9) 輸送システム（将来Entity化予定のプレースホルダ）
        addItem("industrial_truck_key");
        addItem("forklift_key");
        addItem("mining_dump_truck_key");
        addItem("rail_freight_coupler");
        addItem("autonomous_car_key");

        // 実登録
        ITEMS.forEach(ProjectInfinityMaxAPI::registerItem);
    }

    private static void addItem(String id) {
        ITEMS.put(id, new Item(new Item.Properties()));
    }

    // ====== ブロック登録 ======
    private static void registerBlocks() {
        // 2) 構造ブロック（建築/基礎）
        addSimpleBlock("steel_frame_block");
        addSimpleBlock("reinforced_concrete_block");
        addSimpleBlock("insulation_block");
        addSimpleBlock("tank_block");           // タンク
        addSimpleBlock("industrial_chest");     // 大容量チェスト

        // 3) 加工・製造機械（軽工業） → MachineBlockで一元化
        addMachine("crusher_block", MachineBlock.Kind.CRUSHER);
        addMachine("industrial_furnace_block", MachineBlock.Kind.INDUSTRIAL_FURNACE);
        addMachine("rolling_mill_block", MachineBlock.Kind.ROLLING_MILL);
        addMachine("assembler_block", MachineBlock.Kind.ASSEMBLER);
        addMachine("additive_fabricator_block", MachineBlock.Kind.ADDITIVE_FABRICATOR);
        addMachine("assembly_line_module_block", MachineBlock.Kind.ASSEMBLY_LINE);
        addMachine("packing_machine_block", MachineBlock.Kind.PACKING_MACHINE);

        // 4) 重工業機械
        addMachine("blast_furnace_block", MachineBlock.Kind.BLAST_FURNACE);
        addMachine("electric_furnace_block", MachineBlock.Kind.ELECTRIC_FURNACE);
        addMachine("smelter_block", MachineBlock.Kind.SMELTER);
        addMachine("compressor_block", MachineBlock.Kind.COMPRESSOR);
        addMachine("gas_refiner_block", MachineBlock.Kind.GAS_REFINER);
        addMachine("excavator_machine_block", MachineBlock.Kind.EXCAVATOR);
        addMachine("conveyor_belt_block", MachineBlock.Kind.CONVEYOR_BELT);
        addMachine("industrial_crane_block", MachineBlock.Kind.INDUSTRIAL_CRANE);
        addMachine("robotic_arm_block", MachineBlock.Kind.ROBOTIC_ARM);

        // 5) 化学工業
        addMachine("chemical_reactor_block", MachineBlock.Kind.CHEM_REACTOR);
        addMachine("distillation_tower_block", MachineBlock.Kind.DISTILLATION_TOWER);
        addMachine("electrolyzer_block", MachineBlock.Kind.ELECTROLYZER);
        addMachine("gas_separator_block", MachineBlock.Kind.GAS_SEPARATOR);
        addMachine("polymerizer_block", MachineBlock.Kind.POLYMERIZER);
        addMachine("fertilizer_synthesizer_block", MachineBlock.Kind.FERTILIZER_SYNTH);
        addMachine("wastewater_processor_block", MachineBlock.Kind.WASTEWATER_PROCESSOR);
        addMachine("nuclear_waste_processor_block", MachineBlock.Kind.NUCLEAR_WASTE_PROCESSOR);

        // 6) エネルギーシステム
        addMachine("coal_generator_block", MachineBlock.Kind.COAL_GEN);
        addMachine("oil_generator_block", MachineBlock.Kind.OIL_GEN);
        addMachine("gas_turbine_generator_block", MachineBlock.Kind.GAS_TURBINE);
        addMachine("nuclear_reactor_block", MachineBlock.Kind.NUCLEAR_REACTOR);
        addMachine("fast_breeder_reactor_block", MachineBlock.Kind.FAST_BREEDER);
        addMachine("solar_panel_block", MachineBlock.Kind.SOLAR_PANEL);
        addMachine("wind_turbine_block", MachineBlock.Kind.WIND_TURBINE);
        addMachine("hydro_turbine_block", MachineBlock.Kind.HYDRO_TURBINE);
        addMachine("fuel_cell_generator_block", MachineBlock.Kind.FUEL_CELL);
        addMachine("battery_bank_block", MachineBlock.Kind.BATTERY_BANK);
        addMachine("transformer_block", MachineBlock.Kind.TRANSFORMER);
        addMachine("superconductor_storage_block", MachineBlock.Kind.SUPERCONDUCTOR_STORAGE);
        addMachine("power_transmission_anchor_block", MachineBlock.Kind.POWER_TRANSMISSION_ANCHOR);

        BLOCKS.put("fluid_pipe_block", new FluidPipeBlock());
        BLOCKS.put("power_cable_block", new ElectricCableBlock());

        // 実登録
        BLOCKS.forEach(ProjectInfinityMaxAPI::registerBlock);
    }

    private static void addSimpleBlock(String id) {
        BLOCKS.put(id, new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 10.0f).requiresCorrectToolForDrops()));
    }

    private static void addMachine(String id, MachineBlock.Kind kind) {
        BLOCKS.put(id, new MachineBlock(kind));
    }

    // ====== ブロックエンティティ登録 ======
    private static void registerBlockEntities() {

        // FluidPipe
        BlockEntityType<FluidPipeBlockEntity> fluidPipeType = BlockEntityType.Builder
                .of(FluidPipeBlockEntity::new, BLOCKS.get("fluid_pipe_block"))
                .build(null);
        FluidPipeBlockEntity.TYPE = fluidPipeType;
        ProjectInfinityMaxAPI.registerBlockEntity("fluid_pipe_entity", fluidPipeType);

        // ElectricCable
        BlockEntityType<ElectricCableBlockEntity> cableType = BlockEntityType.Builder
                .of(ElectricCableBlockEntity::new, BLOCKS.get("power_cable_block"))
                .build(null);
        ElectricCableBlockEntity.TYPE = cableType;
        ProjectInfinityMaxAPI.registerBlockEntity("electric_cable_entity", cableType);

        // MachineBlock を全て束ねて1つの BlockEntityType に
        List<Block> machineBlocks = new ArrayList<>();
        BLOCKS.forEach((id, b) -> { if (b instanceof MachineBlock) machineBlocks.add(b); });

        @SuppressWarnings("unchecked")
        BlockEntityType<MachineBlockEntity> type = BlockEntityType.Builder
                .of((pos, state) -> new MachineBlockEntity(pos, state,
                        (state.getBlock() instanceof MachineBlock mb) ? mb.kind : MachineBlock.Kind.CRUSHER),
                    machineBlocks.toArray(new Block[0]))
                .build(null);

        MachineBlockEntity.TYPE = type; // BEクラスに注入しておく
        ProjectInfinityMaxAPI.registerBlockEntity("machine_block_entity", type);
    }
}
