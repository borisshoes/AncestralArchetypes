package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
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
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;

public class HorseSpiritMountItem extends SpiritMountItem{
   public HorseSpiritMountItem(Settings settings){
      super(ArchetypeRegistry.HORSE_SPIRIT_MOUNT, settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayerEntity player){
      IArchetypeProfile profile = profile(player);
      HorseEntity horse = EntityType.HORSE.create(player.getServerWorld(), SpawnReason.MOB_SUMMONED);
      horse.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(60.0f);
      horse.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.35f);
      horse.getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(1.25f);
      horse.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      horse.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(2.0f);
      horse.saddle(new ItemStack(Items.SADDLE), SoundCategory.NEUTRAL);
      ItemStack bodyArmor = new ItemStack(Items.DIAMOND_HORSE_ARMOR);
      bodyArmor.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder()
            .add(EntityAttributes.ARMOR, new EntityAttributeModifier(Identifier.ofVanilla("armor.body"), 15.0, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.BODY)
            .add(EntityAttributes.ARMOR_TOUGHNESS, new EntityAttributeModifier(Identifier.ofVanilla("armor.body"), 5.0F, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.BODY)
            .build());
      bodyArmor.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.BODY).equipSound(SoundEvents.ENTITY_HORSE_ARMOR).model(RegistryKey.of(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.of(MOD_ID,"spirit"))).allowedEntities(EntityType.HORSE).damageOnHurt(false).build());
      bodyArmor.addEnchantment(MiscUtils.getEnchantment(Enchantments.PROTECTION),4);
      horse.equipBodyArmor(bodyArmor);
      horse.setHorseVariant(profile.getHorseColor(), profile.getHorseMarking());
      horse.setHealth(60.0f);
      horse.setOwnerUuid(player.getUuid());
      horse.setTame(true);
      return horse;
   }
}
