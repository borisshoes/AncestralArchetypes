package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

@Mixin({AbstractHorse.class, AbstractChestedHorse.class})
public class AbstractHorseDonkeyMixin {
   @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
   private void archetypes$mountDrops(ServerLevel world, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) ci.cancel();
   }
}
