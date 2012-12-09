package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.Skill;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetSelector {

    private static final int HEIGHT_SCAN_LIMIT = 15;

    private final Hero hero;
    private final double radius;
    private final Block base;

    public TargetSelector(Hero hero, double radius, Block base) {
        this.hero = hero;
        this.radius = radius;
        this.base = base;
    }

    public List<LivingEntity> getLivingsInArea() {
        Snowball testEntity = base.getWorld().spawn(base.getLocation().add(0.5, 0.5, 0.5), Snowball.class);
        List<Entity> areaEntities = testEntity.getNearbyEntities(radius, HEIGHT_SCAN_LIMIT, radius);

        List<LivingEntity> result = new ArrayList<LivingEntity>();
        for (Entity squareAreaEntity : areaEntities) {
            if (squareAreaEntity.isDead()) continue;
            if (!(squareAreaEntity instanceof LivingEntity)) continue;
            LivingEntity curLiving = (LivingEntity) squareAreaEntity;

            if (curLiving.getHealth() <= 0) continue;
            if (testEntity.getLocation().distance(curLiving.getLocation()) > radius) continue;

            result.add(curLiving);
        }
        testEntity.remove();
        return result;
    }

    public List<Player> playersOf(List<LivingEntity> livingList) {
        List<Player> result = new ArrayList<Player>();
        for (LivingEntity livingEntity : livingList) {
            if (livingEntity instanceof Player) {
                result.add((Player) livingEntity);
            }
        }
        return result;
    }

    public List<LivingEntity> friendsOf(List<LivingEntity> livingList) {
        List<LivingEntity> result = new ArrayList<LivingEntity>();

        List<LivingEntity> pets = new ArrayList<LivingEntity>();
        Set<String> friendlyNames = new HashSet<String>();
        friendlyNames.add(hero.getPlayer().getName());

        for (LivingEntity livingEntity : livingList) {
            if (livingEntity instanceof Player) {
                Player player = (Player) livingEntity;
                if (isFriendly(player)) {
                    result.add(player);
                    friendlyNames.add(player.getName());
                }
            } else if (livingEntity instanceof Tameable) {
                Tameable tameable = (Tameable) livingEntity;
                if (tameable.isTamed()) {
                    pets.add(livingEntity);
                } else {
                    result.add(livingEntity);
                }
            } else if (livingEntity instanceof Animals) {
                result.add(livingEntity);
            } else if (livingEntity instanceof HumanEntity || livingEntity instanceof NPC) {
                result.add(livingEntity);
            }
        }

        for (LivingEntity pet : pets) {
            Tameable tamed = (Tameable) pet;
            if (friendlyNames.contains(tamed.getOwner().getName())) {
                result.add(pet);
            }
        }

        return result;
    }

    private boolean isFriendly(Player player) {
        if (player.getName().equals(hero.getPlayer().getName())) return true; // owner

        HeroParty party = hero.getParty();
        if (party != null && party.isPartyMember(player)) return true; // party

        return false;
    }

    public final List<LivingEntity> enemiesOf(List<LivingEntity> livingList) {
        List<LivingEntity> result = new ArrayList<LivingEntity>();

        List<LivingEntity> pets = new ArrayList<LivingEntity>();
        Set<String> enemyNames = new HashSet<String>();

        for (LivingEntity livingEntity : livingList) {
            if (livingEntity instanceof Player) {
                Player player = (Player) livingEntity;
                if (isEnemy(player) && Skill.damageCheck(hero.getPlayer(), player)) {
                    result.add(player);
                    enemyNames.add(player.getName());
                }
            } else if (livingEntity instanceof Monster || livingEntity instanceof EnderDragon) {
                result.add(livingEntity);
            } else if (livingEntity instanceof Tameable) {
                Tameable tameable = (Tameable) livingEntity;
                if (tameable.isTamed()) {
                    pets.add(livingEntity);
                }
            }
        }

        for (LivingEntity pet : pets) {
            Tameable tamed = (Tameable) pet;
            if (enemyNames.contains(tamed.getOwner().getName())) {
                result.add(pet);
            }
        }

        return result;
    }

    private boolean isEnemy(Player player) {
        if (player.getName().equals(hero.getPlayer().getName())) return false; // owner
        if (player.getGameMode() == GameMode.CREATIVE) return false; // creative

        HeroParty party = hero.getParty();
        if (party != null && party.isPartyMember(player)) return false; // party

        // invis
        if (SkillTotem.ref.plugin.getCharacterManager().getHero(player).hasEffectType(EffectType.INVIS)) return false;

        if (!hero.getPlayer().canSee(player)) return false; // vanish

        return true;
    }

    public final LivingEntity selectEnemy(List<LivingEntity> enemies) {
        if (enemies.isEmpty()) return null;

        int maxWeightIndex = -1;
        int maxWeight = 0;

        for (int curIndex = 0; curIndex < enemies.size(); curIndex++) {
            LivingEntity target = enemies.get(curIndex);
            int curWeight = 0;

            // distance modifier
            double distanceMod = base.getLocation().distance(target.getLocation());
            distanceMod = Math.min(Math.max(distanceMod, 1), 24);
            curWeight += (25 - (int) distanceMod);

            // player modifier
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                curWeight += 20;
                if (hero.isInCombatWith(targetPlayer)) curWeight += 30;
            }

            if (curWeight > maxWeight) {
                maxWeight = curWeight;
                maxWeightIndex = curIndex;
            }
        }
        return (maxWeightIndex == -1) ? null : enemies.get(maxWeightIndex);
    }
}
