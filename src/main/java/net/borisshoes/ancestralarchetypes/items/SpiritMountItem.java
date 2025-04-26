package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class SpiritMountItem extends AbilityItem{
   
   public SpiritMountItem(ArchetypeAbility ability, Settings settings){
      super(ability, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.ARMADILLO_SCUTE;
      }else{
         return Items.SADDLE;
      }
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      UUID mountId = profile.getMountEntity(this.ability);
      if(player.isSneaking()){
         if(mountId != null){
            Entity entity = player.getServerWorld().getEntity(mountId);
            if(entity != null && entity.isAlive()){
               profile.setMountHealth(this.ability, ((LivingEntity)entity).getHealth());
               entity.discard();
               profile.setMountEntity(this.ability, null);
               profile.setAbilityCooldown(ability, 20);
            }
         }
      }else{
         if(mountId != null){
            Entity entity = player.getServerWorld().getEntity(mountId);
            if(entity != null && entity.isAlive()){
               profile.setMountHealth(this.ability, ((LivingEntity)entity).getHealth());
               entity.discard();
               profile.setMountEntity(this.ability, null);
               profile.setAbilityCooldown(ability, 20);
               return ActionResult.SUCCESS;
            }
         }
         
         if(player.isOnGround()){
            spawnMount(player);
         }else{
            player.sendMessage(Text.translatable("text.ancestralarchetypes.spirit_mount_in_air").formatted(Formatting.RED,Formatting.ITALIC),true);
            SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
            return ActionResult.PASS;
         }
      }
      
      return ActionResult.SUCCESS;
   }
   
   private void spawnMount(ServerPlayerEntity player){
      IArchetypeProfile profile = profile(player);
      Vec3d summonPos = player.getEyePos().add(player.getRotationVector().multiply(1));
      LivingEntity newMount = getMountEntity(player);
      String customName = profile.getMountName();
      if(customName != null){
         newMount.setCustomName(Text.literal(customName));
         newMount.setCustomNameVisible(true);
      }
      newMount.setPosition(summonPos);
      newMount.refreshPositionAndAngles(summonPos.x, summonPos.y, summonPos.z, player.getYaw(), player.getPitch());
      player.getServerWorld().spawnNewEntityAndPassengers(newMount);
      if(profile.getMountHealth(this.ability) != 0) newMount.setHealth(profile.getMountHealth(this.ability));
      newMount.addCommandTag(getSpiritMountTag());
      profile.setMountEntity(this.ability,newMount.getUuid());
      player.startRiding(newMount,true);
      SoundUtils.playSongToPlayer(player,SoundEvents.ENTITY_HORSE_GALLOP,0.3f,1);
   }
   
   protected abstract LivingEntity getMountEntity(ServerPlayerEntity player);
   
   public  String getSpiritMountTag(){
      return "$"+MOD_ID+".spirit_mount."+this.ability.getId();
   }
}
