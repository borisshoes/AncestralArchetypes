package net.borisshoes.ancestralarchetypes;

import net.borisshoes.borislib.datastorage.DataAccess;
import net.borisshoes.borislib.datastorage.DataKey;
import net.borisshoes.borislib.datastorage.DataRegistry;
import net.borisshoes.borislib.datastorage.StorableData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class ArchetypeAbilityStorage implements StorableData {
   
   private final HashMap<SubArchetype, Set<ArchetypeAbility>> storedAbilities = new HashMap<>();
   
   public static final DataKey<ArchetypeAbilityStorage> KEY = DataRegistry.register(DataKey.ofGlobal(Identifier.fromNamespaceAndPath(MOD_ID, "archetype_abilities"), ArchetypeAbilityStorage::new));
   
   private ArchetypeAbilityStorage(){
      initializeDefaults();
   }
   
   private void initializeDefaults(){
      for(SubArchetype subarchetype : ArchetypeRegistry.SUBARCHETYPES){
         storedAbilities.put(subarchetype, subarchetype.getDefaultAbilities());
      }
   }
   
   @Override
   public void read(ValueInput view){
      CompoundTag abilitiesTag = view.read("storedAbilities", CompoundTag.CODEC).orElse(new CompoundTag());
      
      // Clear and reinitialize to defaults first
      storedAbilities.clear();
      initializeDefaults();
      
      for(String subArchetypeId : abilitiesTag.keySet()){
         try{
            SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, subArchetypeId));
            if(subArchetype == null){
               AncestralArchetypes.LOGGER.warn("Unknown subarchetype '{}' in saved ability storage, skipping", subArchetypeId);
               continue;
            }
            
            ListTag abilityList = abilitiesTag.getListOrEmpty(subArchetypeId);
            Set<ArchetypeAbility> abilities = new HashSet<>();
            for(Tag tag : abilityList){
               if(tag instanceof StringTag(String abilityId)){
                  try{
                     ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, abilityId));
                     if(ability != null){
                        abilities.add(ability);
                     }else{
                        AncestralArchetypes.LOGGER.warn("Unknown ability '{}' for subarchetype '{}', skipping", abilityId, subArchetypeId);
                     }
                  }catch(Exception e){
                     AncestralArchetypes.LOGGER.warn("Failed to parse ability '{}' for subarchetype '{}': {}", abilityId, subArchetypeId, e.getMessage());
                  }
               }
            }
            
            if(!abilities.isEmpty()){
               storedAbilities.put(subArchetype, abilities);
            }
         }catch(Exception e){
            AncestralArchetypes.LOGGER.warn("Failed to parse subarchetype entry '{}': {}", subArchetypeId, e.getMessage());
         }
      }
   }
   
   @Override
   public void writeNbt(CompoundTag tag){
      CompoundTag abilitiesTag = new CompoundTag();
      
      for(var entry : storedAbilities.entrySet()){
         SubArchetype subArchetype = entry.getKey();
         Set<ArchetypeAbility> abilities = entry.getValue();
         
         if(subArchetype == null || abilities == null || abilities.isEmpty()){
            continue;
         }
         
         ListTag abilityList = new ListTag();
         for(ArchetypeAbility ability : abilities){
            if(ability != null && ability.id() != null){
               abilityList.add(StringTag.valueOf(ability.id()));
            }
         }
         
         if(!abilityList.isEmpty()){
            abilitiesTag.put(subArchetype.getId(), abilityList);
         }
      }
      
      tag.put("storedAbilities", abilitiesTag);
   }
   
   public static void loadAbilities(MinecraftServer server){
      ArchetypeAbilityStorage storage = DataAccess.getGlobal(KEY);
      for(SubArchetype subarchetype : ArchetypeRegistry.SUBARCHETYPES){
         subarchetype.setAbilities(storage.storedAbilities.get(subarchetype));
         pushUpdateToPlayers(server,subarchetype);
      }
   }
   
   public static boolean addAbility(MinecraftServer server, SubArchetype subArchetype, ArchetypeAbility ability){
      SubArchetype regArch = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,subArchetype.getId()));
      boolean succ = false;
      if(regArch != null){
         ArchetypeAbilityStorage storage = DataAccess.getGlobal(KEY);
         succ = regArch.addAbility(ability);
         storage.storedAbilities.put(subArchetype,regArch.getRawAbilities());
      }
      pushUpdateToPlayers(server,subArchetype);
      return succ;
   }
   
   public static boolean removeAbility(MinecraftServer server, SubArchetype subArchetype, ArchetypeAbility ability){
      SubArchetype regArch = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,subArchetype.getId()));
      boolean succ = false;
      if(regArch != null){
         ArchetypeAbilityStorage storage = DataAccess.getGlobal(KEY);
         succ = regArch.removeAbility(ability);
         storage.storedAbilities.put(subArchetype,regArch.getRawAbilities());
      }
      pushUpdateToPlayers(server,subArchetype);
      return succ;
   }
   
   public static void resetAbilities(MinecraftServer server, SubArchetype subArchetype){
      SubArchetype regArch = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,subArchetype.getId()));
      if(regArch != null){
         ArchetypeAbilityStorage storage = DataAccess.getGlobal(KEY);
         regArch.resetAbilities();
         storage.storedAbilities.put(subArchetype,regArch.getRawAbilities());
      }
      pushUpdateToPlayers(server,subArchetype);
   }
   
   public static void pushUpdateToPlayers(MinecraftServer server, SubArchetype subArchetype){
      for(ServerPlayer player : server.getPlayerList().getPlayers()){
         PlayerArchetypeData data = AncestralArchetypes.profile(player);
         if(!subArchetype.equals(data.getSubArchetype())) continue;
         data.changeArchetype(player,data.getSubArchetype(),true);
      }
   }
}
