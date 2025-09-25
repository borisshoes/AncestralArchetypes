package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.arcananovum.items.charms.FeastingCharm;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Pseudo
@Mixin(FeastingCharm.FeastingCharmItem.class)
public class FeastingCharmItemMixin {
   
   @ModifyExpressionValue(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;", ordinal = 0))
   private ItemStack archetypes$arcanaFeastingCharm(ItemStack original, ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot){
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.CARNIVORE) && !original.isIn(ArchetypeRegistry.CARNIVORE_FOODS)){
            return ItemStack.EMPTY;
         }
         if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) || profile.hasAbility(ArchetypeRegistry.IRON_EATER) || profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
            return ItemStack.EMPTY;
         }
      }
      return original;
   }
}
