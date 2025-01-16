package net.borisshoes.ancestralarchetypes;

import net.borisshoes.ancestralarchetypes.utils.ConfigUtils;

import java.util.Locale;
import java.util.Objects;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.log;

public class ArchetypeConfig {
   public static int getInt(ArchetypeConfig.ConfigSetting<?> setting){
      try{
         return (int) AncestralArchetypes.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Integer config for "+setting.getName());
         log(3,e.toString());
      }
      return 0;
   }
   
   public static boolean getBoolean(ArchetypeConfig.ConfigSetting<?> setting){
      try{
         return (boolean) AncestralArchetypes.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Boolean config for "+setting.getName());
         log(3,e.toString());
      }
      return false;
   }
   
   public static double getDouble(ArchetypeConfig.ConfigSetting<?> setting){
      try{
         return (double) AncestralArchetypes.CONFIG.getValue(setting.getName());
      }catch(Exception e){
         log(3,"Failed to get Boolean config for "+setting.getName());
         log(3,e.toString());
      }
      return 0;
   }
   
   public record NormalConfigSetting<T>(ConfigUtils.IConfigValue<T> setting) implements ConfigSetting<T>{
      public NormalConfigSetting(ConfigUtils.IConfigValue<T> setting){
         this.setting = Objects.requireNonNull(setting);
      }
      
      public ConfigUtils.IConfigValue<T> makeConfigValue(){
         return setting;
      }
      
      public String getId(){
         return setting.getName().replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
      }
      
      public String getName(){
         return setting.getName();
      }
   }
   
   public interface ConfigSetting<T>{
      ConfigUtils.IConfigValue<T> makeConfigValue();
      String getId();
      String getName();
   }
}
