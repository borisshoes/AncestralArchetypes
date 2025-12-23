package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.borislib.gui.GraphicalItem;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin {
   
   @Shadow public abstract void makeSound(@Nullable SoundEvent sound);
   
   @Shadow public abstract void remove(Entity.RemovalReason reason);
   
   @Inject(method= "onEquipItem", at=@At("HEAD"), cancellable = true)
   private void archetypes$equipSoundSpam(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci){
      if(oldStack.getItem() instanceof AbilityItem && newStack.getItem() instanceof AbilityItem){
         ci.cancel();
      }
   }
   
   @ModifyReturnValue(method = "shouldDropExperience", at = @At("RETURN"))
   private boolean archetypes$mountDropsXP(boolean original){
      if(!original) return false;
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) return false;
      return original;
   }
   
   @ModifyReturnValue(method = "shouldDropLoot", at = @At("RETURN"))
   private boolean archetypes$mountDropsLoot(boolean original){
      if(!original) return false;
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) return false;
      return original;
   }
   
   @Inject(method = "remove", at = @At("HEAD"))
   private void archetypes$removeMount(Entity.RemovalReason reason, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(tags.isEmpty()) return;
      
      if(entity instanceof OwnableEntity tameable && tameable.getOwner() instanceof ServerPlayer player && entity.level() instanceof ServerLevel entityWorld){
         PlayerArchetypeData profile = profile(player);
         ArchetypeAbility ability = AncestralArchetypes.abilityFromTag(tags.getFirst());
         
         if(ability != null){
            UUID mountId = profile.getMountEntity(ability);
            if(mountId != null && entity.getUUID().equals(mountId)){
               if(reason == Entity.RemovalReason.KILLED){
                  profile.setMountHealth(ability, 0);
                  profile.setAbilityCooldown(ability, CONFIG.getInt(ArchetypeRegistry.SPIRIT_MOUNT_KILL_COOLDOWN));
               }
               player.displayClientMessage(Component.translatable("text.ancestralarchetypes.spirit_mount_unsummon").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
            }
         }
      }
   }
   
   @Inject(method = "tick", at = @At("HEAD"))
   private void archetypes$tickMount(CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(tags.isEmpty()) return;
      if(entity instanceof Animal animal){
         if(animal.getInLoveTime() > 0){
            animal.setInLoveTime(0);
         }
      }
      
      boolean remove = false;
      block: {
         if(!(entity instanceof OwnableEntity tameable) || !(tameable.getOwner() instanceof ServerPlayer player) || !(entity.level() instanceof ServerLevel entityWorld)){
            remove = true;
            break block;
         }
         
         remove = !player.level().dimension().identifier().equals(entityWorld.dimension().identifier());
         PlayerArchetypeData profile = profile(player);
         ArchetypeAbility ability = AncestralArchetypes.abilityFromTag(tags.getFirst());
         if(!profile.hasAbility(ability)){
            remove = true;
            break block;
         }
         if(entity.getControllingPassenger() != null && !entity.getControllingPassenger().equals(tameable.getOwner())) entity.ejectPassengers();
         List<Entity> passengers = entity.getPassengers();
         for(Entity passenger : passengers){
            if(passenger instanceof ServerPlayer otherPlayer){
               if(otherPlayer.containerMenu instanceof HorseInventoryMenu){
                  otherPlayer.closeContainer();
                  if(tags.getFirst().contains(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT.id())){
                     MountInventoryGui gui = new MountInventoryGui(player, profile.getMountInventory());
                     gui.open();
                  }else{
                     SimpleGui gui = new SimpleGui(MenuType.HOPPER,player,false);
                     for(int i = 0; i < gui.getSize(); i++){
                        gui.setSlot(i, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.BLACK)).hideTooltip());
                     }
                     gui.setTitle(Component.translatable("container.inventory"));
                     gui.open();
                  }
                  
               }
            }
         }
         
         if(ability == null || player.distanceTo(entity) > 64){
            remove = true;
            break block;
         }
         UUID mountId = profile.getMountEntity(ability);
         if(mountId == null || !mountId.equals(entity.getUUID())){
            remove = true;
         }
      }
      
      if(remove){
         entity.discard();
      }else if(entity.tickCount % 100 == 0){
         entity.heal((float) CONFIG.getDouble(ArchetypeRegistry.SPIRIT_MOUNT_REGENERATION_RATE));
      }
   }
   
   @ModifyReturnValue(method = "checkTotemDeathProtection", at = @At("RETURN"))
   private boolean archetypes$deathProtector(boolean original){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(original) return true;
      
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && profile.getDeathReductionSizeLevel() <= 1){
            profile.changeDeathReductionSizeLevel(player,false);
            player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(), player.getY()+player.getBbHeight()/2, player.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            makeSound(SoundEvents.ZOMBIE_VILLAGER_CURE);
            return true;
         }else if(profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && profile.getDeathReductionSizeLevel() <= 1){
            profile.changeDeathReductionSizeLevel(player,false);
            player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(), player.getY()+player.getBbHeight()/2, player.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            makeSound(SoundEvents.ZOMBIE_VILLAGER_CURE);
            return true;
         }
      }
      return original;
   }
   
   @ModifyVariable(method = "knockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private double archetypes$knockback(double strength){
      LivingEntity entity = (LivingEntity) (Object) this;
      
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.INCREASED_KNOCKBACK)){
            return strength * (float) CONFIG.getDouble(ArchetypeRegistry.KNOCKBACK_INCREASE);
         }
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
            return strength * (float) CONFIG.getDouble(ArchetypeRegistry.LIGHTWEIGHT_INCREASED_KNOCKBACK);
         }
         if(profile.hasAbility(ArchetypeRegistry.REDUCED_KNOCKBACK)){
            return strength * (float) CONFIG.getDouble(ArchetypeRegistry.KNOCKBACK_DECREASE);
         }
      }
      return strength;
   }
   
   @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
   private void archetypes$damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.NO_FALL_DAMAGE) && source.is(DamageTypeTags.IS_FALL)){
            cir.setReturnValue(false);
         }else if(profile.hasAbility(ArchetypeRegistry.BOUNCY) && source.is(DamageTypeTags.IS_FALL) && !player.isShiftKeyDown()){
            cir.setReturnValue(false);
         }
      }
   }
   
   @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
   private boolean archetypes$effectImmunity(boolean original, MobEffectInstance effect){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.ANTIVENOM) && effect.is(MobEffects.POISON)){
            return false;
         }else if(profile.hasAbility(ArchetypeRegistry.WITHERING) && effect.is(MobEffects.WITHER)){
            return false;
         }
      }
      return original;
   }
   
   @ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
   private float archetypes$modifyDamage(float original, DamageSource source, float amount){
      float newReturn = original;
      LivingEntity entity = (LivingEntity) (Object) this;
      Entity attacker = source.getEntity();
      
      if(attacker instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         ItemStack weapon = source.getWeaponItem();
         
         if(profile.hasAbility(ArchetypeRegistry.SOFT_HITTER) && source.isDirect()){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.SOFT_HITTER_DAMAGE_REDUCTION);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HARD_HITTER) && source.isDirect()){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.HARD_HITTER_DAMAGE_INCREASE);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.SNEAK_ATTACK)){
            if(entity instanceof ServerPlayer target){
               Vec3 targetEyePos = target.getEyePosition();
               Vec3 targetRot = target.getForward();
               Vec3 attackerEyePos = player.getEyePosition();
               Vec3 eyeDiff = attackerEyePos.subtract(targetEyePos).normalize();
               double thresh = Math.cos(Math.toRadians(60));
               double dp = eyeDiff.dot(targetRot.normalize().reverse());
               boolean behind = dp >= thresh;
               if(behind){
                  newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.PLAYER_SNEAK_ATTACK_MODIFIER);
                  DustColorTransitionOptions particle = new DustColorTransitionOptions(0xee1c1c,0x621313,1.0f);
                  player.level().sendParticles(particle,entity.getX(), entity.getY()+entity.getBbHeight()/2, entity.getZ(), 25, 0.25, 0.25, 0.25, 0.5);
               }
            }else{
               boolean foundAttacker = false;
               for(CombatEntry damageRecord : entity.getCombatTracker().entries){
                  if(player.equals(damageRecord.source().getEntity()) || player.equals(damageRecord.source().getDirectEntity())){
                     foundAttacker = true;
                     break;
                  }
               }
               if(!foundAttacker){
                  newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.MOB_SNEAK_ATTACK_MODIFIER);
                  DustColorTransitionOptions particle = new DustColorTransitionOptions(0xee1c1c,0x621313,1.0f);
                  player.level().sendParticles(particle,entity.getX(), entity.getY()+entity.getBbHeight()/2, entity.getZ(), 25, 0.25, 0.25, 0.25, 0.5);
               }
            }
         }
      }
      
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         ItemStack weapon = source.getWeaponItem();
         
         if(profile.hasAbility(ArchetypeRegistry.IMPALE_VULNERABLE) && weapon != null){
            int impaleLvl = EnchantmentHelper.getItemEnchantmentLevel(MinecraftUtils.getEnchantment(Enchantments.IMPALING), weapon);
            newReturn += (float) CONFIG.getDouble(ArchetypeRegistry.IMPALE_VULNERABLE_MODIFIER) * impaleLvl;
         }
         if(profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && source.is(DamageTypeTags.IS_FREEZING)){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.COLD_DAMAGE_MODIFIER);
         }
         if(profile.hasAbility(ArchetypeRegistry.HALVED_FALL_DAMAGE) && source.is(DamageTypeTags.IS_FALL)){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.FALL_DAMAGE_REDUCTION);
         }
         if(profile.hasAbility(ArchetypeRegistry.PROJECTILE_RESISTANT) && source.is(DamageTypeTags.IS_PROJECTILE)){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.PROJECTILE_RESISTANT_REDUCTION);
         }
         if(profile.hasAbility(ArchetypeRegistry.SLIPPERY) && player.isInWaterOrRain() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)){
               newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.GREAT_SWIMMER_SLIPPERY_DAMAGE_MODIFIER);
            }else{
               newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.SLIPPERY_DAMAGE_MODIFIER);
            }
         }
         if(profile.hasAbility(ArchetypeRegistry.FORTIFY) && profile.isFortifyActive() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            newReturn *= (float) CONFIG.getDouble(ArchetypeRegistry.FORTIFY_DAMAGE_MODIFIER);
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.HEAVY_CORE_STEP, SoundSource.PLAYERS,2f,player.getRandom().nextFloat() + 0.5F);
         }
         if(profile.hasAbility(ArchetypeRegistry.INSATIABLE) && source.is(DamageTypes.STARVE)){
            newReturn += (float) CONFIG.getDouble(ArchetypeRegistry.ADDED_STARVE_DAMAGE);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.STUNNED_BY_DAMAGE) && amount > CONFIG.getDouble(ArchetypeRegistry.STARTLE_MIN_DAMAGE) && !source.is(ArchetypeRegistry.NO_STARTLE)){
            MobEffectInstance slowness = new MobEffectInstance(MobEffects.SLOWNESS, CONFIG.getInt(ArchetypeRegistry.DAMAGE_STUN_DURATION), 9, false, false, true);
            player.addEffect(slowness);
         }
      }
      
      return newReturn;
   }
   
   
   @ModifyReturnValue(method= "getVisibilityPercent", at=@At("RETURN"))
   private double archetypes$attackRangeScale(double original, Entity attacker){
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      if(!CONFIG.getBoolean(ArchetypeRegistry.IGNORED_BY_MOB_TYPE)) return original;
      if(livingEntity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.getSubArchetype() != null && profile.getSubArchetype().getEntityType() != null && profile.getSubArchetype().getEntityType().equals(attacker.getType())){
            return original * 0.01;
         }
      }
      return original;
   }
   
   @ModifyReturnValue(method= "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at=@At("RETURN"))
   private boolean archetypes$canTarget(boolean original, LivingEntity target){
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      if(!CONFIG.getBoolean(ArchetypeRegistry.IGNORED_BY_MOB_TYPE)) return original;
      if(target instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.getSubArchetype() != null && profile.getSubArchetype().getEntityType() != null && profile.getSubArchetype().getEntityType().equals(livingEntity.getType())){
            return false;
         }
      }
      return original;
   }
}
