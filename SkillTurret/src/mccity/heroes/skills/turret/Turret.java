package mccity.heroes.skills.turret;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import mccity.heroes.skills.turret.integration.PluginsIntegration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.*;

public class Turret {

    private static final float Y_OFFSET_OVER_BASE_BLOCK = 1.6f;
    private static final float RADIUS = 25;
    private static final int ATTACK_TARGET_FOR_TICKS = 80;
    private static final int FIND_TARGET_EVERY_TICKS = 30;

    private final String ownerName;
    private Hero owner; // null when owner is offline
    private final Block baseBlock;
    private final Location loc;
    private final World world;

    private LivingEntity target = null;
    private long startAttackCurrentTargetTick = 0;
    private long lastFindTick = 0;

    private final float damage;
    private final boolean fireArrow;
    private final int attackPeriodTicks;
    private long lastAttackTick = 0;

    private final boolean unlimitedAmmo;
    private int ammo;
    private int ammoPerArrow;

    private final boolean persistent;
    private long endLifeTick;
    private boolean destroyOnOwnerLogout;
    private boolean destroyOnChunkUnload;
    private final boolean lifetimeProtection;

    private boolean targetingNpc = true;
    private boolean targetingPlayer = true;
    private boolean targetingMonster = true;
    private boolean targetingAnimal = true;

    public Turret(Hero owner, Block base, SkillTurret skill) {
        ownerName = owner.getPlayer().getName();
        this.owner = owner;
        this.baseBlock = base;
        this.loc = base.getLocation().add(0.5, Y_OFFSET_OVER_BASE_BLOCK, 0.5);
        this.world = loc.getWorld();

        attackPeriodTicks = SkillConfigManager.getUseSetting(owner, skill, "power.attack-period-ticks", 20, true);
        this.damage = SkillConfigManager.getUseSetting(owner, skill, "power.damage", 3, true);
        this.fireArrow = SkillConfigManager.getUseSetting(owner, skill, "power.fire-arrow", false);
        unlimitedAmmo = skill.isUnlimitedAmmoFor(owner);
        if (!unlimitedAmmo) {
            ammo = skill.getInitialAmmoFor(owner);
            ammoPerArrow = skill.getAmmoPerArrowFor(owner);
        }

        persistent = skill.isPersistentFor(owner);
        if (!persistent) {
            endLifeTick = world.getFullTime() + skill.getTurretLifetimeFor(owner) / 50;
            destroyOnOwnerLogout = SkillConfigManager.getUseSetting(owner, skill, "lifetime.destroy-on-owner-logout", true);
            destroyOnChunkUnload = SkillConfigManager.getUseSetting(owner, skill, "lifetime.destroy-on-chunk-unload", true);
        }
        lifetimeProtection = skill.isLifetimeProtectedFor(owner);

        targetingNpc = SkillConfigManager.getUseSetting(owner, skill, "targeting.npc", true);
        targetingPlayer = SkillConfigManager.getUseSetting(owner, skill, "targeting.player", true);
        targetingMonster = SkillConfigManager.getUseSetting(owner, skill, "targeting.monster", true);
        targetingAnimal = SkillConfigManager.getUseSetting(owner, skill, "targeting.animal", true);
    }

    public Turret(Map<?, ?> turretData) {
        ownerName = (String) turretData.get("owner-name");
        owner = null;

        Location baseBlockLoc = Utils.deserializeLocation((Map<?, ?>) turretData.get("base-block-loc"));
        loc = baseBlockLoc.clone().add(0.5, Y_OFFSET_OVER_BASE_BLOCK, 0.5);
        world = baseBlockLoc.getWorld();
        baseBlock = world.getBlockAt(baseBlockLoc);

        attackPeriodTicks = ((Number) turretData.get("attack-period-ticks")).intValue();
        damage = ((Number) turretData.get("damage")).floatValue();
        fireArrow = (Boolean) turretData.get("fire-arrow");

        unlimitedAmmo = (Boolean) turretData.get("unlimited-ammo");
        if (!unlimitedAmmo) {
            ammo = ((Number) turretData.get("ammo")).intValue();
            ammoPerArrow = ((Number) turretData.get("ammo-per-arrow")).intValue();
        }

        persistent = true;
        if (turretData.containsKey("lifetime-protection")) {
            lifetimeProtection = (Boolean) turretData.get("lifetime-protection");
        } else {
            lifetimeProtection = true;
        }

        Map<?, ?> targeting = (Map<?, ?>) turretData.get("targeting");
        if (targeting != null) {
            targetingNpc = (Boolean) targeting.get("npc");
            targetingPlayer = (Boolean) targeting.get("player");
            targetingMonster = (Boolean) targeting.get("monster");
            targetingAnimal = (Boolean) targeting.get("animal");
        }
    }

