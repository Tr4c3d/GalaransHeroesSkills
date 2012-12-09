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
import org.bukkit.entity.*;

import java.util.List;

public class SkillSummonWolfPack extends ActiveSkill {

    public SkillSummonWolfPack(Heroes plugin) {
        super(plugin, "SummonWolfPack");
        setDescription("100% chance to spawn 1 wolf, $1% for 2, and $2% for 3");
        setUsage("/skill wolfpack");
        setArgumentRange(0, 0);
        setIdentifiers("skill wolfpack", "skill summonwolfpack");
        setTypes(SkillType.SUMMON, SkillType.SILENCABLE, SkillType.EARTH);
    }

    public String getDescription(Hero hero) {
        int chance2x = Math.min((int) (100.0 * getChance(hero, 2)), 100);
        int chance3x = Math.min((int) (100.0 * getChance(hero, 3)), 100);
        return getDescription().replace("$1", String.valueOf(chance2x)).replace("$2", String.valueOf(chance3x));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.REAGENT.node(), 352);
        defaultConfig.set(Setting.REAGENT_COST.node(), 2);
        defaultConfig.set("base-chance-2x", 0.75);
        defaultConfig.set("base-chance-3x", 0.5);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 20);
        defaultConfig.set("chance-2x-per-level", 0.0);
        defaultConfig.set("chance-3x-per-level", 0.0);
        defaultConfig.set(Setting.COOLDOWN.node(), 120000);
        defaultConfig.set(Setting.HEALTH.node(), 20);
        return defaultConfig;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double chance2x = getChance(hero, 2);
        double chance3x = getChance(hero, 3);
        int maxDistance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 20, false);
        Block targetBlock = player.getTargetBlock(null, maxDistance).getRelative(BlockFace.UP);
        if (targetBlock == null) {
            return SkillResult.CANCELLED;
        }

        // remove all player's wolves in 100m
        List<Entity> nearEntities = player.getNearbyEntities(100, 100, 100);
        for (Entity nearEntity : nearEntities) {
            if (nearEntity instanceof Wolf) {
                Wolf nearWolf = (Wolf) nearEntity;
                if (nearWolf.isTamed() && nearWolf.getOwner().getName().equals(player.getName())) {
                    nearWolf.remove();
                }
            }
        }

        double roll = Util.nextRand();
        int wolfAmount = 1;
        if (roll < chance2x) wolfAmount++;
        if (roll < chance3x) wolfAmount++;

        int wolfHp = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH, 20, false);
        wolfHp = Math.max(1, Math.min(wolfHp, 20));

        for (int i = 0; i < wolfAmount; i++) {
            // add some xz dispersion
            Location wolfSpawnLoc = targetBlock.getLocation().clone();
            wolfSpawnLoc.add(Util.nextInt(5) - 2, 0, Util.nextInt(5) - 2);

            // correct spawn location to not spawn wolf inside a solid block
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
            wolf.setHealth(wolfHp);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private double getChance(Hero hero, int count) {
        String countString = String.valueOf(count);
        return SkillConfigManager.getUseSetting(hero, this, "base-chance-$x".replace("$", countString), count == 2 ? 0.75 : 0.5, false) +
                SkillConfigManager.getUseSetting(hero, this, "chance-$x-per-level".replace("$", countString), 0.0, false) * hero.getSkillLevel(this);
    }
}
