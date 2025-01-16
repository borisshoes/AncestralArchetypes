package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class AbilityItem extends Item implements PolymerItem {
   
   public final ArchetypeAbility ability;
   
   public AbilityItem(ArchetypeAbility ability, Settings settings){
      super(settings.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,ability.getId()))));
      this.ability = ability;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected){
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
            if(cooldown > 0 && !player.getItemCooldownManager().isCoolingDown(stack)){
               player.getItemCooldownManager().set(stack,cooldown);
            }else if(cooldown <= 0 && player.getItemCooldownManager().isCoolingDown(stack)){
               player.getItemCooldownManager().set(stack,0);
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
