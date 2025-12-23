package net.borisshoes.ancestralarchetypes.cca;

import net.minecraft.resources.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class PlayerComponentInitializer implements EntityComponentInitializer {
   public static final ComponentKey<IArchetypeProfile> PLAYER_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(Identifier.fromNamespaceAndPath(MOD_ID, "profile"), IArchetypeProfile.class);
   
   @Override
   public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry){
      registry.registerForPlayers(PLAYER_DATA, ArchetypeProfile::new, RespawnCopyStrategy.ALWAYS_COPY);
   }
}
