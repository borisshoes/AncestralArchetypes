package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CamelSpiritMountItem extends SpiritMountItem{
   public CamelSpiritMountItem(Properties settings){
      super(ArchetypeRegistry.CAMEL_SPIRIT_MOUNT, "\uD83D\uDC2B", settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayer player){
      Camel camel = EntityType.CAMEL.create(player.level(), EntitySpawnReason.MOB_SUMMONED);
      camel.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0f);
      camel.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.15f);
      camel.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.65f);
      camel.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      camel.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2.125f);
      camel.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
      camel.setHealth(60.0f);
      camel.setOwner(player);
      camel.setTamed(true);
      return camel;
   }
}
