package mccity.heroes.skills.summonboat;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

public class SkillSummonBoat extends ActiveSkill {

    private String cantSummonHere;

    public SkillSummonBoat(Heroes plugin) {
        super(plugin, "SummonBoat");
        setDescription("Summons a boat.");
        setUsage("/skill summonboat");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonboat");
        setTypes(SkillType.SUMMON, SkillType.KNOWLEDGE);
    }

    @Override
    public void init() {
        super.init();
        cantSummonHere = SkillConfigManager.getRaw(this, "cant-summon-here", "Can't summon boat here");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.COOLDOWN.node(), 3600000);
        defaultConfig.set(Setting.MANA.node(), 50);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 5);
        defaultConfig.set("cant-summon-here-text", "Can't summon boat here");
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 3600000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 50, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        double distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 5, false);
        if (distance > 0) {
            descr.append(" Dist:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int maxDistance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 5, false);
        Block targetBlock = player.getTargetBlock(null, maxDistance);
        if (targetBlock != null && (targetBlock.getType() == Material.WATER || targetBlock.getType() == Material.STATIONARY_WATER)) {
            Location spawnLoc = targetBlock.getLocation().add(0.5, 1, 0.5);
            targetBlock.getWorld().spawn(spawnLoc, Boat.class);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, cantSummonHere);
            return SkillResult.FAIL;
        }
    }
}
