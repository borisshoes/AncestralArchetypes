package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.EQUIPMENT_ASSET_REGISTRY_KEY;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class HoverItem extends AbilityItem{
   public HoverItem(Properties settings){
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
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      double hoverPercentage = ((double)profile.getHoverTime() / profile.getMaxHoverTime());
      boolean onCooldown = profile.getAbilityCooldown(this.ability) > 0;
      if(onCooldown){
         stack.setDamageValue(stack.getMaxDamage() - 1);
      }else{
         stack.setDamageValue((int) ((stack.getMaxDamage()-1) * (1-hoverPercentage)));
      }
      
      DyedItemColor dyedColorComponent = stack.get(DataComponents.DYED_COLOR);
      if(dyedColorComponent == null || dyedColorComponent.rgb() != profile.getHelmetColor()){
         stack.set(DataComponents.DYED_COLOR, new DyedItemColor(profile.getHelmetColor()));
         stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR,true));
      }
      
      boolean hovering = !player.isCreative() && VanillaAbilities.ALLOW_FLYING.getTracker(player).isEnabled() &&
            VanillaAbilities.ALLOW_FLYING.getTracker(player).isGrantedBy(SLOW_HOVER_ABILITY) &&
            VanillaAbilities.FLYING.isEnabledFor(player);
      if(stack.equals(player.getItemBySlot(EquipmentSlot.HEAD)) && hovering){
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
         player.displayClientMessage(Component.literal(message.toString()).withColor(ArchetypeRegistry.GHASTLING.getColor()), true);
         stack.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.fromNamespaceAndPath(MOD_ID,"aviator_helmet_on"))).setDamageOnHurt(false).build());
         stack.remove(DataComponents.TRIM);
      }else{
         stack.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.fromNamespaceAndPath(MOD_ID,"aviator_helmet_off"))).setDamageOnHurt(false).build());
         stack.remove(DataComponents.TRIM);
      }
      
      ArmorTrim armorTrim = stack.get(DataComponents.TRIM);
      ResourceKey<TrimPattern> pattern = hovering ? ArchetypeRegistry.HELMET_TRIM_PATTERN_ON : ArchetypeRegistry.HELMET_TRIM_PATTERN_OFF;
      Holder<TrimMaterial> material = profile.getHelmetTrimMaterial();
      String curId = armorTrim == null ? "" : armorTrim.material().getRegisteredName();
      String neededId = material == null ? "" : material.getRegisteredName();
      if(!curId.equals(neededId)){
         if(neededId.isEmpty()){
            stack.remove(DataComponents.TRIM);
         }else{
            stack.set(DataComponents.TRIM, new ArmorTrim(material, world.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(pattern).get()));
         }
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      
      return super.use(world, user, hand);
   }
}
