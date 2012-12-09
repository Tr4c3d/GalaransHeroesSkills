package mccity.heroes.skills.boomerang;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillBoomerang extends TargettedSkill {

    private static final Map<String, Double> defaultItems = new HashMap<String, Double>();

    static {
        defaultItems.put("SHEARS", 1.0);
    }

    private String unableToThrow;

    public SkillBoomerang(Heroes plugin) {
        super(plugin, "Boomerang");
        setDescription("Throw boomerang to target, which strikes it for $1 damage and returns back. You have a $2% chance to catch your boomerang. Max distance: $3m.");
        setUsage("/skill boomerang");
        setArgumentRange(0, 1);
        setIdentifiers("skill boomerang");
        setTypes(SkillType.PHYSICAL, SkillType.ITEM, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        for (Map.Entry<String, Double> curItem : defaultItems.entrySet()) {
            defaultConfig.set("items." + curItem.getKey(), curItem.getValue());
        }
        defaultConfig.set("catch-chance-base", 0.4);
        defaultConfig.set("catch-chance-per-level", 0.01);
        defaultConfig.set("unable-to-throw-text", "You are not skilled to throw this item");
        defaultConfig.set(Setting.DAMAGE.node(), 10);
        defaultConfig.set(Setting.DAMAGE_INCREASE.node(), 0.1);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 20);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.2);
        defaultConfig.set(Setting.COOLDOWN.node(), 10000);
        defaultConfig.set(Setting.MANA.node(), 5);
        defaultConfig.set("durability-decrease", 5);
        return defaultConfig;
    }

    @Override
    public void init() {
        super.init();
        unableToThrow = SkillConfigManager.getRaw(this, "turrets-per-player", "You are not skilled to throw this item");
    }

    public String getDescription(Hero hero) {
        StringBuilder descrSb = new StringBuilder(getDescription());

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 15000, false) / 1000.0;
        if (cdSec > 0) {
            descrSb.append(" CD:");
            descrSb.append(Util.formatDouble(cdSec));
            descrSb.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 20, false);
        if (mana > 0) {
            descrSb.append(" M:");
            descrSb.append(mana);
        }
        descrSb.append(" Items: " + ChatColor.DARK_PURPLE);
        Iterator<String> itr = getAllowedItemsFor(hero).iterator();
        while (itr.hasNext()) {
            descrSb.append(itr.next().replace('_', ' ').trim().toLowerCase());
            if (itr.hasNext()) {
                descrSb.append(", ");
            }
        }

        String descr = descrSb.toString();
        descr = descr.replace("$1", Integer.toString(getDamageFor(hero)));
        descr = descr.replace("$2", Util.stringDouble(getCatchChanceFor(hero) * 100));
        descr = descr.replace("$3", Util.stringDouble(getMaxDistanceFor(hero)));
        return descr;
    }

    private double getMaxDistanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 20, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE, 0.2, false) * hero.getSkillLevel(this);
    }

    private int getDamageFor(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.1, false) * hero.getSkillLevel(this));
    }

    private double getCatchChanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "catch-chance-base", 0.4, false) +
                SkillConfigManager.getUseSetting(hero, this, "catch-chance-per-level", 0.01, false) * hero.getSkillLevel(this);
    }

    private Set<String> getAllowedItemsFor(Hero hero) {
        return SkillConfigManager.getUseSettingKeys(hero, this, "items");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        ItemStack handStack = player.getItemInHand();
        if (handStack == null || !getAllowedItemsFor(hero).contains(handStack.getType().name())) {
            Messaging.send(player, unableToThrow);
            return SkillResult.FAIL;
        }

        int distance = (int) target.getLocation().toVector().subtract(player.getLocation().toVector()).length();
        distance = Math.max(1, Math.min(distance, 50));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new BoomerangForwardTask(hero, handStack, target, distance), distance);

        player.setItemInHand(null);
        return SkillResult.NORMAL;
    }

    private class BoomerangForwardTask implements Runnable {

        private final Hero hero;
        private final ItemStack item;
        private final LivingEntity target;
        private final int distance;

        public BoomerangForwardTask(Hero hero, ItemStack item, LivingEntity target, int distance) {
            this.hero = hero;
            this.item = item;
            this.target = target;
            this.distance = distance;
        }

        @Override
        public void run() {
            if (!target.isDead()) {
                double damageMult = SkillConfigManager.getUseSetting(hero, SkillBoomerang.this, "items." + item.getType().name(), 0.5, false);
                damageEntity(target, hero.getPlayer(), (int) (getDamageFor(hero) * damageMult), EntityDamageEvent.DamageCause.CUSTOM, true);
                target.getWorld().playEffect(target.getEyeLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new BoomerangReturnTask(hero, item), distance / 2);
        }
    }

    private class BoomerangReturnTask implements Runnable {

        private final Hero hero;
        private final ItemStack item;

        public BoomerangReturnTask(Hero hero, ItemStack item) {
            this.hero = hero;
            this.item = item;
        }

        @Override
        public void run() {
            if (item.getType().getMaxDurability() > 0) {
                int durDecrease = SkillConfigManager.getUseSetting(hero, SkillBoomerang.this, "durability-decrease", 5, false);
                item.setDurability((short) (item.getDurability() + durDecrease));
                if (item.getDurability() >= item.getType().getMaxDurability()) { // tool was breaked
                    return;
                }
            }

            Player player = hero.getPlayer();
            int firstEmptySlot = player.getInventory().firstEmpty();
            if (firstEmptySlot != -1 && Util.nextRand() < getCatchChanceFor(hero) && player.isOnline() && !player.isDead()) {
                player.getInventory().setItem(firstEmptySlot, item);
            } else {
                Vector landingOffset = new Vector(0, 0, 0);
                double yaw = player.getLocation().getYaw();
                landingOffset.setX(-Math.sin(Math.toRadians(yaw)));
                landingOffset.setZ(Math.cos(Math.toRadians(yaw)));
                landingOffset.normalize();
                landingOffset.multiply(1.2);
                Location landingLoc = player.getEyeLocation().add(landingOffset);

                landingLoc.getWorld().dropItem(landingLoc, item);
            }
        }
    }
}
