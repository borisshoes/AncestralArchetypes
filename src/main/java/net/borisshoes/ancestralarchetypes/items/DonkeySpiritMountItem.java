package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;

public class DonkeySpiritMountItem  extends SpiritMountItem{
   public DonkeySpiritMountItem(Settings settings){
      super(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT, settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayerEntity player){
      DonkeyEntity donkey = EntityType.DONKEY.create(player.getServerWorld(), SpawnReason.MOB_SUMMONED);
      donkey.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(60.0f);
      donkey.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.25f);
      donkey.getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(0.85f);
      donkey.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      donkey.saddle(new ItemStack(Items.SADDLE), SoundCategory.NEUTRAL);
      donkey.setHealth(60.0f);
      donkey.setOwnerUuid(player.getUuid());
      donkey.setTame(true);
      donkey.setHasChest(true);
      return donkey;
   }
}