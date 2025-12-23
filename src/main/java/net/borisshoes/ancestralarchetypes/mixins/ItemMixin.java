package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.callbacks.WaxShieldCallback;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

@Mixin(Item.class)
public class ItemMixin {
   
   @Inject(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/Consumable;onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
   private void archetypes$onFinishUsing(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir, @Local Consumable component){
      if (user instanceof ServerPlayer playerEntity) {
         PlayerArchetypeData profile = profile(playerEntity);
         HashMap<Item, Tuple<Float,Integer>> map = null;
         float healthMod = 1.0f;
         
         if(ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())){
            if(profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
               map = ArchetypeRegistry.TUFF_FOODS;
               healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.TUFF_FOOD_HEALTH_MODIFIER);
            }else{
               cir.cancel();
               return;
            }
         }
         if(ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())){
            if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER)){
               map = ArchetypeRegistry.COPPER_FOODS;
               healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.COPPER_FOOD_HEALTH_MODIFIER);
            }else{
               cir.cancel();
               return;
            }
         }
         if(ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())){
            if(profile.hasAbility(ArchetypeRegistry.IRON_EATER)){
               map = ArchetypeRegistry.IRON_FOODS;
               healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.IRON_FOOD_HEALTH_MODIFIER);
            }else{
               cir.cancel();
               return;
            }
         }
         
         if(map != null){
            Tuple<Float,Integer> pair = map.get(stack.getItem());
            playerEntity.heal(pair.getA() * healthMod);
            world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.5f, Mth.randomBetween(playerEntity.getRandom(), 0.9f, 1.0f));
         }
         
         if((profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && stack.is(ArchetypeRegistry.SLIME_GROW_ITEMS))
               || (profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && stack.is(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS))){
            profile.changeDeathReductionSizeLevel(playerEntity,true);
            playerEntity.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,playerEntity.getX(), playerEntity.getY()+playerEntity.getBbHeight()/2, playerEntity.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            playerEntity.makeSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.FUNGUS_SPEED_BOOST) && stack.is(Items.WARPED_FUNGUS)){
            profile.fungusBoost();
         }
         
         if(profile.hasAbility(ArchetypeRegistry.WAX_SHIELD) && stack.is(Items.HONEYCOMB)){
            double maxAbs = CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_MAX_HEALTH);
            float curAbs = playerEntity.getAbsorptionAmount();
            float addedAbs = (float) Math.min(maxAbs,CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_HEALTH));
            int duration = CONFIG.getInt(ArchetypeRegistry.WAX_SHIELD_DURATION);
            BorisLib.addTickTimerCallback(new WaxShieldCallback(duration,playerEntity,addedAbs));
            SoundUtils.playSongToPlayer(playerEntity, SoundEvents.AXE_WAX_OFF, 0.5f, 1.8f);
            MinecraftUtils.addMaxAbsorption(playerEntity, Identifier.fromNamespaceAndPath(MOD_ID,ArchetypeRegistry.WAX_SHIELD.id()),addedAbs);
            playerEntity.setAbsorptionAmount((curAbs + addedAbs));
         }
         
         if(profile.getSubArchetype() == ArchetypeRegistry.PARROT && stack.is(Items.COOKIE)){
            playerEntity.addEffect(new MobEffectInstance(MobEffects.POISON,100,2,true,true,true),playerEntity);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && stack.has(DataComponents.POTION_CONTENTS) && !playerEntity.hasEffect(MobEffects.WATER_BREATHING)){
            PotionContents potions = stack.get(DataComponents.POTION_CONTENTS);
            if(potions.potion().isPresent() && potions.potion().get().equals(Potions.WATER)){
               playerEntity.hurtServer(playerEntity.level(), world.damageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.GENERIC_BURN, playerEntity.getSoundSource(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
            }
         }
      }
   }
}
