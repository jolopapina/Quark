package vazkii.quark.content.building.module;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import vazkii.quark.base.block.QuarkFenceGateBlock;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;

/**
 * @author WireSegal
 * Created at 10:51 AM on 10/9/19.
 */
@LoadModule(category = ModuleCategory.BUILDING)
public class NetherBrickFenceGateModule extends QuarkModule {
    @Override
    public void construct() {
        new QuarkFenceGateBlock("nether_brick_fence_gate", this, CreativeModeTab.TAB_REDSTONE,
                Block.Properties.of(Material.STONE, MaterialColor.NETHER)
                .requiresCorrectToolForDrops()
                .sound(SoundType.NETHER_BRICKS)
                .strength(2.0F, 6.0F));
    }
}
