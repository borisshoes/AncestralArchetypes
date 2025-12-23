package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
   @Invoker
   void callSetLevel(Level var1);
   
   @Invoker("isInRain")
   boolean rainedOn();
   
   @Accessor("DATA_SHARED_FLAGS_ID")
   static EntityDataAccessor<Byte> getFLAGS(){
      throw new AssertionError();
   }
}
