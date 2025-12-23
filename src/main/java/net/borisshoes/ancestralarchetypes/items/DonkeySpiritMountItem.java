package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DonkeySpiritMountItem  extends SpiritMountItem{
   public DonkeySpiritMountItem(Properties settings){
      super(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT, "\uD83E\uDECF",settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayer player){
      Donkey donkey = EntityType.DONKEY.create(player.level(), EntitySpawnReason.MOB_SUMMONED);
      donkey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0f);
      donkey.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25f);
      donkey.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.85f);
      donkey.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      donkey.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.75f);
      donkey.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
      donkey.setHealth(60.0f);
      donkey.setOwner(player);
      donkey.setTamed(true);
      donkey.setChest(true);
      return donkey;
   }
}