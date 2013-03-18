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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SkillBoomerang extends TargettedSkill {

    private static final Map<String, Double> defaultItems = new HashMap<String, Double>();

    static {
        defaultItems.put("SHEARS", 1.0);
    }

    private static final String UNABLE_TO_THROW_TEXT_DEFAULT = "You are not skilled to throw this item";
    private String unableToThrowText;

    public SkillBoomerang(Heroes plugin) {
        super(plugin, "Boomerang");
        setDescription("Throw boomerang to target, which strikes it and returns back. You have a $1% chance to catch your boomerang. Max distance: $2.");
        setUsage("/skill boomerang <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill boomerang");
        setTypes(SkillType.DAMAGING, SkillType.HARMFUL, SkillType.ITEM);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        for (Map.Entry<String, Double> curItem : defaultItems.entrySet()) {
            defaultConfig.set("items." + curItem.getKey(), curItem.getValue());
        }
        
        defaultConfig.set("catch-chance-base", 0.4);
        defaultConfig.set("catch-chance-per-level", 0.01);
        defaultConfig.set(Setting.DAMAGE.node(), 10);
        defaultConfig.set(Setting.DAMAGE_INCREASE.node(), 0.1);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 20);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.2);
        defaultConfig.set(Setting.COOLDOWN.node(), 10000);
        defaultConfig.set(Setting.MANA.node(), 5);
        defaultConfig.set("durability-decrease", 5);
        
        defaultConfig.set("unable-to-throw-text", UNABLE_TO_THROW_TEXT_DEFAULT);
        return defaultConfig;
    }

    @Override
    public void init() {
        super.init();
        unableToThrowText = SkillConfigManager.getRaw(this, "unable-to-throw-text", UNABLE_TO_THROW_TEXT_DEFAULT);
    }

    @Override
    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription()
                .replace("$1", Util.stringDouble(getCatchChanceFor(hero) * 100))
                .replace("$2", Util.stringDouble(getMaxDistanceFor(hero)))
        );

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
        
        descr.append(ChatColor.GREEN);
        descr.append(" Throwable items and damage: ");
        Iterator<String> itr = getAllowedItemsFor(hero).iterator();
        while (itr.hasNext()) {
            String matName = itr.next();
            descr.append(ChatColor.DARK_PURPLE);
            descr.append(matName.replace('_', ' ').toLowerCase());
            descr.append(": ");
            descr.append(getDamage(hero, matName));
            
            if (itr.hasNext()) {
                descr.append(ChatColor.GRAY);
                descr.append(", ");
            }
        }

        return descr.toString();
    }

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return (int) Math.max(0.0, SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 0.0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE, 0.0, false) * hero.getSkillLevel(this));
    }

    private double getMaxDistanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 20, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE, 0.2, false) * hero.getSkillLevel(this);
    }

    private double getCatchChanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "catch-chance-base", 0.4, false) +
                SkillConfigManager.getUseSetting(hero, this, "catch-chance-per-level", 0.01, false) * hero.getSkillLevel(this);
    }

    private Set<String> getAllowedItemsFor(Hero hero) {
        return SkillConfigManager.getUseSettingKeys(hero, this, "items");
    }

    private double getDamageBase(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.1, false) * hero.getSkillLevel(this);
    }
    
    private int getDamage(Hero hero, String matName) {
        return (int) (getDamageBase(hero) * SkillConfigManager.getUseSetting(hero, this, "items." + matName, 0.5, false));
    }
    
    private int getDurabilityCost(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "durability-decrease", 5, true);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        ItemStack handStack = player.getItemInHand();
        if (handStack == null || !getAllowedItemsFor(hero).contains(handStack.getType().name())) {
            Messaging.send(player, unableToThrowText);
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
                damageEntity(target, hero.getPlayer(), getDamage(hero, item.getType().name()), EntityDamageEvent.DamageCause.CUSTOM, true);
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
                item.setDurability((short) (item.getDurability() + getDurabilityCost(hero)));
                if (item.getDurability() >= item.getType().getMaxDurability()) { // tool was broken
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
