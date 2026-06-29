package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.MathUtils;
import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SONIC_BLAST;

public class SonicBlastItem extends AbilityItem{
   public SonicBlastItem(Properties settings){
      super(SONIC_BLAST, "\uD83D\uDD0A", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.ECHO_SHARD;
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      player.startUsingItem(hand);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayer player)) return;
      int useTime = this.getUseDuration(stack, user) - remainingUseTicks;
      int maxWindup = CONFIG.getInt(ArchetypeRegistry.SONIC_BLAST_CHARGE_DURATION);
      float windup = Mth.clamp((float) useTime / maxWindup,0, 1);
      
      if(windup > 0.25){
         PlayerArchetypeData profile = AncestralArchetypes.profile(player);
         double percentage = (windup-0.25) / 0.75;
         MutableComponent leapMessage = Component.literal("");
         leapMessage.append(Component.literal("\uD83D\uDD0A [").withColor(profile.getSubArchetype().getColor()));
         for(int i = 0; i < 20; i++){
            if(percentage * 20 > i){
               leapMessage.append(Component.literal("|").withColor(profile.getArchetype().color()));
            }else{
               leapMessage.append(Component.literal(".").withColor(profile.getArchetype().color()));
            }
         }
         leapMessage.append(Component.literal("] \uD83D\uDD0A").withColor(profile.getSubArchetype().getColor()));
         player.sendSystemMessage(leapMessage, true);
      }
      if(windup > 0.10){
         if(remainingUseTicks % 15 == 0){
            SoundUtils.playSound(player.level(),player.blockPosition(),SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3.0F, 0.5f*windup+0.625f);
         }
         if(remainingUseTicks % 8 == 0){
            player.level().sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + player.getBbHeight()/2, player.getZ(), 1, 0.3F, 0.3F, 0.3F, 0.0F);
         }
      }
      
      if(windup >= 1){
         player.releaseUsingItem();
      }
   }
   
   @Override
   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayer player)) return false;
      int useTime = this.getUseDuration(stack, user) - remainingUseTicks;
      float maxDamage = CONFIG.getFloat(ArchetypeRegistry.SONIC_BLAST_DAMAGE);
      float maxRange = CONFIG.getFloat(ArchetypeRegistry.SONIC_BLAST_RANGE);
      float maxWidth = CONFIG.getFloat(ArchetypeRegistry.SONIC_BLAST_WIDTH);
      float maxKnockbackMod = CONFIG.getFloat(ArchetypeRegistry.SONIC_BLAST_KNOCKBACK);
      int maxWindup = CONFIG.getInt(ArchetypeRegistry.SONIC_BLAST_CHARGE_DURATION);
      int cooldown = CONFIG.getInt(ArchetypeRegistry.SONIC_BLAST_COOLDOWN);
      
      float windup = Mth.clamp((float) useTime / maxWindup,0, 1);
      if(windup < 0.25) return false;
      float damage = windup * maxDamage;
      float range = Mth.clamp(windup*1.5f,0,1) * maxRange;
      float width = Mth.clamp(windup*2f,0,1) * maxWidth;
      float knockback = 1.5f * (windup > 0.75f ? 1.0f : 0.5f) * maxKnockbackMod;
      
      SoundUtils.playSound(player.level(),player.blockPosition(),SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
      
      ParticleEffectUtils.animatedLightningBolt(player.level(),
            player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(range)),
            (int) range, width / 4.0, ParticleTypes.SONIC_BOOM,3,1,
            0.25,0.0f,false,0,10);
      
      final double closeW = width/2.0;
      final double farW = width;
      double mul = 1.5*range;
      Vec3 boxStart = player.position().subtract(mul,mul,mul);
      Vec3 boxEnd = player.position().add(mul,mul,mul);
      AABB rangeBox = new AABB(boxStart,boxEnd);
      List<Entity> entities = player.level().getEntities(player,rangeBox, e -> e instanceof LivingEntity);
      for(Entity e : entities){
         if(!(e instanceof LivingEntity entity)) continue;
         if(MathUtils.inCone(player.getEyePosition(),player.getLookAngle(),range,closeW,farW,e.getEyePosition())){
            Vec3 source = player.getEyePosition();
            Vec3 delta = entity.getEyePosition().subtract(source);
            Vec3 normalize = delta.normalize();
            
            DamageSource dmgSource = player.damageSources().source(ArchetypeRegistry.SONIC_BOOM,player,player);
            if (entity.hurtServer(player.level(), dmgSource, damage)) {
               double knockbackVertical = (double)0.5F * ((double)1.0F - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) * knockback;
               double knockbackHorizontal = (double)2.5F * ((double)1.0F - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) * knockback;
               
               Vec3 velocity = new Vec3(normalize.x() * knockbackHorizontal, normalize.y() * knockbackVertical, normalize.z() * knockbackHorizontal);
               entity.push(velocity);
            }
         }
      }
      
      profile(player).setAbilityCooldown(this.ability, cooldown);
      return false;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      return ItemUseAnimation.BOW;
   }
   
   @Override
   public int getUseDuration(ItemStack stack, LivingEntity user){
      return 72000;
   }
}
