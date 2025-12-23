package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class GliderItem extends AbilityItem{
   public final int textColor;
   public final ResourceKey<TrimPattern> trimKey;
   
   public GliderItem(ArchetypeAbility ability, int textColor, Item.Properties settings, ResourceKey<TrimPattern> trimKey){
      super(ability, "\uD83E\uDD9C", settings);
      this.textColor = textColor;
      this.trimKey = trimKey;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.LEATHER_CHESTPLATE;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      double glidePercentage = ((double)profile.getGlideTime() / profile.getMaxGlideTime());
      boolean onCooldown = profile.getAbilityCooldown(this.ability) > 0;
      if(onCooldown){
         stack.setDamageValue(stack.getMaxDamage() - 1);
      }else{
         stack.setDamageValue((int) ((stack.getMaxDamage()-1) * (1-glidePercentage)));
      }
      
      DyedItemColor dyedColorComponent = stack.get(DataComponents.DYED_COLOR);
      if(dyedColorComponent == null || dyedColorComponent.rgb() != profile.getGliderColor()){
         stack.set(DataComponents.DYED_COLOR, new DyedItemColor(profile.getGliderColor()));
         stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR,true));
      }
      
      ArmorTrim armorTrim = stack.get(DataComponents.TRIM);
      Holder<TrimMaterial> material = profile.getGliderTrimMaterial();
      String curId = armorTrim == null ? "" : armorTrim.material().getRegisteredName();
      String neededId = material == null ? "" : material.getRegisteredName();
      if(!curId.equals(neededId)){
         if(neededId.isEmpty()){
            stack.remove(DataComponents.TRIM);
         }else{
            stack.set(DataComponents.TRIM, new ArmorTrim(material, world.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(trimKey).get()));
         }
      }
      
      if(stack.equals(player.getItemBySlot(EquipmentSlot.CHEST)) && player.isFallFlying()){
         int glideValue = (int) (glidePercentage * 100);
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
         player.displayClientMessage(Component.literal(message.toString()).withColor(textColor), true);
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
