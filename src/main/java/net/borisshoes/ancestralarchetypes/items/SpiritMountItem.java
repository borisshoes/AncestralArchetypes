package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class SpiritMountItem extends AbilityItem{
   
   public SpiritMountItem(ArchetypeAbility ability, String character, Properties settings){
      super(ability, character, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.LEATHER;
      }else{
         return Items.SADDLE;
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      
      UUID mountId = profile.getMountEntity(this.ability);
      if(player.isShiftKeyDown()){
         if(mountId != null){
            Entity entity = player.level().getEntity(mountId);
            if(entity != null && entity.isAlive()){
               profile.setMountHealth(this.ability, ((LivingEntity)entity).getHealth());
               entity.discard();
               profile.setMountEntity(this.ability, null);
               profile.setAbilityCooldown(ability, 20);
            }
         }
      }else{
         if(mountId != null){
            Entity entity = player.level().getEntity(mountId);
            if(entity != null && entity.isAlive()){
               profile.setMountHealth(this.ability, ((LivingEntity)entity).getHealth());
               entity.discard();
               profile.setMountEntity(this.ability, null);
               profile.setAbilityCooldown(ability, 20);
               return InteractionResult.SUCCESS;
            }
         }
         
         if(player.onGround()){
            spawnMount(player);
         }else{
            player.displayClientMessage(Component.translatable("text.ancestralarchetypes.spirit_mount_in_air").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
            SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
            return InteractionResult.PASS;
         }
      }
      
      return InteractionResult.SUCCESS;
   }
   
   private void spawnMount(ServerPlayer player){
      PlayerArchetypeData profile = profile(player);
      Vec3 summonPos = player.getEyePosition().add(player.getLookAngle().scale(1));
      LivingEntity newMount = getMountEntity(player);
      String customName = profile.getMountName();
      if(customName != null){
         newMount.setCustomName(Component.literal(customName));
         newMount.setCustomNameVisible(true);
      }
      float p = player.getXRot();
      float y = player.getYRot();
      newMount.setPos(summonPos);
      newMount.snapTo(summonPos.x, summonPos.y, summonPos.z, player.getYRot(), player.getXRot());
      player.level().tryAddFreshEntityWithPassengers(newMount);
      if(profile.getMountHealth(this.ability) != 0) newMount.setHealth(profile.getMountHealth(this.ability));
      newMount.addTag(getSpiritMountTag());
      newMount.setYRot(player.getYRot());
      newMount.setXRot(player.getXRot());
      profile.setMountEntity(this.ability,newMount.getUUID());
      player.startRiding(newMount,true,true);
      player.connection.send(new ClientboundPlayerRotationPacket(y,false,p,false));
      SoundUtils.playSongToPlayer(player, SoundEvents.HORSE_GALLOP,0.3f,1);
   }
   
   protected abstract LivingEntity getMountEntity(ServerPlayer player);
   
   public  String getSpiritMountTag(){
      return "$"+MOD_ID+".spirit_mount."+this.ability.id();
   }
}