    public Map<String, Object> getDataEntry() {
        if (!persistent) return null;

        Map<String, Object> dataEntry = new LinkedHashMap<String, Object>();

        dataEntry.put("owner-name", ownerName);
        dataEntry.put("base-block-loc", Utils.serializeLocation(baseBlock.getLocation()));

        dataEntry.put("attack-period-ticks", attackPeriodTicks);
        dataEntry.put("damage", damage);
        dataEntry.put("fire-arrow", fireArrow);

        dataEntry.put("unlimited-ammo", unlimitedAmmo);
        if (!unlimitedAmmo) {
            dataEntry.put("ammo", ammo);
            dataEntry.put("ammo-per-arrow", ammoPerArrow);
        }

        dataEntry.put("lifetime-protection", lifetimeProtection);

        Map<String, Object> targeting = new LinkedHashMap<String, Object>();
        targeting.put("npc", targetingNpc);
        targeting.put("player", targetingPlayer);
        targeting.put("monster", targetingMonster);
        targeting.put("animal", targetingAnimal);
        dataEntry.put("targeting", targeting);

        return dataEntry;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void AITick() {
        long curTick = world.getFullTime();

        if (!unlimitedAmmo && ammo <= 0) {
            if (!tryLoadAmmo()) {
                world.playEffect(loc, Effect.SMOKE, 4);
                return;
            }
        }

        if (target != null && (target.isDead() || curTick >= startAttackCurrentTargetTick + ATTACK_TARGET_FOR_TICKS)) {
            target = null;
        }
        if (target == null && curTick >= lastFindTick + FIND_TARGET_EVERY_TICKS) {
            lastFindTick = curTick;
            target = findNewTarget();
            if (target != null) {
                startAttackCurrentTargetTick = curTick;
            }
        }

        if (target != null && (unlimitedAmmo || ammo > 0) && curTick >= lastAttackTick + attackPeriodTicks) {
            shootTarget();
            lastAttackTick = curTick;
            if (!unlimitedAmmo) {
                ammo--;
                if (ammo == 0) {
                    if (!tryLoadAmmo()) {
                        Utils.safeSend(ownerName, Messages.outOfAmmo);
                    }
                }
            }
        }
    }

    private boolean tryLoadAmmo() {
        BlockState baseState = baseBlock.getState();
        if (baseState instanceof Dispenser) {
            Dispenser dispenser = (Dispenser) baseState;
            if (Utils.tryConsumeItem(dispenser.getInventory(), Material.ARROW)) {
                ammo += ammoPerArrow;
                return true;
            }
        }
        return false;
    }

    private LivingEntity findNewTarget() {
        Entity dummy = world.spawnEntity(loc, EntityType.ENDER_SIGNAL);
        List<Entity> nearEntities = dummy.getNearbyEntities(RADIUS, RADIUS, RADIUS);
        dummy.remove();
        
        world.playEffect(loc, Effect.ENDER_SIGNAL, 0);

        List<LivingEntity> possiblyTargets = new ArrayList<LivingEntity>();
        Set<String> friendlyPlayerNames = new HashSet<String>();
        for (Entity nearEntity : nearEntities) {
            if (!(nearEntity instanceof LivingEntity)) continue;
            LivingEntity nearLiving = (LivingEntity) nearEntity;

            if (nearLiving.getHealth() <= 0) continue; // dead

            if (nearLiving instanceof Player) {
                if (!targetingPlayer) continue;
                Player nearPlayer = (Player) nearLiving;

                if (!canBeTarget(nearPlayer)) {
                    friendlyPlayerNames.add(nearPlayer.getName());
                    continue;
                }
            } else if (nearLiving instanceof HumanEntity || nearLiving instanceof NPC) {
                if (!targetingNpc) continue;
            } else if (nearLiving instanceof Monster || nearLiving instanceof EnderDragon) {
                if (!targetingMonster) continue;
            } else if (nearLiving instanceof Animals || nearLiving instanceof Squid) {
                if (!targetingAnimal) continue;
            }

            if (!Utils.isInLineOfSight(loc, nearLiving.getLocation().add(0, nearLiving.getEyeHeight(), 0))) continue;

            possiblyTargets.add(nearLiving);
        }

        return selectTarget(possiblyTargets, friendlyPlayerNames);
    }

    private boolean canBeTarget(Player player) {
        if (player.getName().equals(ownerName)) return false; // owner
        if (player.getGameMode() == GameMode.CREATIVE) return false; // creative

        if (owner != null) { // owner-online checks
            if (!owner.getPlayer().canSee(player)) return false;

            HeroParty party = owner.getParty();
            if (party != null && party.isPartyMember(player)) return false; // party

            if (!Skill.damageCheck(owner.getPlayer(), player)) return false; // can not attack
        }
        if (Utils.getHero(player).hasEffectType(EffectType.INVIS)) return false; // vanish

        if (PluginsIntegration.instance.isFriendly(ownerName, player)) return false;

        return true;
    }

    private LivingEntity selectTarget(List<LivingEntity> targets, Set<String> friendlyPlayerNames) {
        if (targets.isEmpty()) return null;

        int maxWeightIndex = -1;
        int maxWeight = 0;

        for (int curIndex = 0; curIndex < targets.size(); curIndex++) {
            LivingEntity target = targets.get(curIndex);
            int curWeight = 0;

            // skip friend's pets
            if (target instanceof Tameable) {
                Tameable pet = (Tameable) target;
                if (pet.getOwner() != null && friendlyPlayerNames.contains(pet.getOwner().getName())) {
                    continue;
                }
            }

            // distance modifier
            double distanceMod = loc.distance(target.getLocation());
            distanceMod = Math.min(Math.max(distanceMod, 1), 24);
            curWeight += (25 - (int) distanceMod);

            // player modifier
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                curWeight += 20;
                if (targetPlayer.isSneaking()) curWeight -= 27; // sneaking
                if (owner != null && owner.isInCombatWith(targetPlayer)) curWeight += 30; // enemy!
            }

            if (curWeight > maxWeight) {
                maxWeight = curWeight;
                maxWeightIndex = curIndex;
            }
        }

        return (maxWeightIndex == -1) ? null : targets.get(maxWeightIndex);
    }

