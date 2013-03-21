package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import mccity.heroes.skills.totem.Totem;
import mccity.heroes.skills.totem.TotemBuilder;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.util.Map;

public class RecoveryTotemBuilder extends TotemBuilder {

    @Override
    public Totem buildSpecific(Hero hero) {
        return new RecoveryTotem(getHealAmount(hero));
    }

    @Override
    public String getName() {
        return "Recovery";
    }

    @Override
    public MaterialData getBaseMaterial() {
        return new MaterialData(Material.MELON_BLOCK, (byte) 0);
    }

    @Override
    protected String getShortDescription() {
        return "Heals you and your party";
    }

    @Override
    protected void appendToDescription(StringBuilder descr, Hero hero) {
        descr.append("HealPerTick:");
        descr.append(getHealAmount(hero));
        descr.append("hp");
    }

    @Override
    protected void fillSpecificDefaults(Map<String, Object> defaults) {
        defaults.put("heal-amount", 4.0);
        defaults.put("heal-amount-increase", 0.05);
    }

    private int getHealAmount(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, skill, subkey("heal-amount"), 4.0, false) +
                SkillConfigManager.getUseSetting(hero, skill, subkey("heal-amount-increase"), 0.05, false) * hero.getSkillLevel(skill));
    }
}
