package com.infinitymax.industry.registry;

import com.infinitymax.api.ProjectInfinityMaxAPI;
import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.blockentity.MachineBlockEntity;
import com.infinitymax.industry.energy.ElectricCableBlock;
import com.infinitymax.industry.energy.ElectricCableBlockEntity;
import com.infinitymax.industry.fluid.FluidPipeBlock;
import com.infinitymax.industry.fluid.FluidPipeBlockEntity;
import com.infinitymax.industry.gui.machine.MachineMenu;
import com.infinitymax.industry.recipe.RecipeBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.*;

import static com.infinitymax.industry.block.MachineBlock.Kind.*;

public final class RegistryManager {

    private RegistryManager(){}

    // 保持
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    // 公開エントリポイント
    public static void init() {
        registerItems();
        registerBlocks();
        registerBlockItems();
        registerBlockEntities();
        registerMenuTypes();

        // レシピ初期登録（任意）
        RecipeBootstrap.init();

        System.out.println("[InfinityMax-Industry] Registry done: items=" + ITEMS.size() + ", blocks=" + BLOCKS.size());
    }

    // ========== アイテム ==========
    private static void registerItems() {
        // …あなたのリストを addItem("id") でずらっと
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

        ITEMS.forEach(ProjectInfinityMaxAPI::registerItem);
    }
    private static void addItem(String id) { ITEMS.put(id, new Item(new Item.Properties())); }

    // ========== ブロック ==========
    private static void registerBlocks() {
        addSimpleBlock("steel_frame_block");
        addSimpleBlock("reinforced_concrete_block");
        addSimpleBlock("insulation_block");
        addSimpleBlock("tank_block");
        addSimpleBlock("industrial_chest");

        // 軽工業
        addMachine("crusher_block", CRUSHER);
        addMachine("industrial_furnace_block", INDUSTRIAL_FURNACE);
        addMachine("rolling_mill_block", ROLLING_MILL);
        addMachine("assembler_block", ASSEMBLER);
        addMachine("additive_fabricator_block", ADDITIVE_FABRICATOR);
        addMachine("assembly_line_module_block", ASSEMBLY_LINE);
        addMachine("packing_machine_block", PACKING_MACHINE);

        // 重工業
        addMachine("blast_furnace_block", BLAST_FURNACE);
        addMachine("electric_furnace_block", ELECTRIC_FURNACE);
        addMachine("smelter_block", SMELTER);
        addMachine("compressor_block", COMPRESSOR);
        addMachine("gas_refiner_block", GAS_REFINER);
        addMachine("excavator_machine_block", EXCAVATOR);
        addMachine("conveyor_belt_block", CONVEYOR_BELT);
        addMachine("industrial_crane_block", INDUSTRIAL_CRANE);
        addMachine("robotic_arm_block", ROBOTIC_ARM);

        // 化学
        addMachine("chemical_reactor_block", CHEM_REACTOR);
        addMachine("distillation_tower_block", DISTILLATION_TOWER);
        addMachine("electrolyzer_block", ELECTROLYZER);
        addMachine("gas_separator_block", GAS_SEPARATOR);
        addMachine("polymerizer_block", POLYMERIZER);
        addMachine("fertilizer_synthesizer_block", FERTILIZER_SYNTH);
        addMachine("wastewater_processor_block", WASTEWATER_PROCESSOR);
        addMachine("nuclear_waste_processor_block", NUCLEAR_WASTE_PROCESSOR);

        // エネルギー
        addMachine("coal_generator_block", COAL_GEN);
        addMachine("oil_generator_block", OIL_GEN);
        addMachine("gas_turbine_generator_block", GAS_TURBINE);
        addMachine("nuclear_reactor_block", NUCLEAR_REACTOR);
        addMachine("fast_breeder_reactor_block", FAST_BREEDER);
        addMachine("solar_panel_block", SOLAR_PANEL);
        addMachine("wind_turbine_block", WIND_TURBINE);
        addMachine("hydro_turbine_block", HYDRO_TURBINE);
        addMachine("fuel_cell_generator_block", FUEL_CELL);
        addMachine("battery_bank_block", BATTERY_BANK);
        addMachine("transformer_block", TRANSFORMER);
        addMachine("superconductor_storage_block", SUPERCONDUCTOR_STORAGE);
        addMachine("power_transmission_anchor_block", POWER_TRANSMISSION_ANCHOR);

        BLOCKS.put("fluid_pipe_block", new FluidPipeBlock());
        BLOCKS.put("power_cable_block", new ElectricCableBlock());

        BLOCKS.forEach(ProjectInfinityMaxAPI::registerBlock);
    }

