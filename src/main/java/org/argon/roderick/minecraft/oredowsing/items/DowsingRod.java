package org.argon.roderick.minecraft.oredowsing.items;

import java.util.List;

import org.argon.roderick.minecraft.oredowsing.lib.Reference;
import org.argon.roderick.minecraft.oredowsing.render.DowsingRodRenderer;

import cofh.api.energy.IEnergyContainerItem;
import cofh.lib.inventory.ComparableItemStackSafe;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.StringHelper;
import cpw.mods.fml.common.Optional;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

@Optional.Interface(modid = "CoFHAPI|energy", iface = "cofh.api.energy.IEnergyContainerItem")
public class DowsingRod extends Item implements IEnergyContainerItem
{
    private static final String BASE_NAME       = "DowsingRod";
    private static final int    DAMAGE_PER_USE  = 1;
    private static final float  RENDER_DURATION = 30.0F;
    private static final int    RF_PER_DAMAGE   = 3000;

    private static final String NBT_RADIUS                = "radius";
    private static final String NBT_TARGET_BLOCK_ID       = "block_id";
    private static final String NBT_TARGET_BLOCK_METADATA = "block_metadata";
    private static final String NBT_NUM_UPGRADES          = "num_upgrades";

    private final Block   forcedTargetBlock; // null for any ore
    private final int     baseSquareRadius;
    private final boolean isChargeable;
    private final int     diamondsPerUpgrade;
    private final int     maxUpgrades;
    public  final Object  ingredientBase;
    public  final Object  ingredientTop;

    public DowsingRod(String parNamePrefix,
            Object parIngredientBase, Object parIngredientTop,
            Block parForcedTargetBlock,
            int parMaxDamage, int parSquareRadius, boolean parIsChargeable,
            int parDiamondsPerUpgrade, int parMaxUpgrades)
    {
        super();
        setUnlocalizedName(Reference.MODID + "_" + parNamePrefix + BASE_NAME);
        setTextureName(Reference.MODID + ":" + parNamePrefix + BASE_NAME);
        setMaxStackSize(1);
        setMaxDamage(parMaxDamage);
        setCreativeTab(CreativeTabs.tabTools);

        forcedTargetBlock  = parForcedTargetBlock;
        baseSquareRadius   = parSquareRadius;
        isChargeable       = parIsChargeable;
        diamondsPerUpgrade = parDiamondsPerUpgrade;
        maxUpgrades        = parMaxUpgrades;
        ingredientBase     = parIngredientBase;
        ingredientTop      = parIngredientTop;
    }

