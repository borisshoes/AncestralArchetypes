package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class CamelSpiritMountItem extends SpiritMountItem{
   public CamelSpiritMountItem(Settings settings){
      super(ArchetypeRegistry.CAMEL_SPIRIT_MOUNT, "\uD83D\uDC2B", settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayerEntity player){
      CamelEntity camel = EntityType.CAMEL.create(player.getEntityWorld(), SpawnReason.MOB_SUMMONED);
      camel.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(60.0f);
      camel.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.15f);
      camel.getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(0.65f);
      camel.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      camel.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(2.125f);
      camel.equipStack(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
      camel.setHealth(60.0f);
      camel.setOwner(player);
      camel.setTame(true);
      return camel;
   }
}
