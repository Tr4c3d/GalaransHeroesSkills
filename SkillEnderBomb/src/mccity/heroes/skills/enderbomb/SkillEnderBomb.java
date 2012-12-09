package mccity.heroes.skills.enderbomb;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillEnderBomb extends ActiveSkill {

    private final Map<EnderPearl, EnderBomb> enderBombs = new HashMap<EnderPearl, EnderBomb>();
    private final List<String> playersUsingSkill = new ArrayList<String>();

    private String enderpearlDenyText;

    public SkillEnderBomb(Heroes plugin) {
        super(plugin, "EnderBomb");
        setDescription("Throw enderbomb");
        setUsage("/skill enderbomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbomb");
        setTypes(SkillType.DAMAGING, SkillType.FIRE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEnderBombListener(), plugin);
    }

    @Override
    public void init() {
        super.init();
        enderpearlDenyText = SkillConfigManager.getRaw(this, "enderpearl-deny-text", "Not allowed to use enderpearl");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.REAGENT.node(), 368);
        defaultConfig.set(Setting.REAGENT_COST.node(), 1);
        defaultConfig.set(Setting.HEALTH_COST.node(), 1);
        defaultConfig.set("explosion-radius", 3.0);
        defaultConfig.set("entity-damage-multiplier", 1.5);
        defaultConfig.set("fire", false);
        defaultConfig.set("prevent-wilderness-block-damage-and-fire", true);
        defaultConfig.set("prevent-enderpearl-use", false);
        defaultConfig.set("enderpearl-deny-text", "Not allowed to use enderpearl");
        return defaultConfig;
    }

    public SkillResult use(Hero hero, String[] args) {
        float explosionRadius = (float) SkillConfigManager.getUseSetting(hero, this, "explosion-radius", 3.0, false);
        double entityDamageMultiplier = SkillConfigManager.getUseSetting(hero, this, "entity-damage-multiplier", 1.5, false);
        boolean fire = SkillConfigManager.getUseSetting(hero, this, "fire", false);
        boolean preventWildDamage = SkillConfigManager.getUseSetting(hero, this, "prevent-wilderness-block-damage-and-fire", true);

        Player player = hero.getPlayer();
        EnderPearl enderBomb = player.launchProjectile(EnderPearl.class);
        enderBombs.put(enderBomb, new EnderBomb(hero, explosionRadius, fire, preventWildDamage, entityDamageMultiplier));
        playersUsingSkill.add(player.getName());

        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public boolean isPreventEnderPearlUseFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "prevent-enderpearl-use", false);
    }

    public class SkillEnderBombListener implements Listener {

        /*
         * Ender bomb explosion detector
         * onProjectileHit, onEntityExplode and onEntityDamage executes in the same tick and location +/- for enderbomb hit
         */
        private long enderbombHitTick = 0;
        private Location enderbombLandingLoc;

        private double currentEntityDamageMultiplier = 1.0;
        private boolean currentPreventWildBlockDamage = false;

        // enderpearl teleport executes before onProjectileHit, so handle this
        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                if (playersUsingSkill.contains(event.getPlayer().getName())) {
                    event.setCancelled(true);
                }
            }
        }

        // fires before onProjectileHit
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPaintingBreakByEntity(HangingBreakByEntityEvent event) {
            if (event.getRemover() instanceof Player) {
                Player remover = (Player) event.getRemover();
                if (playersUsingSkill.contains(remover.getName())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (event.getEntityType() == EntityType.ENDER_PEARL) {
                EnderPearl enderPearl = (EnderPearl) event.getEntity();
                EnderBomb enderBomb = enderBombs.get(enderPearl);

                if (enderBomb != null) {
                    enderbombHitTick = getFirstWorldTime();
                    enderbombLandingLoc = enderPearl.getLocation();
                    currentEntityDamageMultiplier = enderBomb.getEntityDamageMultiplier();
                    currentPreventWildBlockDamage = enderBomb.isPreventWildDamage();

                    enderPearl.getWorld().createExplosion(enderPearl.getLocation(), enderBomb.getRadius(), enderBomb.isFire());

                    enderBombs.remove(enderPearl);
                    LivingEntity shooter = enderPearl.getShooter();
                    if (shooter instanceof Player) {
                        playersUsingSkill.remove(((Player) shooter).getName());
                    }
                    enderPearl.remove();
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityExplode(EntityExplodeEvent event) {
            if (getFirstWorldTime() == enderbombHitTick && event.getEntity() == null &&
                    enderbombLandingLoc.distanceSquared(event.getLocation()) < 4) { // enderbomb explosion

                // force explosion, even if protection plugin canceled it, but without block damage
                if (currentPreventWildBlockDamage || event.isCancelled()) {
                    event.blockList().clear();
                    event.setYield(0.0F);
                }
                event.setCancelled(false);
            }
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (getFirstWorldTime() == enderbombHitTick) {
                Entity entity = event.getEntity();
                if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION &&
                        enderbombLandingLoc.distanceSquared(entity.getLocation()) < 625) { // enderbomb explosion
                    if (currentEntityDamageMultiplier > 0 && entity instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) entity;
                        living.setNoDamageTicks(0);
                        event.setDamage((int) Math.round(event.getDamage() * currentEntityDamageMultiplier));
                    } else {
                        event.setCancelled(true);
                    }
                }
            }
        }

        private long getFirstWorldTime() {
            return Bukkit.getWorlds().get(0).getFullTime();
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerInteract(PlayerInteractEvent event) {
            Action action = event.getAction();
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            Player player = event.getPlayer();
            if (player.getGameMode() == GameMode.CREATIVE) return;
            if (player.getItemInHand() == null || player.getItemInHand().getType() != Material.ENDER_PEARL) return;

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (isPreventEnderPearlUseFor(hero)) {
                event.setUseItemInHand(Event.Result.DENY);
                Messaging.send(player, enderpearlDenyText);
            }
        }
    }
}
