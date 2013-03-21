package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import mccity.heroes.skills.totem.Totem;
import mccity.heroes.skills.totem.TotemBuilder;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.util.Map;

public class TempestTotemBuilder extends TotemBuilder {

    @Override
    public Totem buildSpecific(Hero hero) {
        return new TempestTotem(getDamage(hero));
    }

    @Override
    public String getName() {
        return "Tempest";
    }

    @Override
    public MaterialData getBaseMaterial() {
        return new MaterialData(Material.LAPIS_BLOCK, (byte) 0);
    }

    @Override
    protected String getShortDescription() {
        return "Strikes your enemies with lightning";
    }

    @Override
    protected void appendToDescription(StringBuilder descr, Hero hero) {
        descr.append("Damage:");
        descr.append(getDamage(hero));
    }

    @Override
    protected void fillSpecificDefaults(Map<String, Object> defaults) {
        defaults.put(SkillSetting.DAMAGE.node(), 5.0);
        defaults.put(SkillSetting.DAMAGE_INCREASE.node(), 0.06);
    }

    private int getDamage(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.DAMAGE), 5.0, false) +
                SkillConfigManager.getUseSetting(hero, skill, subkey(SkillSetting.DAMAGE_INCREASE), 0.06, false) * hero.getSkillLevel(skill));
    }
}
