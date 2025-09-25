package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(Item.class)
public class ItemMixin {
   
   @Inject(method = "finishUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/ConsumableComponent;finishConsumption(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
   private void archetypes$onFinishUsing(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir, @Local ConsumableComponent component){
      if (user instanceof ServerPlayerEntity playerEntity) {
         IArchetypeProfile profile = profile(playerEntity);
         HashMap<Item, Pair<Float,Integer>> map = null;
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
            Pair<Float,Integer> pair = map.get(stack.getItem());
            playerEntity.heal(pair.getLeft() * healthMod);
            world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.5f, MathHelper.nextBetween(playerEntity.getRandom(), 0.9f, 1.0f));
         }
         
         if((profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && stack.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS))
               || (profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && stack.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS))){
            profile.changeDeathReductionSizeLevel(true);
            playerEntity.getWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,playerEntity.getX(), playerEntity.getY()+playerEntity.getHeight()/2, playerEntity.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            playerEntity.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.FUNGUS_SPEED_BOOST) && stack.isOf(Items.WARPED_FUNGUS)){
            profile.fungusBoost();
         }
         
         if(profile.getSubArchetype() == ArchetypeRegistry.PARROT && stack.isOf(Items.COOKIE)){
            playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,100,2,true,true,true),playerEntity);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && stack.contains(DataComponentTypes.POTION_CONTENTS)){
            PotionContentsComponent potions = stack.get(DataComponentTypes.POTION_CONTENTS);
            if(potions.potion().isPresent() && potions.potion().get().equals(Potions.WATER)){
               playerEntity.damage(playerEntity.getWorld(), world.getDamageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_GENERIC_BURN, playerEntity.getSoundCategory(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
            }
         }
      }
   }
}
