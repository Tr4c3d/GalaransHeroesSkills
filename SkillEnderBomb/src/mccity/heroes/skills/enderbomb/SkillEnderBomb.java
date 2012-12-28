package mccity.heroes.skills.enderbomb;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class SkillEnderBomb extends ActiveSkill {

    private final Map<EnderPearl, Hero> enderbombMap = new WeakHashMap<EnderPearl, Hero>();
    private final List<String> enderbombThrowers = new ArrayList<String>();

    public SkillEnderBomb(Heroes plugin) {
        super(plugin, "EnderBomb");
        setDescription("Throw enderbomb, which deals $1 + explosion damage. Explosion radius: $2.");
        setUsage("/skill enderbomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbomb");
        setTypes(SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FIRE);

        Bukkit.getPluginManager().registerEvents(new SkillEnderBombListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.REAGENT.node(), Material.ENDER_PEARL.getId());
        node.set(Setting.REAGENT_COST.node(), 1);
        node.set(Setting.DAMAGE.node(), 15.0);
        node.set(Setting.DAMAGE_INCREASE.node(), 0.3);
        node.set("explosion-radius", 3.0);
        node.set("fire", false);
        node.set("prevent-wilderness-block-damage-and-fire", true);
        node.set("damage-friendly", false);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription()
                .replace("$1", String.valueOf(getDamage(hero)))
                .replace("$2", Util.stringDouble(getExplosionRadius(hero)))
        );
        
        if (!isDamageFriendly(hero)) {
            descr.append(" No friendly damage.");
        }

        int cd = getCooldown(hero);
        if (cd > 0) {
            descr.append(" CD:");
            descr.append(Util.stringDouble(cd / 1000.0));
            descr.append("s");
        }

        int mana = getMana(hero);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        return descr.toString();
    }

    public int getDamage(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 15.0, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.3, false) * hero.getSkillLevel(this));
    }

    public float getExplosionRadius(Hero hero) {
        return (float) SkillConfigManager.getUseSetting(hero, this, "explosion-radius", 3.0, false);
    }

    public boolean isFire(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "fire", false);
    }

    public boolean isNoGrief(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "prevent-wilderness-block-damage-and-fire", true);
    }
    
    public boolean isDamageFriendly(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "damage-friendly", false);
    }

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return (int) Math.max(0.0, SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 0.0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE, 0.0, false) * hero.getSkillLevel(this));
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        EnderPearl enderBomb = player.launchProjectile(EnderPearl.class);
        enderbombMap.put(enderBomb, hero);
        enderbombThrowers.add(player.getName());

        return SkillResult.NORMAL;
    }

    public class SkillEnderBombListener implements Listener {

        /*
         * Ender bomb explosion detector
         * onProjectileHit, onEntityExplode and onEntityDamage calls in the same tick
         * onEntityExplode and onEntityDamage are close to each other
         */
        private long currentTick = 0;
        private Location currentLandingLoc;

        private Hero currentThrower;

        // enderpearl teleport fires before onProjectileHit, check if player throwed enderbomb earlier
        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                if (enderbombThrowers.contains(event.getPlayer().getName())) {
                    event.setCancelled(true);
                }
            }
        }

        // fires before onProjectileHit
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
            if (event.getRemover() instanceof Player) {
                Player remover = (Player) event.getRemover();
                if (enderbombThrowers.contains(remover.getName())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (event.getEntityType() == EntityType.ENDER_PEARL) {
                EnderPearl enderPearl = (EnderPearl) event.getEntity();
                Hero thrower = enderbombMap.remove(enderPearl);

                if (thrower != null) {
                    if (thrower.getPlayer().isOnline()) {
                        currentThrower = thrower;
                        currentTick = getFirstWorldTime();
                        currentLandingLoc = enderPearl.getLocation();

                        enderPearl.getWorld().createExplosion(currentLandingLoc, getExplosionRadius(thrower), isFire(thrower));
                    }
                    enderbombThrowers.remove(thrower.getPlayer().getName());
                    enderPearl.remove();
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityExplode(EntityExplodeEvent event) {
            if (event.getEntity() == null && getFirstWorldTime() == currentTick &&
                    currentLandingLoc.distanceSquared(event.getLocation()) < 4) { // it's enderbomb!

                // force explosion, even if protection plugin canceled it, but without block damage
                if (isNoGrief(currentThrower) || event.isCancelled()) {
                    event.blockList().clear();
                    event.setYield(0.0F);
                }
                event.setCancelled(false);
            }
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;
            if (getFirstWorldTime() != currentTick) return;

            if (!(event.getEntity() instanceof LivingEntity)) {
                event.setCancelled(true); // do not damage items, etc
                return;
            }
            LivingEntity target = (LivingEntity) event.getEntity();
            
            if (currentLandingLoc.distanceSquared(target.getLocation()) > 625) return; // not current enderbomb explosion
            
            if (isDamageFriendly(currentThrower) || !isFriendlyTo(target, currentThrower)) {
                damageEntity(target, currentThrower.getPlayer(), getDamage(currentThrower), EntityDamageEvent.DamageCause.MAGIC);
            } else {
                event.setCancelled(true);
            }
        }

        private long getFirstWorldTime() {
            return Bukkit.getWorlds().get(0).getFullTime();
        }

        private boolean isFriendlyTo(LivingEntity target, Hero hero) {
            Player player = hero.getPlayer();

            // self
            if (target.equals(player)) return true;
            Set<String> friendlyNames = new HashSet<String>();
            friendlyNames.add(player.getName());

            // party
            HeroParty party = hero.getParty();
            if (party != null) {
                if (target instanceof Player && party.isPartyMember((Player) target)) {
                    return true;
                } else {
                    for (Hero partyMember : party.getMembers()) {
                        friendlyNames.add(partyMember.getName());
                    }
                }
            }

            // pet
            if (target instanceof Tameable) {
                Tameable tameable = (Tameable) target;
                if (tameable.isTamed() && friendlyNames.contains(tameable.getOwner().getName())) return true;
            }

            return false;
        }
    }
}
