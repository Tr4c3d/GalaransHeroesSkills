package mccity.heroes.skills.enderbomb;

import com.herocraftonline.heroes.characters.Hero;

public class EnderBomb {

    private final Hero hero;
    private final float radius;
    private final boolean fire;
    private final double entityDamageMultiplier;
    private final boolean preventWildDamage;

    public EnderBomb(Hero hero, float radius, boolean fire, boolean preventWildDamage, double entityDamageMultiplier) {
        this.radius = radius;
        this.hero = hero;
        this.fire = fire;
        this.preventWildDamage = preventWildDamage;
        this.entityDamageMultiplier = entityDamageMultiplier;
    }

    public Hero getHero() {
        return hero;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isFire() {
        return fire;
    }

    public double getEntityDamageMultiplier() {
        return entityDamageMultiplier;
    }

    public boolean isPreventWildDamage() {
        return preventWildDamage;
    }
}
