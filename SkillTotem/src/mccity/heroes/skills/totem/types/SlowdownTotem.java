package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import mccity.heroes.skills.totem.TargetSelector;
import mccity.heroes.skills.totem.Totem;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SlowdownTotem extends Totem {

    private final int slowEffectLevel;

    public SlowdownTotem(int slowEffectLevel) {
        this.slowEffectLevel = slowEffectLevel;
    }

    @Override
    public int totemTick() {
        TargetSelector selector = getTargetSelector();
        List<Player> targets = selector.playersOf(selector.enemiesOf(selector.getLivingsInArea()));

        int affectedTargets = targets.size();
        for (Player target : targets) {
            Hero targetHero = getSkill().plugin.getCharacterManager().getHero(target);
            SlowEffect slow = new SlowEffect(getSkill(), getTickInterval() * 50 * 2, slowEffectLevel - 1, true, null, null, getHero());
            targetHero.addEffect(slow);
        }

        if (affectedTargets > 0) {
            Location effectLoc = getStructure().getBase().getLocation().add(0.5, 2.5, 0.5);
            effectLoc.getWorld().playEffect(effectLoc, Effect.POTION_BREAK, 10);
        }
        return affectedTargets;
    }
}