    private static void addSimpleBlock(String id) {
        BLOCKS.put(id, new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 10.0f).requiresCorrectToolForDrops()));
    }
    private static void addMachine(String id, MachineBlock.Kind kind) {
        BLOCKS.put(id, new MachineBlock(kind));
    }

    // BlockItem を自動生成（見落としがち）
    private static void registerBlockItems() {
        for (var e : BLOCKS.entrySet()) {
            String id = e.getKey();
            Block b = e.getValue();
            ITEMS.putIfAbsent(id, new BlockItem(b, new Item.Properties()));
        }
        // APIへ
        ITEMS.forEach(ProjectInfinityMaxAPI::registerItem);
    }

    // ========== BlockEntity ==========
    private static void registerBlockEntities() {
        // MachineBlock を集めて1つの BE Type に束ねる
        List<Block> machineBlocks = new ArrayList<>();
        BLOCKS.forEach((id, b) -> { if (b instanceof MachineBlock) machineBlocks.add(b); });

        BlockEntityType<MachineBlockEntity> type = BlockEntityType.Builder
                .of((pos, state) -> new MachineBlockEntity(pos, state,
                        (state.getBlock() instanceof MachineBlock mb) ? mb.kind : CRUSHER),
                    machineBlocks.toArray(new Block[0]))
                .build(null);

        MachineBlockEntity.TYPE = type;
        ProjectInfinityMaxAPI.registerBlockEntity("machine_block_entity", type);

        // ほか：FluidPipe / ElectricCable なども同様に…
        BlockEntityType<FluidPipeBlockEntity> fluidType =
                BlockEntityType.Builder.of(FluidPipeBlockEntity::new, BLOCKS.get("fluid_pipe_block")).build(null);
        FluidPipeBlockEntity.TYPE = fluidType;
        ProjectInfinityMaxAPI.registerBlockEntity("fluid_pipe_entity", fluidType);

        BlockEntityType<ElectricCableBlockEntity> cableType =
                BlockEntityType.Builder.of(ElectricCableBlockEntity::new, BLOCKS.get("power_cable_block")).build(null);
        ElectricCableBlockEntity.TYPE = cableType;
        ProjectInfinityMaxAPI.registerBlockEntity("power_cable_entity", cableType);

        // Coal generator
        BlockEntityType<CoalGeneratorBlockEntity> coalGenType = 
                BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new, BLOCKS.get("coal_generator_block")).build(null);
            CoalGeneratorBlockEntity.TYPE = coalGenType;
            ProjectInfinityMaxAPI.registerBlockEntity("coal_generator_entity", coalGenType);

        // Transformer
        BlockEntityType<TransformerBlockEntity> transType = 
                BlockEntityType.Builder.of(TransformerBlockEntity::new, BLOCKS.get("transformer_block")).build(null);
            TransformerBlockEntity.TYPE = transType;
            ProjectInfinityMaxAPI.registerBlockEntity("transformer_entity", transType);

        // Fluid tank
        BlockEntityType<FluidTankBlockEntity> tankType = 
                BlockEntityType.Builder.of(FluidTankBlockEntity::new, BLOCKS.get("tank_block")).build(null);
            FluidTankBlockEntity.TYPE = tankType;
            ProjectInfinityMaxAPI.registerBlockEntity("tank_entity", tankType);

    }

    // ========== MenuType（GUI） ==========
    // APIに registerMenuType(...) がある前提。無い場合は API に1メソッド追加してください。
    private static void registerMenuTypes() {
        MenuType<MachineMenu> machineMenu = new MenuType<>(
                (id, inv) -> new MachineMenu(id, inv, new SimpleContainer(3), new SimpleContainerData(4), BlockPos.ZERO)
        );
        // Recipe Serializer / Type 登録（必須）
        Registry.register(Registry.RECIPE_SERIALIZER, new ResourceLocation("infinitymax", "machine_recipe"), MachineRecipe.Serializer.INSTANCE);
        RecipeType<MachineRecipe> rtype = RecipeType.register("infinitymax:machine_recipe"); // or MachineRecipeTypes.MACHINE

        // MenuType 登録（Generatorなど）
        ProjectInfinityMaxAPI.registerMenuType("generator_menu", generatorMenuType);
        ProjectInfinityMaxAPI.registerMenuType("machine_menu", machineMenu);
    }
}