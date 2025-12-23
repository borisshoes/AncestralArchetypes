package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class AbilityItem extends Item implements PolymerItem {
   
   public final ArchetypeAbility ability;
   public final String textCharacter;
   
   public AbilityItem(ArchetypeAbility ability, String character, Properties settings){
      super(settings.setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID,ability.id()))));
      this.ability = ability;
      this.textCharacter = character;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      boolean dontDestroy = false;
      
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(this.ability)){
            dontDestroy = true;
            
            for(int i = 0; i < player.getInventory().getContainerSize(); i++){
               ItemStack other = player.getInventory().getItem(i);
               if(other.getItem() instanceof AbilityItem abilityItem && abilityItem.ability == this.ability && !other.equals(stack)){
                  player.getInventory().setItem(i, ItemStack.EMPTY);
               }
            }
            
            int cooldown = profile.getAbilityCooldown(this.ability);
            if(cooldown > 0){
               if(!player.getCooldowns().isOnCooldown(stack) || player.level().getServer().getTickCount() % 20 == 0){
                  player.getCooldowns().addCooldown(stack,100000000);
               }
               if((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && player.level().getServer().getTickCount() % 2 == 0){
                  StringBuilder builder = new StringBuilder(textCharacter+" ");
                  int value = (int) ((1.0-profile.getAbilityCooldownPercent(this.ability)) * 100);
                  char[] unicodeChars = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
                  for (int i = 0; i < 10; i++) {
                     int segmentValue = value - (i * 10);
                     if (segmentValue <= 0) {
                        builder.append(unicodeChars[0]);
                     } else if (segmentValue >= 10) {
                        builder.append(unicodeChars[unicodeChars.length - 1]);
                     } else {
                        int charIndex = (int) ((double) segmentValue / 10 * (unicodeChars.length - 1));
                        builder.append(unicodeChars[charIndex]);
                     }
                  }
                  builder.append(" ").append(textCharacter);
                  player.displayClientMessage(Component.literal(builder.toString()).withColor(profile.getSubArchetype().getColor()),true);
               }
            }else{
               if(player.getCooldowns().isOnCooldown(stack)){
                  player.getCooldowns().addCooldown(stack,0);
               }
            }
         }
      }
      
      if(!dontDestroy){
         stack.setCount(0);
      }
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
       if(PolymerResourcePackUtils.hasMainPack(context)){
          return Identifier.fromNamespaceAndPath(MOD_ID,this.ability.id());
       }else{
          return BuiltInRegistries.ITEM.getResourceKey(getPolymerItem(stack,context)).get().identifier();
       }
   }
}
