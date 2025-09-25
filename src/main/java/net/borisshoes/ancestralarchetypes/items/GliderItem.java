package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
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
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class GliderItem extends AbilityItem{
   public final int textColor;
   public final RegistryKey<ArmorTrimPattern> trimKey;
   
   public GliderItem(ArchetypeAbility ability, int textColor, Item.Settings settings, RegistryKey<ArmorTrimPattern> trimKey){
      super(ability, "\uD83E\uDD9C", settings);
      this.textColor = textColor;
      this.trimKey = trimKey;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.LEATHER_CHESTPLATE;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      double glidePercentage = ((double)profile.getGlideTime() / profile.getMaxGlideTime());
      boolean onCooldown = profile.getAbilityCooldown(this.ability) > 0;
      if(onCooldown){
         stack.setDamage(stack.getMaxDamage() - 1);
      }else{
         stack.setDamage((int) ((stack.getMaxDamage()-1) * (1-glidePercentage)));
      }
      
      DyedColorComponent dyedColorComponent = stack.get(DataComponentTypes.DYED_COLOR);
      if(dyedColorComponent == null || dyedColorComponent.rgb() != profile.getGliderColor()){
         stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(profile.getGliderColor()));
         stack.set(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.DYED_COLOR,true));
      }
      
      ArmorTrim armorTrim = stack.get(DataComponentTypes.TRIM);
      RegistryEntry<ArmorTrimMaterial> material = profile.getGliderTrimMaterial();
      String curId = armorTrim == null ? "" : armorTrim.material().getIdAsString();
      String neededId = material == null ? "" : material.getIdAsString();
      if(!curId.equals(neededId)){
         if(neededId.isEmpty()){
            stack.remove(DataComponentTypes.TRIM);
         }else{
            stack.set(DataComponentTypes.TRIM, new ArmorTrim(material, world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN).getOptional(trimKey).get()));
         }
      }
      
      if(stack.equals(player.getEquippedStack(EquipmentSlot.CHEST)) && player.isGliding()){
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
         player.sendMessage(Text.literal(message.toString()).withColor(textColor), true);
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
