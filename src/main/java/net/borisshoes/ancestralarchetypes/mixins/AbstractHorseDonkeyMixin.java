package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

@Mixin({AbstractHorseEntity.class, AbstractDonkeyEntity.class})
public class AbstractHorseDonkeyMixin {
   @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
   private void archetypes$mountDrops(ServerWorld world, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) ci.cancel();
   }
}
