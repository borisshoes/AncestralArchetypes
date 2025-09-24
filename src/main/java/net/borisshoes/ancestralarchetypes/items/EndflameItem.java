package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.MathUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EndflameItem extends AbilityItem{
   public EndflameItem(Settings settings){
      super(ArchetypeRegistry.ENDERFLAME, "\uD83D\uDD25", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.DRAGON_BREATH;
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
      
      if(player.isSneaking()){
         shootFireball(player);
         profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_FIREBALL_COOLDOWN));
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, user.getStackInHand(hand)));
      }else{
         player.setCurrentHand(hand);
      }
      return ActionResult.SUCCESS;
   }
   
   @Override
   public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayerEntity player)) return;
      
      final double range = 10.0;
      final double closeW = 1.5;
      final double farW = 5.5;
      double mul = 1.5*range;
      Vec3d boxStart = player.getPos().subtract(mul,mul,mul);
      Vec3d boxEnd = player.getPos().add(mul,mul,mul);
      Box rangeBox = new Box(boxStart,boxEnd);
      
      if(remainingUseTicks % 5 == 2){
         SoundUtils.playSound(world, player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 0.6f, (float) (Math.random() * .5 + .5));
         
         List<Entity> entities = player.getWorld().getOtherEntities(player,rangeBox, e -> e instanceof LivingEntity);
         for(Entity e : entities){
            if(!(e instanceof LivingEntity entity)) continue;
            if(MathUtils.inCone(player.getEyePos(),player.getRotationVector(),range,closeW,farW,e.getEyePos())){
               entity.damage(player.getWorld(),player.getDamageSources().indirectMagic(player,player),(float) CONFIG.getDouble(ArchetypeRegistry.ENDERFLAME_BUFFET_DAMAGE));
            }
         }
      }
      
      double mod = 10;
      double percentage = 1.0 - (remainingUseTicks % mod / mod);
      double R = (farW-closeW)*percentage + closeW;
      DustColorTransitionParticleEffect dust = new DustColorTransitionParticleEffect(0xe400ff, 0xa106b8, 1.4f);
      double layerWidth = range / mod;
      
      Vec3d axis = player.getRotationVector();
      Vec3d upRef = Math.abs(axis.y) < 0.999 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
      Vec3d xBasis = axis.crossProduct(upRef).normalize();
      Vec3d zBasis = axis.crossProduct(xBasis).normalize();
      
      for(int i = 0; i < R*R*4; i++){
         double r = R * Math.sqrt(Math.random());
         double theta = Math.random() * 2 * Math.PI;
         Vec3d unRotatedOffset = new Vec3d(r * Math.cos(theta), percentage*range, r * Math.sin(theta));
         Vec3d rotatedOffset = xBasis.multiply(unRotatedOffset.getX()).add(axis.multiply(unRotatedOffset.getY())).add(zBasis.multiply(unRotatedOffset.getZ()));
         Vec3d pos = rotatedOffset.add(player.getEyePos());
         player.getWorld().spawnParticles(dust,pos.getX(),pos.getY(),pos.getZ(),1,0.1,0.1+layerWidth/2.0,0.1,0.01);
      }
   }
   
   @Override
   public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      int cooldown = CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_BUFFET_COOLDOWN);
      profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double)remainingUseTicks/getMaxUseTime(stack,user)))));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return false;
   }
   
   @Override
   public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user){
      if(!(user instanceof ServerPlayerEntity player)) return stack;
      profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_BUFFET_COOLDOWN));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack){
      return UseAction.BOW;
   }
   
   @Override
   public int getMaxUseTime(ItemStack stack, LivingEntity user){
      return 200;
   }
   
   private void shootFireball(ServerPlayerEntity player){
      double errorRad = Math.toRadians(0.25);
      
      double randomAngle = player.getRandom().nextDouble() * 2 * Math.PI;
      double randomTheta = Math.acos(1 - player.getRandom().nextDouble() * (1 - Math.cos(errorRad)));
      
      Vec3d randomOrthogonal = player.getRotationVector().crossProduct(new Vec3d(
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5
      )).normalize();
      
      Vec3d rotatedVector = player.getRotationVector().multiply(Math.cos(randomTheta))
            .add(randomOrthogonal.multiply(Math.sin(randomTheta) * Math.cos(randomAngle)))
            .add(randomOrthogonal.crossProduct(player.getRotationVector()).multiply(Math.sin(randomTheta) * Math.sin(randomAngle)))
            .normalize();
      
      DragonFireballEntity smallFireballEntity = new DragonFireballEntity(player.getWorld(), player, rotatedVector.multiply(1.5));
      smallFireballEntity.setPosition(smallFireballEntity.getX(), player.getBodyY(0.5) + 0.5, smallFireballEntity.getZ());
      player.getWorld().spawnEntity(smallFireballEntity);
      SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS,1f, 1.25f);
   }
}
