package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.characters.skill.Skill;
import mccity.heroes.skills.totem.TargetSelector;
import mccity.heroes.skills.totem.Totem;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

public class TempestTotem extends Totem {

    private final int damage;

    public TempestTotem(int damage) {
        this.damage = damage;
    }

    @Override
    public int totemTick() {
        TargetSelector selector = getTargetSelector();
        LivingEntity target = selector.selectEnemy(selector.enemiesOf(selector.getLivingsInArea()));
        if (target != null) {
            World targetWorld = target.getWorld();
            targetWorld.strikeLightningEffect(target.getLocation());
            targetWorld.strikeLightningEffect(getStructure().getBase().getLocation());

            getSkill().plugin.getDamageManager().addSpellTarget(target, getHero(), getSkill());
            Skill.damageEntity(target, getHero().getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
            return 1;
        }
        return 0;
    }
}
