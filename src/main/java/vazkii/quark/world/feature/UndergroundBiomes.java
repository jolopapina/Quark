package vazkii.quark.world.feature;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.arl.block.BlockMod;
import vazkii.arl.block.BlockModSlab;
import vazkii.arl.block.BlockModStairs;
import vazkii.arl.recipe.RecipeHandler;
import vazkii.arl.util.ProxyRegistry;
import vazkii.quark.base.handler.BiomeTypeConfigHandler;
import vazkii.quark.base.handler.DimensionConfig;
import vazkii.quark.base.module.Feature;
import vazkii.quark.base.module.GlobalConfig;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.building.feature.VanillaWalls;
import vazkii.quark.world.block.*;
import vazkii.quark.world.block.slab.BlockElderPrismarineSlab;
import vazkii.quark.world.block.slab.BlockFireStoneSlab;
import vazkii.quark.world.block.slab.BlockIcyStoneSlab;
import vazkii.quark.world.block.stairs.BlockElderPrismarineStairs;
import vazkii.quark.world.block.stairs.BlockFireStoneStairs;
import vazkii.quark.world.block.stairs.BlockIcyStoneStairs;
import vazkii.quark.world.world.UndergroundBiomeGenerator;
import vazkii.quark.world.world.underground.*;

import java.util.ArrayList;
import java.util.List;

public class UndergroundBiomes extends Feature {

	public static List<UndergroundBiomeGenerator> biomes;
	
	public static BlockMod biome_cobblestone;
	public static BlockMod glowcelium;
	public static Block glowshroom;
	public static Block glowshroom_block;
	public static Block elder_prismarine;
	public static Block elder_sea_lantern;
	
	public static int glowshroomGrowthRate;
	
	public static IBlockState firestoneState, icystoneState;
	
	public static boolean firestoneEnabled, icystoneEnabled, glowceliumEnabled, bigGlowshroomsEnabled, elderPrismarineEnabled;
	boolean enableStairsAndSlabs, enableWalls, allowCraftingElderPrismarine;
	
	private static UndergroundBiomeGenerator prismarineBiomeGen;
	
