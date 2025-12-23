package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;

public class HorseSpiritMountItem extends SpiritMountItem{
   public HorseSpiritMountItem(Properties settings){
      super(ArchetypeRegistry.HORSE_SPIRIT_MOUNT, "\uD83D\uDC0E",settings);
   }
   
   @Override
   protected LivingEntity getMountEntity(ServerPlayer player){
      PlayerArchetypeData profile = profile(player);
      Horse horse = EntityType.HORSE.create(player.level(), EntitySpawnReason.MOB_SUMMONED);
      horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0f);
      horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35f);
      horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(1.25f);
      horse.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(10.0f);
      horse.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2.25f);
      horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
      ItemStack bodyArmor = new ItemStack(Items.DIAMOND_HORSE_ARMOR);
      bodyArmor.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(Identifier.withDefaultNamespace("armor.body"), 15.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY)
            .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(Identifier.withDefaultNamespace("armor.body"), 5.0F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY)
            .build());
      bodyArmor.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.BODY).setEquipSound(SoundEvents.HORSE_ARMOR).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.fromNamespaceAndPath(MOD_ID,"spirit"))).setAllowedEntities(EntityType.HORSE).setDamageOnHurt(false).build());
      bodyArmor.enchant(MinecraftUtils.getEnchantment(Enchantments.PROTECTION),4);
      horse.setBodyArmorItem(bodyArmor);
      horse.setVariantAndMarkings(profile.getHorseColor(), profile.getHorseMarking());
      horse.setHealth(60.0f);
      horse.setOwner(player);
      horse.setTamed(true);
      return horse;
   }
}
