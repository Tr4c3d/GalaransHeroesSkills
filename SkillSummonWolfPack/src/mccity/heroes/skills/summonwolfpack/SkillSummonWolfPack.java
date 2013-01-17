package mccity.heroes.skills.summonwolfpack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class SkillSummonWolfPack extends ActiveSkill {

    public SkillSummonWolfPack(Heroes plugin) {
        super(plugin, "SummonWolfPack");
        setDescription("100% chance to spawn 1 wolf, $1% for 2, and $2% for 3.");
        setUsage("/skill wolfpack");
        setArgumentRange(0, 0);
        setIdentifiers("skill wolfpack", "skill summonwolfpack");
        setTypes(SkillType.SUMMON, SkillType.SILENCABLE, SkillType.EARTH);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 5.0);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.1);
        defaultConfig.set(Setting.REAGENT.node(), 352);
        defaultConfig.set(Setting.REAGENT_COST.node(), 0);
        defaultConfig.set("base-chance-2x", 0.75);
        defaultConfig.set("chance-2x-per-level", 0.0);
        defaultConfig.set("base-chance-3x", 0.5);
        defaultConfig.set("chance-3x-per-level", 0.0);
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription()
                .replace("$1", Util.stringDouble(getChance(hero, 2) * 100))
                .replace("$2", Util.stringDouble(getChance(hero, 3) * 100))
        );

        int cd = getCooldown(hero);
        if (cd > 0) {
            descr.append(" CD:");
            descr.append(Util.stringDouble(cd / 1000.0));
            descr.append("s");
        }

        int mana = getMana(hero);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        descr.append(" MaxDist:");
        descr.append(String.valueOf(getMaxDistance(hero)));
        
        return descr.toString();
    }

    public int getMaxDistance(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 5.0, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE, 0.1, false) * hero.getSkillLevel(this));
    }

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return (int) Math.max(0.0, SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 0.0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE, 0.0, false) * hero.getSkillLevel(this));
    }

    public double getChance(Hero hero, int count) {
        String countString = String.valueOf(count);
        double chance = SkillConfigManager.getUseSetting(hero, this, "base-chance-$x".replace("$", countString), count == 2 ? 0.75 : 0.5, false) +
                SkillConfigManager.getUseSetting(hero, this, "chance-$x-per-level".replace("$", countString), 0.0, false) * hero.getSkillLevel(this);
        return Math.min(chance, 1.0);
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double chance2x = getChance(hero, 2);
        double chance3x = getChance(hero, 3);
        int maxDistance = getMaxDistance(hero);
        Block targetBlock = player.getTargetBlock(null, maxDistance).getRelative(BlockFace.UP);
        if (targetBlock == null) { // y >= 256
            return SkillResult.CANCELLED;
        }

        // remove all player's wolves in 100m
        for (Wolf wolf : player.getWorld().getEntitiesByClass(Wolf.class)) {
            if (wolf.isTamed() && wolf.getOwner().getName().equals(player.getName())
                    && wolf.getLocation().distanceSquared(player.getLocation()) <= 10000) {
                wolf.remove();
            }
        }

        double roll = Util.nextRand();
        int wolfAmount = 1;
        if (roll < chance2x) wolfAmount++;
        if (roll < chance3x) wolfAmount++;

        for (int i = 0; i < wolfAmount; i++) {
            // add some xz dispersion
            Location wolfSpawnLoc = targetBlock.getLocation().clone();
            wolfSpawnLoc.add(Util.nextInt(5) - 2, 0, Util.nextInt(5) - 2);

            // correct spawn location - not spawn inside a block
            int upShifting = 0;
            Block testBlock = wolfSpawnLoc.getBlock();
            while (testBlock.getType() != Material.AIR ||
                    testBlock.getRelative(-1, 0, 0).getType() != Material.AIR ||
                    testBlock.getRelative(0, 0, -1).getType() != Material.AIR ||
                    testBlock.getRelative(-1, 0, -1).getType() != Material.AIR) {
                testBlock = testBlock.getRelative(BlockFace.UP);
                upShifting++;
                if (upShifting > 7) break;
            }
            if (upShifting > 7) {
                continue;
            } else {
                wolfSpawnLoc.add(0, upShifting, 0);
            }

            Wolf wolf = (Wolf) player.getWorld().spawnEntity(wolfSpawnLoc, EntityType.WOLF);
            wolf.setOwner(player);
        }
        
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