	@Override
	public void setupConfig() {
		biomes = new ArrayList<>();
		
		firestoneEnabled = loadPropBool("Enable Firestone", "", true);
		icystoneEnabled = loadPropBool("Enable Froststone", "", true);
		glowceliumEnabled = loadPropBool("Enable Glowcelium and Glowshrooms", "", true);
		bigGlowshroomsEnabled = loadPropBool("Enable Big Glowshrooms", "", true);
		elderPrismarineEnabled = loadPropBool("Enable Elder Prismarine", "", true);
		enableStairsAndSlabs = loadPropBool("Enable stairs and slabs", "", true)  && GlobalConfig.enableVariants;
		enableWalls = loadPropBool("Enable walls", "", true)  && GlobalConfig.enableVariants;
		allowCraftingElderPrismarine = loadPropBool("Allow crafting Elder Prismarine", "", true);

		glowshroomGrowthRate = loadPropInt("Glowshroom Growth Rate", "The smaller, the faster glowshrooms will spread. Vanilla mushroom speed is 25.", 20);
		
		biomes.add(loadUndergrondBiomeInfo("Lush", new UndergroundBiomeLush(), 80, Type.JUNGLE));
		biomes.add(loadUndergrondBiomeInfo("Sandstone", new UndergroundBiomeSandstone(), 80, Type.SANDY));
		biomes.add(loadUndergrondBiomeInfo("Slime", new UndergroundBiomeSlime(), 120, Type.SWAMP));
		biomes.add(prismarineBiomeGen = loadUndergrondBiomeInfo("Prismarine", new UndergroundBiomePrismarine(), 100, Type.OCEAN));
		biomes.add(loadUndergrondBiomeInfo("Spider", new UndergroundBiomeSpiderNest(), 80, Type.PLAINS));
		biomes.add(loadUndergrondBiomeInfo("Overgrown", new UndergroundBiomeOvergrown(), 80, Type.FOREST));
		biomes.add(loadUndergrondBiomeInfo("Icy", new UndergroundBiomeIcy(), 80, Type.COLD));
		biomes.add(loadUndergrondBiomeInfo("Lava", new UndergroundBiomeLava(), 80, Type.MESA));
		biomes.add(loadUndergrondBiomeInfo("Glowshroom", new UndergroundBiomeGlowshroom(), 80, Type.MOUNTAIN, Type.MUSHROOM));
		
		if(elder_prismarine != null)
			((UndergroundBiomePrismarine) prismarineBiomeGen.info.biome).update();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void preInit(FMLPreInitializationEvent event) {
		if(firestoneEnabled || icystoneEnabled)
			biome_cobblestone = new BlockBiomeCobblestone();
		
		if(elderPrismarineEnabled) {
			elder_prismarine = new BlockElderPrismarine();
			elder_sea_lantern = new BlockElderSeaLantern();
			
			if(allowCraftingElderPrismarine)
				RecipeHandler.addShapelessOreDictRecipe(ProxyRegistry.newStack(elder_prismarine, 2), ProxyRegistry.newStack(elder_prismarine), ProxyRegistry.newStack(Blocks.PRISMARINE));
			
			RecipeHandler.addOreDictRecipe(ProxyRegistry.newStack(elder_prismarine, 4, 1), 
					"PPP", "PPP", "PPP",
					'P', ProxyRegistry.newStack(elder_prismarine));
			RecipeHandler.addOreDictRecipe(ProxyRegistry.newStack(elder_prismarine, 1, 2), 
					"PPP", "PBP", "PPP",
					'P', ProxyRegistry.newStack(elder_prismarine),
					'B', "dyeBlack");
			RecipeHandler.addShapelessOreDictRecipe(ProxyRegistry.newStack(elder_sea_lantern), ProxyRegistry.newStack(elder_prismarine), ProxyRegistry.newStack(Blocks.SEA_LANTERN));
		}
		((UndergroundBiomePrismarine) prismarineBiomeGen.info.biome).update();
		
		if(enableStairsAndSlabs) {
			if(firestoneEnabled) {
				BlockModSlab.initSlab(biome_cobblestone, 0, new BlockFireStoneSlab(false), new BlockFireStoneSlab(true));
				BlockModStairs.initStairs(biome_cobblestone, 0, new BlockFireStoneStairs());
			}
			
			if(icystoneEnabled) {
				BlockModSlab.initSlab(biome_cobblestone, 1, new BlockIcyStoneSlab(false), new BlockIcyStoneSlab(true));
				BlockModStairs.initStairs(biome_cobblestone, 1, new BlockIcyStoneStairs());
			}
			
			if(elderPrismarineEnabled) {
				for(BlockElderPrismarine.Variants v : BlockElderPrismarine.Variants.values())
					BlockModSlab.initSlab(elder_prismarine, v.ordinal(), new BlockElderPrismarineSlab(v, false), new BlockElderPrismarineSlab(v, true));
				for(BlockElderPrismarine.Variants v : BlockElderPrismarine.Variants.values())
					BlockModStairs.initStairs(elder_prismarine, v.ordinal(), new BlockElderPrismarineStairs(v));
			}
		}

		VanillaWalls.add("fire_stone", biome_cobblestone, 0, enableWalls && firestoneEnabled);
		VanillaWalls.add("icy_stone", biome_cobblestone, 1, enableWalls && icystoneEnabled);
		for(BlockElderPrismarine.Variants v : BlockElderPrismarine.Variants.values())
			VanillaWalls.add(v.getName(), elder_prismarine, v.ordinal(), enableWalls && elderPrismarineEnabled);

		if(glowceliumEnabled) {
			glowcelium = new BlockGlowcelium();
			glowshroom = new BlockGlowshroom();
			
			if(bigGlowshroomsEnabled)
				glowshroom_block = new BlockHugeGlowshroom();
			
			RecipeHandler.addShapelessOreDictRecipe(new ItemStack(Items.MUSHROOM_STEW), "mushroomAny", "mushroomAny", new ItemStack(Items.BOWL));
		}
		
		if(firestoneEnabled)
			firestoneState = biome_cobblestone.getDefaultState().withProperty(biome_cobblestone.getVariantProp(), BlockBiomeCobblestone.Variants.FIRE_STONE);
		if(icystoneEnabled)
			icystoneState = biome_cobblestone.getDefaultState().withProperty(biome_cobblestone.getVariantProp(), BlockBiomeCobblestone.Variants.ICY_STONE);

		addOreDict();
	}
	
	private void addOreDict() {
		if(glowceliumEnabled) {
			addOreDict("mushroomAny", Blocks.RED_MUSHROOM);
			addOreDict("mushroomAny", Blocks.BROWN_MUSHROOM);	
			addOreDict("mushroomAny", glowshroom);
		}
	}
	
	@SubscribeEvent
	public void onOreGenerate(OreGenEvent.GenerateMinable event) {
		if(event.getType() == EventType.DIRT) {
			World world = event.getWorld();
			BlockPos pos = event.getPos();
			
			Chunk chunk = world.getChunk(pos);

			for(UndergroundBiomeGenerator gen : biomes)
				gen.generate(chunk.x, chunk.z, world);
		}
	}
	
	@Override
	public boolean hasOreGenSubscriptions() {
		return true;
	}
	
	@Override
	public boolean requiresMinecraftRestartToEnable() {
		return true;
	}
	
	private UndergroundBiomeGenerator loadUndergrondBiomeInfo(String name, UndergroundBiome biome, int rarity, BiomeDictionary.Type... biomes) {
		String category = configCategory + "." + name;
		UndergroundBiomeInfo info = new UndergroundBiomeInfo(category, biome, rarity, biomes);

		return new UndergroundBiomeGenerator(info);
	}
	
	public static class UndergroundBiomeInfo {
		
		public final boolean enabled;
		public final UndergroundBiome biome;
		public final DimensionConfig dims;
		public final List<BiomeDictionary.Type> types;
		public final int rarity;
		public final int minXSize, minYSize, minZSize;
		public final int xVariation, yVariation, zVariation;
		public final int minY, maxY;
		
		private UndergroundBiomeInfo(String category, UndergroundBiome biome, int rarity, BiomeDictionary.Type... biomes) {
			this.enabled = ModuleLoader.config.getBoolean("Enabled", category, true, "");
			this.biome = biome;
			this.types = BiomeTypeConfigHandler.parseBiomeTypeArrayConfig("Allowed Biome Types", category, biomes);
			this.rarity = ModuleLoader.config.getInt("Rarity", category, rarity, 0, Integer.MAX_VALUE, "This biome will spawn in 1 of X valid chunks");
			
			dims = new DimensionConfig(category);
			
			minY = ModuleLoader.config.getInt("Minimum Y Level", category, 10, 0, 255, "");
			maxY = ModuleLoader.config.getInt("Maximum Y Level", category, 40, 0, 255, "");
			
			minXSize = ModuleLoader.config.getInt("X Minimum", category, 26, 0, Integer.MAX_VALUE, "");
			minYSize = ModuleLoader.config.getInt("Y Minimum", category, 12, 0, Integer.MAX_VALUE, "");
			minZSize = ModuleLoader.config.getInt("Z Minimum", category, 26, 0, Integer.MAX_VALUE, "");
			
			xVariation = ModuleLoader.config.getInt("X Variation", category, 14, 0, Integer.MAX_VALUE, "");
			yVariation = ModuleLoader.config.getInt("Y Variation", category, 6, 0, Integer.MAX_VALUE, "");
			zVariation = ModuleLoader.config.getInt("Z Variation", category, 14, 0, Integer.MAX_VALUE, "");
			
			biome.setupBaseConfig(category);
		}
		
	}
	
}
