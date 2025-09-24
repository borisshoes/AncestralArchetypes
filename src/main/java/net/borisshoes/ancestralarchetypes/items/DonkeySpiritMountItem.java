package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class DonkeySpiritMountItem  extends SpiritMountItem{
   public DonkeySpiritMountItem(Settings settings){
      super(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT, "\uD83E\uDECF",settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayerEntity player){
      DonkeyEntity donkey = EntityType.DONKEY.create(player.getWorld(), SpawnReason.MOB_SUMMONED);
      donkey.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(60.0f);
      donkey.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.25f);
      donkey.getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(0.85f);
      donkey.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      donkey.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(1.75f);
      donkey.equipStack(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
      donkey.setHealth(60.0f);
      donkey.setOwner(player);
      donkey.setTame(true);
      donkey.setHasChest(true);
      return donkey;
   }
}