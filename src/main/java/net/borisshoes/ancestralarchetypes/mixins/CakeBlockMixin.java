package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {
   
   @ModifyExpressionValue(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"))
   private static boolean archetypes$cakeBlockEat(boolean original, LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Player player){
      if(!original || !(player instanceof ServerPlayer serverPlayer)) return false;
      PlayerArchetypeData data = AncestralArchetypes.profile(player.getUUID());
      if(data.hasAbility(ArchetypeRegistry.COPPER_EATER) || data.hasAbility(ArchetypeRegistry.TUFF_EATER) || data.hasAbility(ArchetypeRegistry.IRON_EATER)){
         serverPlayer.connection.send(new ClientboundSetHealthPacket(serverPlayer.getHealth(), serverPlayer.getFoodData().getFoodLevel(), serverPlayer.getFoodData().getSaturationLevel()));
         return false;
      }else if(data.hasAbility(ArchetypeRegistry.CARNIVORE) && !Items.CAKE.getDefaultInstance().is(ArchetypeRegistry.CARNIVORE_FOODS)){
         serverPlayer.connection.send(new ClientboundSetHealthPacket(serverPlayer.getHealth(), serverPlayer.getFoodData().getFoodLevel(), serverPlayer.getFoodData().getSaturationLevel()));
         return false;
      }
      if(data.hasAbility(ArchetypeRegistry.CHOCOLATE_ALLERGY) && new ItemStack(Items.CAKE).is(ArchetypeRegistry.CHOCOLATE_ALLERGY_FOODS)){
         player.addEffect(new MobEffectInstance(MobEffects.POISON, CONFIG.getInt(ArchetypeRegistry.CHOCOLATE_ALLERGY_DURATION), CONFIG.getInt(ArchetypeRegistry.CHOCOLATE_ALLERGY_AMPLIFIER), true, true, true), player);
      }
      return true;
   }
}
