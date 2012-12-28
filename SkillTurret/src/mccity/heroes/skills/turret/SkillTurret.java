package mccity.heroes.skills.turret;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import mccity.heroes.skills.turret.integration.PluginsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

public class SkillTurret extends PassiveSkill {

    private static final int AI_PERIOD_TICKS = 5;

    private TurretManager turretManager;

    public SkillTurret(Heroes plugin) {
        super(plugin, "Turret");
        setIdentifiers("skill turret");
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.SUMMON, SkillType.KNOWLEDGE);

        Utils.init(plugin);
    }

    @Override
    public void init() {
        super.init();
        Messages.load(this);

        boolean checkTowny = SkillConfigManager.getRaw(this, "integration.towny", true);
        boolean checkFactions = SkillConfigManager.getRaw(this, "integration.factions", true);
        PluginsIntegration.instance.init(checkTowny, checkFactions);
        if (PluginsIntegration.instance.isEnabled()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, PluginsIntegration.instance);
        }
        
        turretManager = new TurretManager(this);
        turretManager.loadAll();
        
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, turretManager, 0, AI_PERIOD_TICKS);
        Bukkit.getPluginManager().registerEvents(new TurretListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new ShutdownListener(), plugin);
    }

    @Override
    protected void unapply(Hero hero) {
        super.unapply(hero);
        turretManager.removeAll(hero);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.COOLDOWN.node(), 45000);
        defaultConfig.set(Setting.MANA.node(), 0);
        defaultConfig.set("max-turrets-per-player", 3);
        defaultConfig.set("replace-oldest-on-limit", false);

        defaultConfig.set("targeting.npc", true);
        defaultConfig.set("targeting.player", true);
        defaultConfig.set("targeting.monster", true);
        defaultConfig.set("targeting.animal", true);

        defaultConfig.set("power.attack-period-ticks", 20);
        defaultConfig.set("power.damage", 3);
        defaultConfig.set("power.fire-arrow", false);

        defaultConfig.set("ammo.unlimited-ammo", false);
        defaultConfig.set("ammo.initial-ammo-base", 10);
        defaultConfig.set("ammo.initial-ammo-increase", 0.4);
        defaultConfig.set("ammo.ammo-per-arrow-base", 5);
        defaultConfig.set("ammo.ammo-per-arrow-increase", 0.2);

        defaultConfig.set("lifetime.persistent", false);
        defaultConfig.set("lifetime.max-lifetime", 300000);
        defaultConfig.set("lifetime.destroy-on-owner-logout", true);
        defaultConfig.set("lifetime.destroy-on-chunk-unload", true);
        defaultConfig.set("lifetime.protect-block", true);

        defaultConfig.set("integration.towny", true);
        defaultConfig.set("integration.factions", true);

        Messages.fillDefaults(defaultConfig);
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder sb = new StringBuilder("You have the ability to build up to ");
        sb.append(maxTurretsFor(hero));
        if (isPersistentFor(hero)) {
            sb.append(" persistent");
        }
        if (isLifetimeProtectedFor(hero)) {
            sb.append(" protected");
        }
        sb.append(" turrets (you have ");
        sb.append(turretManager.getTotalTurrets(hero));
        sb.append(" now) with ");
        if (isUnlimitedAmmoFor(hero)) {
            sb.append("unlimited");
        } else {
            sb.append(getInitialAmmoFor(hero));
        }
        sb.append(" charges, which attacks enemies.");
        if (!isUnlimitedAmmoFor(hero)) {
            sb.append(" Each arrow, loaded to turret adds ");
            sb.append(getAmmoPerArrowFor(hero));
            sb.append(" charges.");
        }

        if (!isPersistentFor(hero)) {
            sb.append(" Max lifetime: ");
            sb.append(getTurretLifetimeFor(hero) / 1000);
            sb.append("s");
        }

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 45000, false) / 1000.0;
        if (cdSec > 0) {
            sb.append(" CD:");
            sb.append(Util.formatDouble(cdSec));
            sb.append("s");
        }

        int mana = getManaCostFor(hero);
        if (mana > 0) {
            sb.append(" M:");
            sb.append(mana);
        }

        return sb.toString();
    }

    public int maxTurretsFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "max-turrets-per-player", 3, false);
    }

    public boolean isUnlimitedAmmoFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "ammo.unlimited-ammo", false);
    }

    public int getInitialAmmoFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "ammo.initial-ammo-base", 10, false) +
                (int) (SkillConfigManager.getUseSetting(hero, this, "ammo.initial-ammo-increase", 0.4, false) * hero.getSkillLevel(this));
    }

    public int getAmmoPerArrowFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "ammo.ammo-per-arrow-base", 5, false) +
                (int) (SkillConfigManager.getUseSetting(hero, this, "ammo.ammo-per-arrow-increase", 0.2, false) * hero.getSkillLevel(this));
    }

    public boolean isPersistentFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "lifetime.persistent", false);
    }

    public int getTurretLifetimeFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "lifetime.max-lifetime", 300000, false);
    }

    public int getManaCostFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 0, false);
    }

    public boolean isLifetimeProtectedFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "lifetime.protect-block", true);
    }

    public class TurretListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {
            Block block = event.getBlockPlaced();
            if (block.getType() != Material.DISPENSER) return;
            Hero hero = Utils.getHero(event.getPlayer());
            if (!hero.hasEffect("Turret")) return;
            if (!checkTurretPlatform(block.getLocation())) return;

            if (turretManager.canAddFor(hero)) {
                Long expiry = hero.getCooldown("Turret");
                if (expiry == null || (expiry <= System.currentTimeMillis())) {
                    int reqMana = getManaCostFor(hero);
                    int mana = hero.getMana();
                    if (mana >= reqMana) {
                        if (turretManager.addFor(hero, block)) {
                            broadcast(block.getLocation(), Messages.turretPlacedBy, event.getPlayer().getName());
                            long cooldown = SkillConfigManager.getUseSetting(hero, SkillTurret.this, Setting.COOLDOWN.node(), 30000, false);
                            hero.setCooldown("Turret", System.currentTimeMillis() + cooldown);
                            hero.setMana(mana - reqMana);
                        }
                    } else {
                        Messaging.send(hero.getPlayer(), "Not enough mana!");
                    }
                } else {
                    long remaining = expiry - System.currentTimeMillis();
                    Messaging.send(hero.getPlayer(), "Sorry, $1 still has $2 seconds left on cooldown!", "Turret", remaining / 1000);
                }
            } else {
                Messaging.send(hero.getPlayer(), Messages.alreadyHaveMaxTurrets);
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getType() == Material.DISPENSER) {
                boolean cancelled = turretManager.onDispenserDestroyed(event.getBlock(), event.getPlayer());
                event.setCancelled(cancelled);
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            Block clicked = event.getClickedBlock();
            if (clicked.getType() == Material.DISPENSER) {
                boolean canceled = turretManager.onDispenserOpen(clicked, event.getPlayer());
                event.setCancelled(canceled);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            turretManager.onPlayerJoin(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            turretManager.onPlayerQuit(event.getPlayer());
        }
    }

    private boolean checkTurretPlatform(Location dispenserLoc) {
        World world = dispenserLoc.getWorld();
        int x = dispenserLoc.getBlockX();
        int y = dispenserLoc.getBlockY();
        int z = dispenserLoc.getBlockZ();

        if (world.getBlockAt(x, y - 1, z).getType() != Material.NETHER_FENCE) return false;

        if (world.getBlockAt(x - 1 , y - 1, z).getType() != Material.NETHER_FENCE) return false;
        if (world.getBlockAt(x + 1 , y - 1, z).getType() != Material.NETHER_FENCE) return false;
        if (world.getBlockAt(x, y - 1, z - 1).getType() != Material.NETHER_FENCE) return false;
        if (world.getBlockAt(x, y - 1, z + 1).getType() != Material.NETHER_FENCE) return false;

        if (world.getBlockAt(x - 1, y, z).getType() != Material.REDSTONE_TORCH_ON) return false;
        if (world.getBlockAt(x + 1, y, z).getType() != Material.REDSTONE_TORCH_ON) return false;
        if (world.getBlockAt(x, y, z - 1).getType() != Material.REDSTONE_TORCH_ON) return false;
        if (world.getBlockAt(x, y, z + 1).getType() != Material.REDSTONE_TORCH_ON) return false;

        return true;
    }

    private class ShutdownListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin().getName().equals("Heroes")) {
                turretManager.saveAll();
            }
        }
    }
}
