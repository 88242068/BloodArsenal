package arc.bloodarsenal.item.tool;

import WayofTime.bloodmagic.api.event.BoundToolEvent;
import WayofTime.bloodmagic.api.iface.IActivatable;
import WayofTime.bloodmagic.api.iface.IBindable;
import WayofTime.bloodmagic.api.util.helper.*;
import WayofTime.bloodmagic.client.IMeshProvider;
import WayofTime.bloodmagic.registry.ModItems;
import WayofTime.bloodmagic.util.Utils;
import WayofTime.bloodmagic.util.helper.TextHelper;
import arc.bloodarsenal.BloodArsenal;
import arc.bloodarsenal.registry.Constants;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Shameless copying from Blood Magic repo just to change a few lines ¯\_(ツ)_/¯
 * Wait a minute, I wrote half of this...
 */
public abstract class ItemBoundTool extends ItemTool implements IBindable, IActivatable, IMeshProvider
{
    protected final String tooltipBase;
    private final String name;

    public Map<ItemStack, Boolean> heldDownMap = new HashMap<>();
    public Map<ItemStack, Integer> heldDownCountMap = new HashMap<>();

    public final int chargeTime = 30;

    public ItemBoundTool(String name, float damage, Set<Block> effectiveBlocks)
    {
        super(damage, 1, ModItems.BOUND_TOOL_MATERIAL, effectiveBlocks);
        setUnlocalizedName(BloodArsenal.MOD_ID + ".bound." + name);
        setCreativeTab(BloodArsenal.TAB_BLOOD_ARSENAL);

        this.name = name;
        this.tooltipBase = "tooltip.bloodarsenal.bound." + name + ".";
    }

    @Override
    public float getStrVsBlock(ItemStack stack, IBlockState state)
    {
        return getActivated(stack) ? getToolMaterial().getEfficiencyOnProperMaterial() : 1.0F;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems)
    {
        subItems.add(Utils.setUnbreakable(new ItemStack(itemIn)));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        if (Strings.isNullOrEmpty(getOwnerUUID(stack)))
        {
            setActivatedState(stack, false);
            return;
        }

        if (entity instanceof EntityPlayer && getActivated(stack) && isSelected && getBeingHeldDown(stack) && stack == ((EntityPlayer) entity).getActiveItemStack())
        {
            EntityPlayer player = (EntityPlayer) entity;
            setHeldDownCount(stack, Math.min(player.getItemInUseCount(), chargeTime));
        }
        else if (!isSelected)
        {
            setBeingHeldDown(stack, false);
        }

        if (entity instanceof EntityPlayer && getActivated(stack) && world.getTotalWorldTime() % 80 == 0)
            NetworkHelper.getSoulNetwork(getOwnerUUID(stack)).syphonAndDamage((EntityPlayer) entity, 20);
    }

    protected int getHeldDownCount(ItemStack stack)
    {
        if (!heldDownCountMap.containsKey(stack))
            return 0;

        return heldDownCountMap.get(stack);
    }

    protected void setHeldDownCount(ItemStack stack, int count)
    {
        heldDownCountMap.put(stack, count);
    }

    protected boolean getBeingHeldDown(ItemStack stack)
    {
        if (!heldDownMap.containsKey(stack))
            return false;

        return heldDownMap.get(stack);
    }

    protected void setBeingHeldDown(ItemStack stack, boolean heldDown)
    {
        heldDownMap.put(stack, heldDown);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking())
            setActivatedState(stack, !getActivated(stack));

        if (!player.isSneaking() && getActivated(stack))
        {
            BoundToolEvent.Charge event = new BoundToolEvent.Charge(player, stack);
            if (MinecraftForge.EVENT_BUS.post(event))
                return new ActionResult<>(EnumActionResult.FAIL, event.result);

            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return super.onItemRightClick(itemStack, world, player, hand);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft)
    {
        if (entityLiving instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) entityLiving;
            if (!player.isSneaking() && getActivated(stack))
            {
                int i = this.getMaxItemUseDuration(stack) - timeLeft;
                BoundToolEvent.Release event = new BoundToolEvent.Release(player, stack, i);
                if (MinecraftForge.EVENT_BUS.post(event))
                    return;

                i = event.charge;

                onBoundRelease(stack, worldIn, player, Math.min(i, chargeTime));
                setBeingHeldDown(stack, false);
            }
        }
    }

    protected void onBoundRelease(ItemStack stack, World world, EntityPlayer player, int charge)
    {

    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entityLiving)
    {
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BOW;
    }

    @Override
    public int getItemEnchantability()
    {
        return 50;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)
    {
        if (TextHelper.canTranslate(tooltipBase + "desc"))
            tooltip.add(TextHelper.localizeEffect(tooltipBase + "desc"));

        tooltip.add(TextHelper.localize("tooltip.BloodMagic." + (getActivated(stack) ? "activated" : "deactivated")));

        if (!stack.hasTagCompound())
            return;

        if (!Strings.isNullOrEmpty(getOwnerUUID(stack)))
            tooltip.add(TextHelper.localizeEffect("tooltip.BloodMagic.currentOwner", PlayerHelper.getUsernameFromStack(stack)));

        super.addInformation(stack, player, tooltip, advanced);
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack)
    {
        return ImmutableSet.of(name);
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot)
    {
        return ArrayListMultimap.create(); // No-op
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        return getActivated(stack) && getBeingHeldDown(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        return ((double) -Math.min(getHeldDownCount(stack), chargeTime) / chargeTime) + 1;
    }

    // IBindable

    @Override
    public String getOwnerName(ItemStack stack)
    {
        return stack != null ? NBTHelper.checkNBT(stack).getTagCompound().getString(Constants.NBT.OWNER_NAME) : null;
    }

    @Override
    public String getOwnerUUID(ItemStack stack)
    {
        return stack != null ? NBTHelper.checkNBT(stack).getTagCompound().getString(Constants.NBT.OWNER_UUID) : null;
    }

    @Override
    public boolean onBind(EntityPlayer player, ItemStack stack)
    {
        return true;
    }

    // IActivatable

    @Override
    public boolean getActivated(ItemStack stack)
    {
        return stack != null && NBTHelper.checkNBT(stack).getTagCompound().getBoolean(Constants.NBT.ACTIVATED);
    }

    @Override
    public ItemStack setActivatedState(ItemStack stack, boolean activated)
    {
        if (stack != null)
        {
            NBTHelper.checkNBT(stack).getTagCompound().setBoolean(Constants.NBT.ACTIVATED, activated);
            return stack;
        }

        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getCustomLocation()
    {
        return null;
    }

    @Override
    public List<String> getVariants()
    {
        List<String> ret = new ArrayList<>();
        ret.add("active=true");
        ret.add("active=false");
        return ret;
    }
}
