package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.ArchetypeParticles;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.GUARDIAN_RAY;

public class GuardianRayItem extends AbilityItem{
   public GuardianRayItem(Settings settings){
      super(GUARDIAN_RAY, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.PRISMARINE_CRYSTALS;
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
      player.setCurrentHand(hand);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayerEntity player)) return;
      int useTime = this.getMaxUseTime(stack, user) - remainingUseTicks;
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      int duration = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_DURATION);
      
      MinecraftUtils.LasercastResult lasercast = MinecraftUtils.lasercast(world, player.getEyePos(), player.getRotationVecClient(), 25, false, player);
      ArchetypeParticles.guardianRay(player.getWorld(),lasercast.startPos().subtract(0,player.getHeight()/3,0),lasercast.endPos(), useTime);
      
      if(useTime < windup){ // Windup
         if(useTime % 5 == 0) SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS,0.3f, 0.5f + 1.2f*((float) useTime / windup));
      }else if(useTime < (windup+duration)){ // Shoot
         if(useTime == windup) {
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND, SoundCategory.PLAYERS,1.2f, 0.8f);
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS,1.2f, 1.2f);
         }
         
         float damage = (float) CONFIG.getDouble(ArchetypeRegistry.GUARDIAN_RAY_DAMAGE);
         if(useTime % 15 == 0){
            for(Entity hit : lasercast.sortedHits()){
               hit.damage(player.getWorld(), player.getDamageSources().indirectMagic(player,player), damage);
            }
         }
         
         if(useTime % 20 == 0){
            SoundUtils.playSound(player.getWorld(), player.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.2f, 1.2f);
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND, SoundCategory.PLAYERS,0.75f, 0.7f);
         }
      }else{ // Reset
         player.stopUsingItem();
      }
   }
   
   @Override
   public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      int useTime = this.getMaxUseTime(stack, user) - remainingUseTicks;
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      int duration = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_DURATION);
      int cooldown = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_COOLDOWN);
      
      if(useTime > windup){
         profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double) (useTime - windup) / duration))));
      }
      return false;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack){
      return UseAction.BOW;
   }
   
   @Override
   public int getMaxUseTime(ItemStack stack, LivingEntity user){
      return 72000;
   }
}
