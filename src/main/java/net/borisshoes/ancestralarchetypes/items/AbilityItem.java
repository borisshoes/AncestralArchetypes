package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class AbilityItem extends Item implements PolymerItem {
   
   public final ArchetypeAbility ability;
   public final String textCharacter;
   
   public AbilityItem(ArchetypeAbility ability, String character, Settings settings){
      super(settings.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,ability.getId()))));
      this.ability = ability;
      this.textCharacter = character;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot){
      boolean dontDestroy = false;
      
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(this.ability)){
            dontDestroy = true;
            
            for(int i = 0; i < player.getInventory().size(); i++){
               ItemStack other = player.getInventory().getStack(i);
               if(other.getItem() instanceof AbilityItem abilityItem && abilityItem.ability == this.ability && !other.equals(stack)){
                  player.getInventory().setStack(i,ItemStack.EMPTY);
               }
            }
            
            int cooldown = profile.getAbilityCooldown(this.ability);
            if(cooldown > 0){
               if(!player.getItemCooldownManager().isCoolingDown(stack) || player.getServer().getTicks() % 20 == 0){
                  player.getItemCooldownManager().set(stack,100000000);
               }
               if((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && player.getServer().getTicks() % 2 == 0){
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
                  player.sendMessage(Text.literal(builder.toString()).withColor(profile.getSubArchetype().getColor()),true);
               }
            }else{
               if(player.getItemCooldownManager().isCoolingDown(stack)){
                  player.getItemCooldownManager().set(stack,0);
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
      return Identifier.of(MOD_ID,this.ability.getId());
   }
}
