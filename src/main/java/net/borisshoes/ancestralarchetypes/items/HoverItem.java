package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class HoverItem extends AbilityItem{
   public HoverItem(Settings settings){
      super(ArchetypeRegistry.SLOW_HOVER, "\uD83D\uDE81", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.LEATHER;
      }else{
         return Items.LEATHER_HELMET;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      double hoverPercentage = ((double)profile.getHoverTime() / profile.getMaxHoverTime());
      boolean onCooldown = profile.getAbilityCooldown(this.ability) > 0;
      if(onCooldown){
         stack.setDamage(stack.getMaxDamage() - 1);
      }else{
         stack.setDamage((int) ((stack.getMaxDamage()-1) * (1-hoverPercentage)));
      }
      
      DyedColorComponent dyedColorComponent = stack.get(DataComponentTypes.DYED_COLOR);
      if(dyedColorComponent == null || dyedColorComponent.rgb() != profile.getHelmetColor()){
         stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(profile.getHelmetColor()));
         stack.set(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.DYED_COLOR,true));
      }
      
      boolean hovering = !player.isCreative() && VanillaAbilities.ALLOW_FLYING.getTracker(player).isEnabled() &&
            VanillaAbilities.ALLOW_FLYING.getTracker(player).isGrantedBy(SLOW_HOVER_ABILITY) &&
            VanillaAbilities.FLYING.isEnabledFor(player);
      if(stack.equals(player.getEquippedStack(EquipmentSlot.HEAD)) && hovering){
         int glideValue = (int) (hoverPercentage * 100);
         char[] unicodeChars = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
         StringBuilder message = new StringBuilder("≈ ");
         for (int i = 0; i < 10; i++) {
            int segmentValue = glideValue - (i * 10);
            if (segmentValue <= 0) {
               message.append(unicodeChars[0]);
            } else if (segmentValue >= 10) {
               message.append(unicodeChars[unicodeChars.length - 1]);
            } else {
               int charIndex = (int) ((double) segmentValue / 10 * (unicodeChars.length - 1));
               message.append(unicodeChars[charIndex]);
            }
         }
         message.append(" ≈");
         player.sendMessage(Text.literal(message.toString()).withColor(ArchetypeRegistry.GHASTLING.getColor()), true);
         stack.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.HEAD).equipSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER).model(RegistryKey.of(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.of(MOD_ID,"aviator_helmet_on"))).damageOnHurt(false).build());
         stack.remove(DataComponentTypes.TRIM);
      }else{
         stack.set(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.HEAD).equipSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER).model(RegistryKey.of(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.of(MOD_ID,"aviator_helmet_off"))).damageOnHurt(false).build());
         stack.remove(DataComponentTypes.TRIM);
      }
      
      ArmorTrim armorTrim = stack.get(DataComponentTypes.TRIM);
      RegistryKey<ArmorTrimPattern> pattern = hovering ? ArchetypeRegistry.HELMET_TRIM_PATTERN_ON : ArchetypeRegistry.HELMET_TRIM_PATTERN_OFF;
      RegistryEntry<ArmorTrimMaterial> material = profile.getHelmetTrimMaterial();
      String curId = armorTrim == null ? "" : armorTrim.material().getIdAsString();
      String neededId = material == null ? "" : material.getIdAsString();
      if(!curId.equals(neededId)){
         if(neededId.isEmpty()){
            stack.remove(DataComponentTypes.TRIM);
         }else{
            stack.set(DataComponentTypes.TRIM, new ArmorTrim(material, world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN).getOptional(pattern).get()));
         }
      }
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      return super.use(world, user, hand);
   }
}
