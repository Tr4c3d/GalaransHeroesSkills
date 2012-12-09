package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import mccity.heroes.skills.totem.Totem;
import mccity.heroes.skills.totem.TotemBuilder;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.util.Map;

public class SlowdownTotemBuilder extends TotemBuilder {

    @Override
    public Totem buildSpecific(Hero hero) {
        return new SlowdownTotem(getSlowEffectLevel(hero));
    }

    @Override
    public String getName() {
        return "Slowdown";
    }

    @Override
    public MaterialData getBaseMaterial() {
        return new MaterialData(Material.SOUL_SAND, (byte) 0);
    }

    @Override
    protected String getShortDescription() {
        return "Slows your enemies in totem radius";
    }

    @Override
    protected void appendToDescription(StringBuilder descr, Hero hero) {
        descr.append("SlowEffectLevel:");
        descr.append(getSlowEffectLevel(hero));
    }

    @Override
    protected void fillSpecificDefaults(Map<String, Object> defaults) {
        defaults.put("slow-effect-level", 1);
    }

    private int getSlowEffectLevel(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, subkey("slow-effect-level"), 1, false);
    }
}