    private void initNBT(ItemStack stack)
    {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound= new NBTTagCompound();
        }
        if (forcedTargetBlock != null) {
            forceSetTarget(stack, forcedTargetBlock, 0, null);
        }
        stack.stackTagCompound.setInteger(NBT_RADIUS, baseSquareRadius);
        stack.stackTagCompound.setInteger(NBT_NUM_UPGRADES, 0);
    }

    public boolean addUpgrade(ItemStack stack, int num_upgrades)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }

        int new_num_upgrades = this.getNumUpgrades(stack) + num_upgrades;
        if (new_num_upgrades > maxUpgrades)
            return false;

        stack.stackTagCompound.setInteger(NBT_NUM_UPGRADES, new_num_upgrades);
        stack.stackTagCompound.setInteger(NBT_RADIUS, baseSquareRadius + new_num_upgrades);
        return true;
    }

    public boolean addUpgrade(ItemStack stack)
    {
        return this.addUpgrade(stack, 1);
    }

    public int getDiamondsPerUpgrade()
    {
        return diamondsPerUpgrade;
    }

    public int getMaxUpgrades()
    {
        return maxUpgrades;
    }

    public int getNumUpgrades(ItemStack stack)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }
        return stack.stackTagCompound.getInteger(NBT_NUM_UPGRADES);
    }

    public boolean canUpgrade(ItemStack stack)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }
        return this.getNumUpgrades(stack) < this.getMaxUpgrades();
    }

    public ItemStack getTargetStack(ItemStack stack)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }
        int block_id = stack.stackTagCompound.getInteger(NBT_TARGET_BLOCK_ID);
        int metadata = stack.stackTagCompound.getInteger(NBT_TARGET_BLOCK_METADATA);
        return block_id == 0
                ? null
                : new ItemStack(Block.getBlockById(block_id), 1, metadata);
    }

    private int getSquareRadius(ItemStack stack)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }
        return stack.stackTagCompound.getInteger(NBT_RADIUS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }

        ItemStack target_stack = getTargetStack(stack);
        list.add(String.format(StringHelper.localize("text.oredowsing.tooltip.0"),
                        (target_stack != null ? target_stack.getDisplayName()
                            : StringHelper.localize("text.oredowsing.all_ores"))));
        list.add(String.format(StringHelper.localize("text.oredowsing.tooltip.1"),
                        1+2*getSquareRadius(stack)));
        if (forcedTargetBlock == null) {
            list.add(StringHelper.localize("text.oredowsing.tooltip.2"));
        }
        if (isChargeable) {
            list.add(StringHelper.localize("text.oredowsing.tooltip.3"));
        }
        if (this.getNumUpgrades(stack) < maxUpgrades) {
            list.add(String.format(StringHelper.localize("text.oredowsing.tooltip.4"), diamondsPerUpgrade));
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (StringHelper.isShiftKeyDown()) {
            setTarget(stack, null, 0, world.isRemote ? null : player);
        }
        else {
            divine(stack, world, player);
        }
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        if (StringHelper.isShiftKeyDown()) {
            setTarget(stack, world.getBlock(x, y, z),
                    world.getBlockMetadata(x, y, z),
                    world.isRemote ? null : player);
        }
        else {
            divine(stack, world, player);
        }
        return true;
    }

    public void setTarget(ItemStack stack, Block targetBlock, int metadata, EntityPlayer player)
    {
        if (forcedTargetBlock != null) {
            if (player != null) {
                player.addChatMessage(new ChatComponentText(
                        StringHelper.localize("text.oredowsing.change_target.no")));
            }
            return;
        }
        forceSetTarget(stack, targetBlock, metadata, player);
    }

    private void forceSetTarget(ItemStack stack, Block targetBlock, int metadata, EntityPlayer player)
    {
        if (stack.stackTagCompound == null) {
            initNBT(stack);
        }
        stack.stackTagCompound.setInteger(NBT_TARGET_BLOCK_ID,
                targetBlock == null ? 0 : Block.getIdFromBlock(targetBlock));
        stack.stackTagCompound.setInteger(NBT_TARGET_BLOCK_METADATA, metadata);
        if (player != null) {
            player.addChatMessage(new ChatComponentText(String.format(
                    StringHelper.localize("text.oredowsing.change_target.yes"),
                    (targetBlock == null
                        ? StringHelper.localize("text.oredowsing.all_ores")
                        : new ItemStack(targetBlock, 1, metadata).getDisplayName()))));
        }
    }

    public boolean blockMatches(ComparableItemStackSafe comp_target_stack, ItemStack world_stack)
    {
        return (comp_target_stack != null)
                // detect specific block
                ? comp_target_stack.isItemEqual(new ComparableItemStackSafe(world_stack))
                // detect any ore
                : ItemHelper.isOre(world_stack);
    }

    public void divine(ItemStack stack, World world, EntityPlayer player)
    {
        stack.damageItem(DAMAGE_PER_USE, player);

        if (!world.isRemote)
            return;

        ItemStack target_stack = getTargetStack(stack);
        ComparableItemStackSafe comp_target_stack = (target_stack != null)
                ? (new ComparableItemStackSafe(target_stack))
                : null;
        int r = getSquareRadius(stack);
        int x, y, z;
        for (x = (int)player.posX - r; x <= player.posX + r; x++) {
            for (y = (int)player.posY - r; y <= player.posY + r; y++) {
                for (z = (int)player.posZ - r; z <= player.posZ + r; z++) {
                    if (blockMatches(comp_target_stack,
                                    new ItemStack(world.getBlock(x, y, z), 1,
                                            world.getBlockMetadata(x, y, z)))) {
                        DowsingRodRenderer.addBlockToHighlight(new ChunkCoordinates(x, y, z), world, player, RENDER_DURATION);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------

    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate)
    {
        if (!isChargeable) {
            return 0;
        }

        int cur_damage    = container.getItemDamage();
        int energy_wanted = cur_damage * RF_PER_DAMAGE;
        int energy_taken  = Math.min(energy_wanted, maxReceive);
        int damage_healed = energy_taken / RF_PER_DAMAGE;
        energy_taken = damage_healed * RF_PER_DAMAGE; // adjust for maxReceive % RF_PER_DAMAGE != 0

        if (!simulate) {
            container.setItemDamage(cur_damage - damage_healed);
        }
        //System.out.println("max energy=" + maxReceive
        //      + " energy_taken=" + energy_taken
        //      + " damage_healed=" + damage_healed);
        return energy_taken;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate)
    {
        return 0;
    }

    @Override
    public int getEnergyStored(ItemStack container)
    {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(ItemStack container)
    {
        // Energetic Infuser won't keep offering energy if this is 0
        return isChargeable
                ? container.getItemDamage() * RF_PER_DAMAGE
                : 0;

    }

}
