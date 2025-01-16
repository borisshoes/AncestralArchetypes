# Ancestral Archetypes

A server-sided Origins-style mod where players pick mob-based Archetypes to gain different abilities.
The mod is highly configurable and balanced around a competitive environment. This mod also has full translation support.

##### This mod should only be installed on a server.

## Mod Spotlight /  Tutorial
[![Mod Tutorial](https://img.youtube.com/vi/QApJcu1rpD0/0.jpg)](https://www.youtube.com/watch?v=QApJcu1rpD0)

### Player Commands
* ```/archetypes changeArchetype``` Opens a GUI for a user to change their Archetype.
* ```/archetypes items``` Gives a user the Ability Items for their Archetype, has a 1 minute cooldown.
* ```/archetypes setHorseVariant <color> <markings>``` Sets the color of your Spirit Horse as a Horse Archetype.
* ```/archetypes setGliderColor <hex color>``` Sets the color of your Glider Wings as a Parrot Archetype.
* ```/archetypes toggleReminders``` Toggles the periodic reminders for players that are missing an Archetype or an Ability Item

### Admin Commands
* ```/archetypes resetCooldowns <targets>``` Resets the abilities of the selected players.
* ```/archetypes setArchetype <targets> <archetype>``` Sets the Archetypes of the selected players.
* ```/archetypes addChanges <targets> <changes>``` Gives a number of allowed Archetype changes to the selected players.

### Admin Configuration Commands
The following commands can be used to adjust configurable settings in the AncestralArchetypes.properties file without a server reboot. These commands can be suffixed with "get" or "set" followed by a value to get or set the current setting. 
* ```/archetypes config addedStarveDamage``` The amount of added starvation damage dealt to Gelatians.
* ```/archetypes config biomeDamage``` The amount of damage dealt to Aquarians or Infernals for being in an incompatible biome.
* ```/archetypes config canAlwaysChangeArchetype``` Whether or not players can change their Archetype at will, or use the limited change system.
* ```/archetypes config cauldronDrinkableCooldownModifier``` The time modifier multiplied against the cumulative effect duration of drinkable potions to determine the cooldown of the Witch's active ability.
* ```/archetypes config cauldronInstantEffectCooldown``` The effective duration of instant potion effects for determining the cooldown of the Witch's active ability.
* ```/archetypes config cauldronThrowableCooldownModifier``` The time modifier multiplied against the cumulative effect duration of throwable potions to determine the cooldown of the Witch's active ability.
* ```/archetypes config changesPerChangeItem``` The number of allowed Archetype changes given for consuming a Change Item.
* ```/archetypes config coldDamageModifier``` The modifier multiplied to cold damage dealt to Infernals.
* ```/archetypes config damageStunDuration``` The duration of the movement stun applied to Centaurs.
* ```/archetypes config fallDamageReduction``` The modifier multiplied to fall damage dealt to Ocelots.
* ```/archetypes config fireballCooldown``` The cooldown duration of the Blaze's volley ability.
* ```/archetypes config gliderCooldown``` The cooldown duration of the Parrot's glider ability.
* ```/archetypes config healthSprintCutoff``` The fraction of a Golem's max health that allows them to sprint.
* ```/archetypes config impaleVulnerableModifier``` The modifier multiplied per level of impaling to the damage dealt to an Aquarian.
* ```/archetypes config insatiableHungerRate``` The exhaustion passively given to Gelatians every half-second.
* ```/archetypes config knockbackIncrease``` The modifier multiplied against the knockback dealt to Copper Golems.
* ```/archetypes config knockbackDecrease``` The modifier multiplied against the knockback dealt to Iron Golem.
* ```/archetypes config projectileResistantReduction``` The modifier multiplied against the projectile damage dealt to Golems and Breezes.
* ```/archetypes config regenerationRate``` The amount of health restored to Axolotl's per tick when below half health.
* ```/archetypes config remindersOnByDefault``` Whether or not new players receive reminders about missing an Archetype or Ability Item.
* ```/archetypes config sneakAttackModifier``` The modifier multiplied against the damage dealt to a creature for the first time by an Ocelot.
* ```/archetypes config snowballDamage``` The amount of damage dealt to Infernals by Snowballs.
* ```/archetypes config softhitterDamageReduction``` The modifier multiplied against melee damage dealt by Copper Golems and Breezes.
* ```/archetypes config spiritMountKillCooldown``` The cooldown duration of a Centaur's Spirit Mount when it is killed.
* ```/archetypes config spiritMountRegenerationRate``` The amount of health a Centaur's Spirit Mount heals every 5 seconds.
* ```/archetypes config startingArchetypeChanges``` The number of allowed Archetype changes given to new players.
* ```/archetypes config windChargeCooldown``` The cooldown duration of the Breeze's volley ability.


### LICENSE NOTICE
By using this project in any form, you hereby give your "express assent" for the terms of the license of this project, and acknowledge that I, BorisShoes, have fulfilled my obligation under the license to "make a reasonable effort under the circumstances to obtain the express assent of recipients to the terms of this License.