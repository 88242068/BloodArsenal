package arc.bloodarsenal.item.inventory;

import WayofTime.bloodmagic.api.Constants;
import WayofTime.bloodmagic.api.iface.ISigil;
import WayofTime.bloodmagic.item.inventory.ItemInventory;
import WayofTime.bloodmagic.item.sigil.ItemSigilHolding;
import WayofTime.bloodmagic.util.Utils;
import arc.bloodarsenal.item.sigil.ItemSigilAugmentedHolding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public class InventoryAugmentedHolding extends ItemInventory
{
    protected ItemStack[] inventory;

    public InventoryAugmentedHolding(ItemStack itemStack)
    {
        super(itemStack, ItemSigilAugmentedHolding.inventorySize, "SigilOfAugmentedHolding");
    }

    public void onGuiSaved(EntityPlayer entityPlayer)
    {
        masterStack = findParentStack(entityPlayer);

        if (masterStack != null)
        {
            save();
        }
    }

    public ItemStack findParentStack(EntityPlayer entityPlayer)
    {
        if (Utils.hasUUID(masterStack))
        {
            UUID parentStackUUID = new UUID(masterStack.getTagCompound().getLong(Constants.NBT.MOST_SIG), masterStack.getTagCompound().getLong(Constants.NBT.LEAST_SIG));
            for (int i = 0; i < entityPlayer.inventory.getSizeInventory(); i++)
            {
                ItemStack itemStack = entityPlayer.inventory.getStackInSlot(i);

                if (itemStack != null && Utils.hasUUID(itemStack))
                {
                    if (itemStack.getTagCompound().getLong(Constants.NBT.MOST_SIG) == parentStackUUID.getMostSignificantBits() && itemStack.getTagCompound().getLong(Constants.NBT.LEAST_SIG) == parentStackUUID.getLeastSignificantBits())
                    {
                        return itemStack;
                    }
                }
            }
        }

        return null;
    }

    public void save()
    {
        NBTTagCompound nbtTagCompound = masterStack.getTagCompound();

        if (nbtTagCompound == null)
        {
            nbtTagCompound = new NBTTagCompound();

            UUID uuid = UUID.randomUUID();
            nbtTagCompound.setLong(Constants.NBT.MOST_SIG, uuid.getMostSignificantBits());
            nbtTagCompound.setLong(Constants.NBT.LEAST_SIG, uuid.getLeastSignificantBits());
        }

        writeToNBT(nbtTagCompound);
        masterStack.setTagCompound(nbtTagCompound);
    }

    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemStack)
    {
        return itemStack.getItem() instanceof ISigil && !(itemStack.getItem() instanceof ItemSigilHolding);
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }
}