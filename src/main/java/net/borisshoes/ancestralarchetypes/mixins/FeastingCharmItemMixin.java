package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.arcananovum.items.charms.FeastingCharm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Pseudo
@Mixin(FeastingCharm.FeastingCharmItem.class)
public class FeastingCharmItemMixin {
   
   @ModifyExpressionValue(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
   private ItemStack archetypes$arcanaFeastingCharm(ItemStack original, ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot){
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.CARNIVORE) && !original.is(ArchetypeRegistry.CARNIVORE_FOODS)){
            return ItemStack.EMPTY;
         }
         if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) || profile.hasAbility(ArchetypeRegistry.IRON_EATER) || profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
            return ItemStack.EMPTY;
         }
      }
      return original;
   }
   
   @Inject(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/Consumable;onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
   private void archetypes$eatEffects(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot, CallbackInfo ci){
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.getSubArchetype() == ArchetypeRegistry.PARROT && stack.is(Items.COOKIE)){
            player.addEffect(new MobEffectInstance(MobEffects.POISON,100,2,true,true,true),player);
         }
      }
   }
}
