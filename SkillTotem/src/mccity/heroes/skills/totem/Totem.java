package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.party.HeroParty;
import me.galaran.bukkitutils.skilltotem.PlayersAroundChecker;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Totem {

    private static final int MAX_ACTIVE_DISTANCE = 50;
    private static PlayersAroundChecker distanceChecker = null;

    private TargetSelector targetSelector;

    private Hero hero;
    private TotemStructure structure;
    private World world;
    private boolean protect;
    private double radius;

    private long nextTickAt;
    private int tickInterval;
    private long deadTick;

    private int charges;

    void setBaseParams(Hero hero, TotemStructure structure, boolean protect, double radius, int tickIntervalMs,
                       int maxLifetimeMs, int maxCharges) {
        this.hero = hero;
        this.structure = structure;
        world = structure.getBase().getWorld();
        this.protect = protect;
        this.radius = radius;
        tickInterval = tickIntervalMs / 50;
        nextTickAt = 0;
        deadTick = world.getFullTime() + maxLifetimeMs / 50;
        charges = maxCharges;

        targetSelector = new TargetSelector(hero, radius, structure.getBase());
    }

    protected SkillTotem getSkill() {
        return SkillTotem.ref;
    }

    protected double getRadius() {
        return radius;
    }

    protected int getTickInterval() {
        return tickInterval;
    }

    public TotemStructure getStructure() {
        return structure;
    }

    public Hero getHero() {
        return hero;
    }

    void breakStructure() {
        structure.destroy(getSkill().isDropBaseBlock(hero));
    }

    boolean isProtected() {
        return protect;
    }

    protected final TargetSelector getTargetSelector() {
        return targetSelector;
    }

    /**
     * @return How many charges totem used on this tick
     */
    public abstract int totemTick();

    boolean onTick() {
        long curTick = world.getFullTime();
        if (curTick >= nextTickAt) {
            if (curTick >= deadTick) return false;
            if (!hero.getPlayer().isOnline()) return false;
            if (!isAnyPlayerNearby()) return false;
            if (!structure.check()) return false;
            nextTickAt = curTick + tickInterval;

            charges -= totemTick();
            if (charges <= 0) return false;
        }
        return true;
    }

    private boolean isAnyPlayerNearby() {
        if (distanceChecker == null) {
            distanceChecker = new PlayersAroundChecker(getSkill().plugin, 100);
        }
        return distanceChecker.isPlayerNearby(structure.getBase().getLocation(), MAX_ACTIVE_DISTANCE);
    }
}