    private void shootTarget() {
        Location tLoc = target.getLocation();
        double vecX = tLoc.getX() - loc.getX();
        double vecY = tLoc.getY() - loc.getY() + target.getEyeHeight() - 0.7;
        double vecZ = tLoc.getZ() - loc.getZ();
        double vecXZ = Math.sqrt(vecX * vecX + vecZ * vecZ);
        float yaw = (float) (Math.atan2(vecZ, vecX) * 180.0 / Math.PI) - 90f;
        float pitch = (float) (-(Math.atan2(vecY, vecXZ) * 180.0 / Math.PI));
        
        Location arrowLoc = loc.clone();
        // CraftBukkit bug, actually Yaw and Pitch swapped in spawnArrow()! 21.03.2013
        arrowLoc.setPitch(yaw);
        arrowLoc.setYaw(pitch);
        
        float distanceMod = (float) vecXZ * 0.2f;
        Vector shootVec = new Vector(vecX, vecY + distanceMod, vecZ);
        
        Arrow arrow = world.spawnArrow(arrowLoc, shootVec, 1.6f, 12f);
        world.playEffect(loc, Effect.BOW_FIRE, 0);

        ReflectionUtils.setArrowDamage(arrow, damage);
        ReflectionUtils.setNotPickupable(arrow);
        if (fireArrow) {
            arrow.setFireTicks(200);
        }
    }

    public void ownerJoined(Hero owner) {
        this.owner = owner;
    }

    public void ownerQuited() {
        this.owner = null;
    }

    public boolean isAlive() {
        if (!persistent) {
            if (world.getFullTime() >= endLifeTick) return false;

            if (destroyOnChunkUnload && !isChunkLoaded()) return false;

            if (owner == null) { // owner offline
                if (destroyOnOwnerLogout) return false;
            } else {
                if (!owner.canUseSkill("Turret")) return false;
            }
        }
        return true;
    }

    public Block getBaseBlock() {
        return baseBlock;
    }

    /** Forces turret chunk to load */
    public boolean checkBaseBlock() {
        return baseBlock.getType() == Material.DISPENSER;
    }

    public boolean isChunkLoaded() {
        return baseBlock.getChunk().isLoaded();
    }

    public boolean isProtected() {
        return lifetimeProtection;
    }
}
