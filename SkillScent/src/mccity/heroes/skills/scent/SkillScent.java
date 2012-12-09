package mccity.heroes.skills.scent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillScent extends ActiveSkill {

    private String playerDetectedText;
    private String nothingText;

    public SkillScent(Heroes plugin) {
        super(plugin, "Scent");
        setDescription("Try to determine whether there is a player near you.");
        setUsage("/skill scent");
        setArgumentRange(0, 0);
        setIdentifiers("skill scent");
        setTypes(SkillType.ILLUSION);
    }

    @Override
    public void init() {
        super.init();
        playerDetectedText = SkillConfigManager.getRaw(this, "player-detected-text", "You hear someone's footsteps");
        nothingText = SkillConfigManager.getRaw(this, "nothing-text", "Seems, there is nobody nearby");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.COOLDOWN.node(), 60000);
        defaultConfig.set(Setting.MANA.node(), 50);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 30);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.4);
        defaultConfig.set("detect-invis", true);
        defaultConfig.set("detect-sneak", true);
        defaultConfig.set("detect-vanish", false);
        defaultConfig.set("invis-radius-multiplier", 0.2);
        defaultConfig.set("sneak-radius-multiplier", 0.4);
        defaultConfig.set("player-detected-text", "You hear someone's footsteps");
        defaultConfig.set("nothing-text", "Seems, there is nobody nearby");
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 60000, false) / 1000.0;
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

        double distance = getDetectRadiusFor(hero);
        if (distance > 0) {
            descr.append(" Radius:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    public double getDetectRadiusFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 30, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE.node(), 0.4, false) * hero.getSkillLevel(this);
    }

    public boolean isDetectFor(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "detect-" + key, true);
    }

    public double getDetectRadiusMultFor(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, key + "-radius-multiplier", 0.3, false);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player skillUser = hero.getPlayer();
        double maxDistance = getDetectRadiusFor(hero);
        maxDistance = Math.max(1, Math.min(maxDistance, 100));

        List<Entity> nearEntities = skillUser.getNearbyEntities(maxDistance, maxDistance, maxDistance);

        for (Entity nearEntity : nearEntities) {
            if (!(nearEntity instanceof Player)) continue;
            Player nearPlayer = (Player) nearEntity;

            if (nearPlayer.equals(skillUser)) continue;

            Hero nearHero = plugin.getCharacterManager().getHero(nearPlayer);
            if (isDetectable(hero, nearHero)) {
                Messaging.send(skillUser, playerDetectedText);
                return SkillResult.NORMAL;
            }
        }

        Messaging.send(skillUser, nothingText);
        return SkillResult.NORMAL;
    }

    public boolean isDetectable(Hero hero, Hero nearHero) {
        Player player = hero.getPlayer();
        Player nearPlayer = nearHero.getPlayer();
        boolean hasInvisEffect = nearHero.hasEffectType(EffectType.INVIS);

        if (!player.canSee(nearPlayer) && !hasInvisEffect) { // non-heroes invis (vanish)
            return isDetectFor(hero, "vanish");
        } else if (hasInvisEffect) { // heroes invis
            return isDetectFor(hero, "invis") && inRadius(hero.getPlayer(), nearPlayer, getDetectRadiusFor(hero) * getDetectRadiusMultFor(hero, "invis"));
        } else if (nearPlayer.isSneaking()) { // sneak
            return isDetectFor(hero, "sneak") && inRadius(hero.getPlayer(), nearPlayer, getDetectRadiusFor(hero) * getDetectRadiusMultFor(hero, "sneak"));
        } else {
            return true;
        }
    }

    public static boolean inRadius(Entity first, Entity second, double radius) {
        return first.getLocation().distance(second.getLocation()) <= radius;
    }
}
