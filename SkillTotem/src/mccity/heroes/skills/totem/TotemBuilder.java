package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import me.galaran.bukkitutils.skilltotem.GUtils;
import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TotemBuilder {

    protected final SkillTotem skill = SkillTotem.ref;

    public Totem buildFor(Hero hero, TotemStructure structure) {
        Totem totem = buildSpecific(hero);
        totem.setBaseParams(hero, structure, isProtected(hero), getRadius(hero), getTickInterval(hero),
                getLifetime(hero), isLimitedCharges(hero) ? getMaxCharges(hero) : Integer.MAX_VALUE);
        return totem;
    }

    public abstract Totem buildSpecific(Hero hero);

    public abstract String getName();

    public abstract MaterialData getBaseMaterial();

    protected abstract String getShortDescription();

    protected abstract void appendToDescription(StringBuilder descr, Hero hero);

    protected abstract void fillSpecificDefaults(Map<String, Object> defaults);

    public final Map<String, ?> getDefaultConfig() {
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("can-use", true);
        defaults.put(SkillSetting.COOLDOWN.node(), 60000);
        defaults.put(SkillSetting.COOLDOWN_REDUCE.node(), 600);
        defaults.put(SkillSetting.RADIUS.node(), 7.0);
        defaults.put(SkillSetting.RADIUS_INCREASE.node(), 0.16);
        defaults.put(SkillSetting.MANA.node(), 50.0);
        defaults.put(SkillSetting.MANA_REDUCE.node(), 0.5);
        defaults.put("lifetime", 60000);
        defaults.put("lifetime-increase", 1000);
        defaults.put("protection", true);
        defaults.put("tick-interval", 3000);
        defaults.put("limited-charges", true);
        defaults.put("max-charges", 10.0);
        defaults.put("max-charges-increase", 0.2);
        fillSpecificDefaults(defaults);
        return defaults;
    }

    public String getDescriptionFor(Hero hero) {
        StringBuilder descr = new StringBuilder();
        descr.append(ChatColor.DARK_PURPLE);
        descr.append(getName());
        descr.append(ChatColor.GRAY);
        descr.append(" (");
        descr.append(ChatColor.BLUE);
        descr.append(GUtils.matDataToStringReadable(getBaseMaterial(), ChatColor.BLUE));
        descr.append(ChatColor.GRAY);
        descr.append(")");
        descr.append(ChatColor.YELLOW);
        descr.append(" - ");
        descr.append(getShortDescription());

        descr.append(" Protection:");
        descr.append(isProtected(hero) ? "Yes" : "No");

        descr.append(" Max_Lifetime:");
        descr.append(getLifetime(hero) / 1000);
        descr.append("s");

        if (isLimitedCharges(hero)) {
            descr.append(" Max_Charges:");
            descr.append(getMaxCharges(hero));
        }

        descr.append(" Radius:");
        descr.append(getRadius(hero));

        descr.append(" Triggers_Every:");
        descr.append(getTickInterval(hero));
        descr.append("ms");

        double cdSec = getCooldown(hero) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = getManaCost(hero);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        descr.append(' ');
        descr.append(ChatColor.YELLOW);
        appendToDescription(descr, hero);
        return descr.toString();
    }

    protected final String subkey(String key) {
        return getName() + "." + key;
    }

    protected final String subkey(SkillSetting setting) {
        return subkey(setting.node());
    }

    boolean isAvailable(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey("can-use"), true);
    }

    int getManaCost(Hero hero) {
        return (int) Math.max(0.0, SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.MANA), 50.0, false) -
                SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.MANA_REDUCE), 0.5, false) * hero.getSkillLevel(skill));
    }

    int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.COOLDOWN), 60000, false) -
                SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.COOLDOWN_REDUCE), 600, false) * hero.getSkillLevel(skill));
    }

    boolean isProtected(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey("protection"), true);
    }

    private double getRadius(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.RADIUS), 7.0, false) +
                SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.RADIUS_INCREASE), 0.16, false) * hero.getSkillLevel(skill);
    }

    private int getTickInterval(Hero hero) {
        return Math.max(250, SkillConfigManager.getUseSetting(hero, skill, subkey("tick-interval"), 3000, false));
    }

    private int getLifetime(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey("lifetime"), 60000, false) +
                SkillConfigManager.getUseSetting(hero, skill, subkey("lifetime-increase"), 1000, false) * hero.getSkillLevel(skill);
    }

    private boolean isLimitedCharges(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey("limited-charges"), true);
    }

    private int getMaxCharges(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, skill, subkey("max-charges"), 10.0, false) +
                SkillConfigManager.getUseSetting(hero, skill, subkey("max-charges-increase"), 0.2, false) * hero.getSkillLevel(skill));
    }
}
