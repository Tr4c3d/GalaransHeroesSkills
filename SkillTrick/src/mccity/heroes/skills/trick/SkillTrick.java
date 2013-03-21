package mccity.heroes.skills.trick;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import java.util.HashSet;
import java.util.Set;

public class SkillTrick extends TargettedSkill implements Listener {

    private String disarmedByText;
    private String failText;

    private final Set<LivingEntity> disarmedMobs = new HashSet<LivingEntity>();

    public SkillTrick(Heroes plugin) {
        super(plugin, "Trick");
        setDescription("Attempt to disarm the target.");
        setUsage("/skill trick <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill trick");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.ITEM);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this.plugin);
    }

    @Override
    public void init() {
        super.init();
        disarmedByText = SkillConfigManager.getRaw(this, "disarmed-by-text", "%target% disarmed by %player%")
                .replace("%target%", "$1")
                .replace("%player%", "$2");
        failText = ChatColor.DARK_RED + SkillConfigManager.getRaw(this, "fail-text", "Failed to disarm target");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(SkillSetting.COOLDOWN.node(), 45000);
        defaultConfig.set(SkillSetting.MANA.node(), 30);
        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 5);
        defaultConfig.set("chance-base", 0.3);
        defaultConfig.set("chance-per-level", 0.005);
        defaultConfig.set("mob-chance-base", 0.4);
        defaultConfig.set("mob-chance-per-level", 0.01);
        defaultConfig.set("mob-disarm-time", 10000);
        defaultConfig.set("disarmed-by-text", "%target% disarmed by %player%");
        defaultConfig.set("fail-text", "Failed to disarm target");
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        descr.append(" Chance (");
        descr.append(ChatColor.DARK_PURPLE);
        descr.append("player");
        descr.append(ChatColor.YELLOW);
        descr.append('/');
        descr.append(ChatColor.DARK_GREEN);
        descr.append("mob");
        descr.append(ChatColor.YELLOW);
        descr.append("): ");
        double playerChance = getDisarmChanceFor(hero, false);
        descr.append(ChatColor.DARK_PURPLE);
        descr.append(Util.formatDouble(playerChance * 100.0));
        descr.append('%');
        descr.append(ChatColor.YELLOW);
        descr.append('/');
        double mobChance = getDisarmChanceFor(hero, true);
        descr.append(ChatColor.DARK_GREEN);
        descr.append(Util.formatDouble(mobChance * 100.0));
        descr.append('%');
        descr.append(ChatColor.YELLOW);

        double cdSec = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 45000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 30, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        double distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 5, false);
        if (distance > 0) {
            descr.append(" Dist:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    private double getDisarmChanceFor(Hero hero, boolean isMob) {
        String prefix = isMob ? "mob-" : "";
        double chance = SkillConfigManager.getUseSetting(hero, this, prefix + "chance-base", 30.0, false) +
                SkillConfigManager.getUseSetting(hero, this, prefix + "chance-per-level", 0.5, false) * hero.getSkillLevel(this);
        return Math.min(chance, 1.0);
    }

    private boolean rollDisarm(Hero hero, boolean isMob) {
        return Util.nextRand() < getDisarmChanceFor(hero, isMob);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        boolean success = false;
        String targetName = null;

        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (rollDisarm(hero, false)) {
                disarmPlayer(targetPlayer);
                Util.syncInventory(targetPlayer, plugin);

                success = true;
                targetName = targetPlayer.getName();
            }
        } else {
            if (rollDisarm(hero, true)) {
                disarmedMobs.add(target);
                int mobDisarmTicks = SkillConfigManager.getUseSetting(hero, this, "mob-disarm-time", 10000, false) / 50;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ArmMobTask(target), mobDisarmTicks);

                success = true;
                targetName = target.getType().getName();
            }
        }

        if (success) {
            broadcast(hero.getPlayer().getLocation(), disarmedByText, targetName, hero.getPlayer().getName());
        } else {
            Messaging.send(hero.getPlayer(), failText);
        }

        return SkillResult.NORMAL;
    }

    private void disarmPlayer(Player target) {
        ItemStack handStack = target.getItemInHand();
        if (handStack == null || handStack.getType() == Material.AIR) return;
        Inventory targetInv = target.getInventory();

        int lastEmptySlot = -1;
        for (int i = 35; i >= 0; i--) {
            ItemStack curStack = targetInv.getItem(i);
            if (curStack == null || curStack.getType() == Material.AIR) {
                lastEmptySlot = i;
                break;
            }
        }

        int swapSlot;
        if (lastEmptySlot >= 0) {
            swapSlot = lastEmptySlot;
        } else { // no empty slots
            // swap with random slot
            swapSlot = Util.nextInt(36);
        }
        target.setItemInHand(targetInv.getItem(swapSlot));
        targetInv.setItem(swapSlot, handStack);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity) {
            LivingEntity damagerLiving = (LivingEntity) damager;
            if (disarmedMobs.contains(damagerLiving)) {
                event.setCancelled(true);
            }
        }
    }

    private class ArmMobTask implements Runnable {

        private final LivingEntity mob;

        public ArmMobTask(LivingEntity mob) {
            this.mob = mob;
        }

        @Override
        public void run() {
            disarmedMobs.remove(mob);
        }
    }
}
