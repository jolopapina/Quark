/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * File Created @ [26/03/2016, 21:31:04 (GMT)]
 */
package vazkii.quark.base.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class ClassTransformer implements IClassTransformer {

	private static final String ASM_HOOKS = "vazkii/quark/base/asm/ASMHooks";

	private static final Map<String, Transformer> transformers = new HashMap<>();

	static {
		// For Emotes
		transformers.put("net.minecraft.client.model.ModelBiped", ClassTransformer::transformModelBiped);

		// For Color Runes
		transformers.put("net.minecraft.client.renderer.RenderItem", ClassTransformer::transformRenderItem);
		transformers.put("net.minecraft.client.renderer.entity.layers.LayerArmorBase", ClassTransformer::transformLayerArmorBase);

		// For Boat Sails
		transformers.put("net.minecraft.client.renderer.entity.RenderBoat", ClassTransformer::transformRenderBoat);
		transformers.put("net.minecraft.entity.item.EntityBoat", ClassTransformer::transformEntityBoat);

		// For Piston Block Breakers and Pistons Move TEs
		transformers.put("net.minecraft.block.BlockPistonBase", ClassTransformer::transformBlockPistonBase);

		// For Better Craft Shifting
		transformers.put("net.minecraft.inventory.ContainerWorkbench", ClassTransformer::transformContainerWorkbench);
		transformers.put("net.minecraft.inventory.ContainerMerchant", ClassTransformer::transformContainerMerchant);

		// For Pistons Move TEs
		transformers.put("net.minecraft.tileentity.TileEntityPiston", ClassTransformer::transformTileEntityPiston);
		transformers.put("net.minecraft.client.renderer.tileentity.TileEntityPistonRenderer", ClassTransformer::transformTileEntityPistonRenderer);

		// For Imrpoved Sleeping
		transformers.put("net.minecraft.world.WorldServer", ClassTransformer::transformWorldServer);

		// For Colored Lights
		transformers.put("net.minecraft.client.renderer.BlockModelRenderer", ClassTransformer::transformBlockModelRenderer);

		// For More Banner Layers
		transformers.put("net.minecraft.item.crafting.RecipesBanners$RecipeAddPattern", ClassTransformer::transformRecipeAddPattern);
		transformers.put("net.minecraft.item.ItemBanner", ClassTransformer::transformItemBanner);
		
		// Better Fire Effect
		transformers.put("net.minecraft.client.renderer.entity.Render", ClassTransformer::transformRender);
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if(transformers.containsKey(transformedName)) {
			log("Transforming " + transformedName);
			return transformers.get(transformedName).apply(basicClass);
		}

		return basicClass;
	}

	private static byte[] transformModelBiped(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("setRotationAngles", "func_78087_a", "a", "(FFFFFFLnet/minecraft/entity/Entity;)V");

		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.RETURN;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 7));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "updateEmotes", "(Lnet/minecraft/entity/Entity;)V", false));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
	}

	private static byte[] transformRenderItem(byte[] basicClass) {
		MethodSignature sig1 = new MethodSignature("renderItem", "func_180454_a", "a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V");
		MethodSignature sig2 = new MethodSignature("renderEffect", "func_191966_a", "a", "(Lnet/minecraft/client/renderer/block/model/IBakedModel;)V");

		byte[] transClass = basicClass;

		transClass = transform(transClass, forMethod(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				}, (MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "setColorRuneTargetStack", "(Lnet/minecraft/item/ItemStack;)V", false));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));

		transClass = transform(transClass, forMethod(sig2, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.LDC && ((LdcInsnNode) node).cst.equals(-8372020);
				}, (MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "getRuneColor", "(I)I", false));

					method.instructions.insert(node, newInstructions);
					return false;
				})));

		return transClass;
	}

	static int invokestaticCount = 0;
	private static byte[] transformLayerArmorBase(byte[] basicClass) {
		MethodSignature sig1 = new MethodSignature("renderArmorLayer", "func_188361_a", "a", "(Lnet/minecraft/entity/EntityLivingBase;FFFFFFFLnet/minecraft/inventory/EntityEquipmentSlot;)V");
		MethodSignature sig2 = new MethodSignature("renderEnchantedGlint", "func_188364_a", "a", "(Lnet/minecraft/client/renderer/entity/RenderLivingBase;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/model/ModelBase;FFFFFFF)V");

		byte[] transClass = basicClass;

		transClass = transform(transClass, forMethod(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.ASTORE;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 10));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "setColorRuneTargetStack", "(Lnet/minecraft/item/ItemStack;)V", false));

					method.instructions.insert(node, newInstructions);
					return true;
				})));

		if(!hasOptifine(sig2.toString())) {
			invokestaticCount = 0;
			transClass = transform(transClass, forMethod(sig2, combine(
					(AbstractInsnNode node) -> { // Filter
						return node.getOpcode() == Opcodes.INVOKESTATIC && ((MethodInsnNode) node).desc.equals("(FFFF)V");
					},
					(MethodNode method, AbstractInsnNode node) -> { // Action
						invokestaticCount++;

						InsnList newInstructions = new InsnList();

						newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "applyRuneColor", "()V", false));

						method.instructions.insert(node, newInstructions);
						return invokestaticCount == 2;
					})));
		}

		return transClass;
	}

	private static byte[] transformEntityBoat(byte[] basicClass) {
		MethodSignature sig1 = new MethodSignature("attackEntityFrom", "func_70097_a", "a", "(Lnet/minecraft/util/DamageSource;F)Z");
		MethodSignature sig2 = new MethodSignature("onUpdate", "func_70071_h_", "B_", "()V");

		byte[] transClass = transform(basicClass, forMethod(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.POP;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "dropBoatBanner", "(Lnet/minecraft/entity/item/EntityBoat;)V", false));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));

		transClass = transform(transClass, forMethod(sig2, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "onBoatUpdate", "(Lnet/minecraft/entity/item/EntityBoat;)V", false));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));

		return transClass;
	}

	private static byte[] transformRenderBoat(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("doRender", "func_188300_b", "b", "(Lnet/minecraft/entity/item/EntityBoat;DDDFF)V");

		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return (node.getOpcode() == Opcodes.INVOKEVIRTUAL || node.getOpcode() == Opcodes.INVOKEINTERFACE)
							&& checkDesc(((MethodInsnNode) node).desc, "(Lnet/minecraft/entity/Entity;FFFFFF)V");
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.FLOAD, 9));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "renderBannerOnBoat", "(Lnet/minecraft/entity/item/EntityBoat;F)V", false));

					method.instructions.insert(node, newInstructions);
					return true;
				})));
	}

	private static int aloadCount = 0;
	private static byte[] transformBlockPistonBase(byte[] basicClass) {
		MethodSignature sig1 = new MethodSignature("doMove", "func_176319_a", "a", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Z)Z");
		MethodSignature sig2 = new MethodSignature("canPush", "func_185646_a", "a", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;ZLnet/minecraft/util/EnumFacing;)Z");
		
		
		byte[] transClass = transform(basicClass, forMethod(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					if(node.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) node).var == 5) {
						aloadCount++;
						if(aloadCount == 2)
							return true;
					}	
					
					return false;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
					newInstructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "onPistonMove", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockPistonStructureHelper;Lnet/minecraft/util/EnumFacing;Z)V", false));

					method.instructions.insert(node, newInstructions);
					return true;
				})));

		transClass = transform(transClass, forMethod(sig2, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) node).name.equals("hasTileEntity");
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "shouldPistonMoveTE", "(ZLnet/minecraft/block/state/IBlockState;)Z", false));

					method.instructions.insert(node, newInstructions);
					return true;
				})));

		return transClass;
	}

	private static byte[] transformContainerWorkbench(byte[] basicClass) {
		return transformTransferStackInSlot(basicClass, 5, 6, "getInventoryBoundaryCrafting");
	}

	private static byte[] transformContainerMerchant(byte[] basicClass) {
		return transformTransferStackInSlot(basicClass, 3, 4, "getInventoryBoundaryVillager");
	}

	static int bipushCount = 0;
	private static byte[] transformTransferStackInSlot(byte[] basicClass, int min, int max, String hook) {
		MethodSignature sig = new MethodSignature("transferStackInSlot", "func_82846_b", "b", "(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;");

		bipushCount = 0;
		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filte
					return node.getOpcode() == Opcodes.BIPUSH;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();
					bipushCount++;

					if(bipushCount != min && bipushCount != max)
						return false;

					log("Adding invokestatic to " + ((IntInsnNode) node).operand + "/" + bipushCount);
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, hook, "(I)I", false));

					method.instructions.insert(node, newInstructions);
					return bipushCount == max;
				})));
	}

	private static byte[] transformTileEntityPiston(byte[] basicClass) {
		MethodSignature clearPistonTileEntitySig = new MethodSignature("clearPistonTileEntity", "func_145866_f", "j", "()V");
		MethodSignature updateSig = new MethodSignature("update", "func_73660_a", "e", "()V");

		MethodAction setPistonBlockAction = combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.INVOKEVIRTUAL && checkDesc(((MethodInsnNode) node).desc, "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z");
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "setPistonBlock", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", false));

					method.instructions.insert(node, newInstructions);
					method.instructions.remove(node);

					return true;
				});
		
		MethodAction onUpdateAction = combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "onPistonUpdate", "(Lnet/minecraft/tileentity/TileEntityPiston;)V", false));

					method.instructions.insertBefore(node, newInstructions);

					return true;
				});

		byte[] transClass = basicClass;
		transClass = transform(transClass, forMethod(updateSig, onUpdateAction));
		transClass = transform(transClass, forMethod(clearPistonTileEntitySig, setPistonBlockAction));
		transClass = transform(transClass, forMethod(updateSig, setPistonBlockAction));
		
		return transClass;
	}

	private static byte[] transformTileEntityPistonRenderer(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("renderStateModel", "func_188186_a", "a", "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/world/World;Z)Z");

		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					for(int i = 1; i <= 4; i++)
						newInstructions.add(new VarInsnNode(Opcodes.ALOAD, i));
					newInstructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "renderPistonBlock", "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/world/World;Z)Z", false));
					newInstructions.add(new InsnNode(Opcodes.IRETURN));

					method.instructions = newInstructions;
					return true;
				})));
	}

	private static byte[] transformWorldServer(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("areAllPlayersAsleep", "func_73056_e", "g", "()Z");

		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "isEveryoneAsleep", "(Lnet/minecraft/world/World;)I", false));
					newInstructions.add(new InsnNode(Opcodes.DUP));
					LabelNode label = new LabelNode();
					newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, label));
					newInstructions.add(new InsnNode(Opcodes.ICONST_1));
					newInstructions.add(new InsnNode(Opcodes.ISUB));
					newInstructions.add(new InsnNode(Opcodes.IRETURN));
					newInstructions.add(label);

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
	}

	private static byte[] transformBlockModelRenderer(byte[] basicClass) {
		MethodSignature sig1 = new MethodSignature("renderQuadsFlat", "func_187496_a", "a", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;IZLnet/minecraft/client/renderer/BufferBuilder;Ljava/util/List;Ljava/util/BitSet;)V");

		if(hasOptifine(sig1.toString()))
			return basicClass;

		return transform(basicClass, forMethod(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.INVOKEVIRTUAL && checkDesc(((MethodInsnNode) node).desc, "(DDD)V");
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 6));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 18));
					newInstructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "putColorsFlat", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/block/model/BakedQuad;I)V", false));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
	}

	private static MethodAction layerCountTransformer = combine(
			(AbstractInsnNode node) -> { // Filter
				return node.getOpcode() == Opcodes.BIPUSH && ((IntInsnNode) node).operand == 6;
			},
			(MethodNode method, AbstractInsnNode node) -> { // Action
				InsnList newInstructions = new InsnList();
				newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "getLayerCount", "()I", false));

				method.instructions.insert(node, newInstructions);
				method.instructions.remove(node);
				return true;
			}); 

	private static byte[] transformRecipeAddPattern(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("matches", "func_77569_a", "a", "(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z");
		return transform(basicClass, forMethod(sig, layerCountTransformer));
	}

	private static byte[] transformItemBanner(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("appendHoverTextFromTileEntityTag", "func_185054_a", "a", "(Lnet/minecraft/item/ItemStack;Ljava/util/List;)V");
		return transform(basicClass, forMethod(sig, layerCountTransformer));
	}
	
	private static byte[] transformRender(byte[] basicClass) {
		MethodSignature sig = new MethodSignature("renderEntityOnFire", "func_76977_a", "a", "(Lnet/minecraft/entity/Entity;DDDF)V");

		return transform(basicClass, forMethod(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.DLOAD, 2));
					newInstructions.add(new VarInsnNode(Opcodes.DLOAD, 4));
					newInstructions.add(new VarInsnNode(Opcodes.DLOAD, 6));
					newInstructions.add(new VarInsnNode(Opcodes.FLOAD, 8));	
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "renderFire", "(Lnet/minecraft/entity/Entity;DDDF)Z", false));
					LabelNode label = new LabelNode();
					newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, label));
					newInstructions.add(new InsnNode(Opcodes.RETURN));
					newInstructions.add(label);

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
	}

	// BOILERPLATE BELOW ==========================================================================================================================================

	private static byte[] transform(byte[] basicClass, TransformerAction... methods) {
		ClassReader reader = new ClassReader(basicClass);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		boolean didAnything = false;

		for(TransformerAction pair : methods) {
			log("Applying Transformation to method (" + pair.sig + ")");
			didAnything |= findMethodAndTransform(node, pair.sig, pair.action);
		}

		if(didAnything) {
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		}

		return basicClass;
	}

	public static boolean findMethodAndTransform(ClassNode node, MethodSignature sig, MethodAction pred) {
		String funcName = sig.funcName;
		if(LoadingPlugin.runtimeDeobfEnabled)
			funcName = sig.srgName;

		for(MethodNode method : node.methods) {
			if((method.name.equals(funcName)|| method.name.equals(sig.obfName) || method.name.equals(sig.srgName)) && (method.desc.equals(sig.funcDesc) || method.desc.equals(sig.obfDesc))) {
				log("Located Method, patching...");

				boolean finish = pred.test(method);
				log("Patch result: " + finish);

				return finish;
			}
		}

		log("Failed to locate the method!");
		return false;
	}

	public static MethodAction combine(NodeFilter filter, NodeAction action) {
		return (MethodNode mnode) -> applyOnNode(mnode, filter, action);
	}

	public static boolean applyOnNode(MethodNode method, NodeFilter filter, NodeAction action) {
		Iterator<AbstractInsnNode> iterator = method.instructions.iterator();

		boolean didAny = false;
		while(iterator.hasNext()) {
			AbstractInsnNode anode = iterator.next();
			if(filter.test(anode)) {
				log("Located patch target node " + getNodeString(anode));
				didAny = true;
				if(action.test(method, anode))
					break;
			}
		}

		return didAny;
	}

	private static void log(String str) {
		LogManager.getLogger("Quark ASM").info(str);
	}

	private static String getNodeString(AbstractInsnNode node) {
		Printer printer = new Textifier();

		TraceMethodVisitor visitor = new TraceMethodVisitor(printer);
		node.accept(visitor);

		StringWriter sw = new StringWriter();
		printer.print(new PrintWriter(sw));
		printer.getText().clear();

		return sw.toString().replaceAll("\n", "").trim();
	}

	private static boolean checkDesc(String desc, String expected) {
		return desc.equals(expected);
	}

	private static boolean hasOptifine(String msg) {
		try {
			if(Class.forName("optifine.OptiFineTweaker") != null) {
				log("Optifine Detected. Disabling Patch for " + msg);
				return true;
			}
		} catch (ClassNotFoundException ignored) { }
		return false;
	}

	private static class MethodSignature {
		String funcName, srgName, obfName, funcDesc, obfDesc;

		public MethodSignature(String funcName, String srgName, String obfName, String funcDesc) {
			this.funcName = funcName;
			this.srgName = srgName;
			this.obfName = obfName;
			this.funcDesc = funcDesc;
		}

		@Override
		public String toString() {
			return "Names [" + funcName + ", " + srgName + ", " + obfName + "] Descriptor " + funcDesc + " / " + obfDesc;
		}

	}

	// Basic interface aliases to not have to clutter up the code with generics over and over again
	private interface Transformer extends Function<byte[], byte[]> { }
	private interface MethodAction extends Predicate<MethodNode> { }
	private interface NodeFilter extends Predicate<AbstractInsnNode> { }
	private interface NodeAction extends BiPredicate<MethodNode, AbstractInsnNode> { }

	private static TransformerAction forMethod(MethodSignature sig, MethodAction action) {
		return new TransformerAction(sig, action);
	}

	private final static class TransformerAction {
		private final MethodSignature sig;
		private final MethodAction action;

		public TransformerAction(MethodSignature sig, MethodAction action) {
			this.sig = sig;
			this.action = action;
		}
	}

}
