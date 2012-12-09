package mccity.heroes.skills.undisguise;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;

public class SkillUndisguise extends ActiveSkill implements Listener {

    private String undisguisedText;
    private String youAreNotDisguisedText;
    private String undisguisedOnAnyDamageText;
    private String undisguisedOnPvpDamageText;
    private String undisguisedOnAnyAttackText;
    private String undisguisedOnPvpAttackText;

    private final DisguiseCraftAPI dcApi;

    public SkillUndisguise(Heroes plugin) {
        super(plugin, "Undisguise");
        setDescription("Undisguises yourself.");
        setUsage("/skill undisguise");
        setArgumentRange(0, 0);
        setIdentifiers("skill undisguise");
        setTypes(SkillType.SILENCABLE, SkillType.ILLUSION);

        dcApi = DisguiseCraft.getAPI();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("undisguise-on-any-damage", false);
        node.set("undisguise-on-pvp-damage", true);
        node.set("undisguise-on-any-attack", false);
        node.set("undisguise-on-pvp-attack", false);

        node.set("undisguised-text", "You transformed back to normal form");
        node.set("you-are-not-disguised-text", "You are already in normal form");
        node.set("undisguised-on-any-damage-text", "You've got damage and were undisguised!");
        node.set("undisguised-on-pvp-damage-text", "You were hit by another hero and undisguised!");
        node.set("undisguised-on-any-attack-text", "You attack a target and were undisguised!");
        node.set("undisguised-on-pvp-attack-text", "You attack a hero and were undisguised!");

        return node;
    }

    @Override
    public void init() {
        super.init();
        undisguisedText = SkillConfigManager.getRaw(this, "undisguised-text", "You transformed back to normal form");
        youAreNotDisguisedText = SkillConfigManager.getRaw(this, "you-are-not-disguised-text", "You are already in normal form");
        undisguisedOnAnyDamageText = SkillConfigManager.getRaw(this, "undisguised-on-any-damage-text", "You've got damage and were undisguised!");
        undisguisedOnPvpDamageText = SkillConfigManager.getRaw(this, "undisguised-on-pvp-damage-text", "You were hit by another hero and undisguised!");
        undisguisedOnAnyAttackText = SkillConfigManager.getRaw(this, "undisguised-on-any-attack-text", "You attack a target and were undisguised!");
        undisguisedOnPvpAttackText = SkillConfigManager.getRaw(this, "undisguised-on-pvp-attack-text", "You attack a hero and were undisguised!");
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        String onDamageKey = null;
        if (isUndisguiseOnDamage(hero, "any")) {
            onDamageKey = "any";
        } else if (isUndisguiseOnDamage(hero, "pvp")) {
            onDamageKey = "pvp";
        }

        String onAttackKey = null;
        if (isUndisguiseOnAttack(hero, "any")) {
            onAttackKey = "any";
        } else if (isUndisguiseOnAttack(hero, "pvp")) {
            onAttackKey = "pvp";
        }

        if (onDamageKey != null || onAttackKey != null) {
            descr.append(" Triggers on");
            if (onDamageKey != null) {
                descr.append(" taking ");
                descr.append(onDamageKey);
                descr.append(" damage");
                if (onAttackKey != null) {
                    descr.append(" and");
                }
            }
            if (onAttackKey != null) {
                descr.append(' ');
                descr.append(onAttackKey);
                descr.append(" attack");
            }
        }

        return descr.toString();
    }

    public boolean isUndisguiseOnDamage(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "undisguise-on-" + key + "-damage", true);
    }

    private boolean isUndisguiseOnAttack(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "undisguise-on-" + key + "-attack", true);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (dcApi.isDisguised(player)) {
            dcApi.undisguisePlayer(player);
            Messaging.send(player, undisguisedText);
        } else {
            Messaging.send(player, youAreNotDisguisedText);
        }

        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() == 0) return;

        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (dcApi.isDisguised(player)) {
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.canUseSkill(this)) {
                if (isUndisguiseOnDamage(hero, "any")) {
                    undisguise(player, undisguisedOnAnyDamageText);
                } else if (isUndisguiseOnDamage(hero, "pvp") && event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
                    if (edbe.getDamager() instanceof Player) {
                        undisguise(player, undisguisedOnPvpDamageText);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillDamage(SkillDamageEvent event) {
        if (event.getDamager() instanceof Hero) {
            onHeroAttack((Hero) event.getDamager(), event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponDamage(WeaponDamageEvent event) {
        if (event.getDamager() instanceof Hero) {
            onHeroAttack((Hero) event.getDamager(), event.getEntity());
        }
    }

    public void onHeroAttack(Hero hero, Entity target) {
        if (!(target instanceof LivingEntity)) return;
        LivingEntity targetLiving = (LivingEntity) target;

        if (hero.canUseSkill(this) && dcApi.isDisguised(hero.getPlayer())) {
            if (isUndisguiseOnAttack(hero, "any")) {
                undisguise(hero.getPlayer(), undisguisedOnAnyAttackText);
            } else if (targetLiving instanceof Player && isUndisguiseOnAttack(hero, "pvp")) {
                undisguise(hero.getPlayer(), undisguisedOnPvpAttackText);
            }
        }
    }

    public void undisguise(Player player, String message) {
        dcApi.undisguisePlayer(player);
        Messaging.send(player, message);
    }
}
