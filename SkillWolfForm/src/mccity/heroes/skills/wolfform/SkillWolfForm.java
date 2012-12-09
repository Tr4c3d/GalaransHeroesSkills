package mccity.heroes.skills.wolfform;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.api.PlayerUndisguiseEvent;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;
import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

import java.util.*;

public class SkillWolfForm extends ActiveSkill implements Listener {

    private String failNotInNormalFormText;
    private String failSkillNotAllowedInWolfFormText;
    private String applyText;
    private String expireText;

    private final DisguiseCraftAPI dcApi;

    public SkillWolfForm(Heroes plugin) {
        super(plugin, "WolfForm");
        setDescription("You transform into wolf for $1 seconds. In this form your attack power and moving speed increasing -");
        setUsage("/skill wolfform");
        setArgumentRange(0, 0);
        setIdentifiers("skill wolfform");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.ILLUSION);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        dcApi = DisguiseCraft.getAPI();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.DURATION.node(), 8000);
        defaultConfig.set(Setting.DURATION_INCREASE.node(), 100);
        defaultConfig.set("damage-multiplier-base", 1.0);
        defaultConfig.set("damage-multiplier-per-level", 0.0);
        defaultConfig.set("damage-multiplier-unarmed-base", 3.0);
        defaultConfig.set("damage-multiplier-unarmed-per-level", 0.0);
        defaultConfig.set("speed-effect-level", 1);
        defaultConfig.set("restrict-skill-use", false);
        defaultConfig.set("allowed-skills", Collections.<String>emptyList());

        defaultConfig.set(Setting.APPLY_TEXT.node(), "You gain wolf power!");
        defaultConfig.set(Setting.EXPIRE_TEXT.node(), "WolfForm expires");
        defaultConfig.set("fail-not-normal-form-text", "Unable to use when transformed");
        defaultConfig.set("fail-skill-not-allowed-in-wolfform-text", "Unable to use this skill in Wolf Form");
        return defaultConfig;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You gain wolf power!");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "WolfForm expires");
        failNotInNormalFormText = SkillConfigManager.getRaw(this, "fail-not-normal-form-text", "Unable to use when transformed");
        failSkillNotAllowedInWolfFormText = SkillConfigManager.getRaw(this, "fail-skill-not-allowed-in-wolfform-text",
                "Unable to use this skill in Wolf Form");
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getDurationFor(hero) / 1000)));

        descr.append(" armed: ");
        descr.append(getDamageMultiplierFor(hero, false));
        descr.append("x, unarmed: ");
        descr.append(getDamageMultiplierFor(hero, true));

        if (isRestrictSkillUseFor(hero)) {
            descr.append(", but you can not use your skills");
            Set<String> allowedSkills = getAllowedSkillsFor(hero);
            if (!allowedSkills.isEmpty()) {
                descr.append(", except of: " + ChatColor.DARK_PURPLE);
                Iterator<String> itr = allowedSkills.iterator();
                while (itr.hasNext()) {
                    descr.append(itr.next());
                    if (itr.hasNext()) {
                        descr.append(", ");
                    }
                }
                descr.append(ChatColor.YELLOW);
            }
        }
        descr.append('.');

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

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getDurationFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 8000, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 100, false) * hero.getSkillLevel(this);
    }

    public double getDamageMultiplierFor(Hero hero, boolean unarmed) {
        String modString = unarmed ? "-unarmed" : "";
        return SkillConfigManager.getUseSetting(hero, this, "damage-multiplier" + modString + "-base", 2.0, false) +
                SkillConfigManager.getUseSetting(hero, this, "damage-multiplier" + modString + "-per-level", 0.0, false) * hero.getSkillLevel(this);
    }

    public int getSpeedEffectLevelFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "speed-effect-level", 1, false);
    }

    public boolean isRestrictSkillUseFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "restrict-skill-use", false);
    }

    public Set<String> getAllowedSkillsFor(Hero hero) {
        List<String> allowedListClass = SkillConfigManager.getUseSetting(hero, this, "allowed-skills", Collections.<String>emptyList());
        Set<String> result = new HashSet<String>();
        for (String curAllowed : allowedListClass) {
            result.add(curAllowed.toLowerCase());
        }
        return result;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (dcApi.isDisguised(player)) {
            Messaging.send(player, failNotInNormalFormText);
            return SkillResult.FAIL;
        }

        hero.addEffect(new WolfFormEffect(this, getDurationFor(hero),
                getDamageMultiplierFor(hero, false), getDamageMultiplierFor(hero, true),
                getSpeedEffectLevelFor(hero), isRestrictSkillUseFor(hero), getAllowedSkillsFor(hero)));

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponDamage(WeaponDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        CharacterTemplate character = event.getDamager();
        if (!(character instanceof Hero)) return;
        Hero hero = (Hero) character;

        if (!hero.hasEffect("WolfForm")) return;
        WolfFormEffect wolfFormEffect = (WolfFormEffect) hero.getEffect("WolfForm");

        Player player = hero.getPlayer();
        boolean unarmed = player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR;
        double damageMultiplier = unarmed ? wolfFormEffect.getDamageMultiplierUnarmed() : wolfFormEffect.getDamageMultiplier();
        event.setDamage((int) (event.getDamage() * damageMultiplier));

        if (damageMultiplier > 1.0) {
            Entity target = event.getEntity();
            if (target instanceof LivingEntity) {
                LivingEntity defender = (LivingEntity) target;
                defender.getWorld().playEffect(defender.getEyeLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSkillUse(SkillUseEvent event) {
        Hero hero = event.getHero();
        if (!hero.hasEffect("WolfForm")) return;
        WolfFormEffect wolfFormEffect = (WolfFormEffect) hero.getEffect("WolfForm");

        if (wolfFormEffect.isRestrictSkillUse()) {
            if (!wolfFormEffect.getAllowedSkills().contains(event.getSkill().getName().toLowerCase())) {
                event.setCancelled(true);
                Messaging.send(hero.getPlayer(), failSkillNotAllowedInWolfFormText);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUndisguise(PlayerUndisguiseEvent event) {
        Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
        if (!hero.hasEffect("WolfForm")) return;
        WolfFormEffect wolfFormEffect = (WolfFormEffect) hero.getEffect("WolfForm");

        if (!wolfFormEffect.isRemovingEffect()) {
            hero.removeEffect(wolfFormEffect);
        }
    }

    public class WolfFormEffect extends ExpirableEffect {

        private final double damageMultiplier;
        private final double damageMultiplierUnarmed;
        private final int speedEffectLevel;
        private final boolean restrictSkillUse;
        private final Set<String> allowedSkills;

        private boolean removingEffect = false;

        public WolfFormEffect(Skill skill, long duration, double damageMultiplier, double damageMultiplierUnarmed,
                              int speedEffectLevel, boolean restrictSkillUse, Set<String> allowedSkills) {
            super(skill, "WolfForm", duration);
            this.damageMultiplier = damageMultiplier;
            this.damageMultiplierUnarmed = damageMultiplierUnarmed;
            this.speedEffectLevel = speedEffectLevel;
            this.restrictSkillUse = restrictSkillUse;
            if (restrictSkillUse) {
                this.allowedSkills = allowedSkills;
            } else {
                this.allowedSkills = null;
            }

            types.add(EffectType.FORM);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            Disguise wolfDisguise = new Disguise(dcApi.newEntityID(), DisguiseType.Wolf);
            if (dcApi.isDisguised(player)) {
                dcApi.changePlayerDisguise(player, wolfDisguise);
            } else {
                dcApi.disguisePlayer(player, wolfDisguise);
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration() / 50), speedEffectLevel - 1));

            Messaging.send(player, SkillWolfForm.this.applyText);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            removingEffect = true; // to prevent remove effect again (stackoverflow) in the undisguise event handler
            if (dcApi.isDisguised(player)) {
                dcApi.undisguisePlayer(player);
            }
            player.removePotionEffect(PotionEffectType.SPEED);

            Messaging.send(player, SkillWolfForm.this.expireText);
        }

        public double getDamageMultiplier() {
            return this.damageMultiplier;
        }

        public double getDamageMultiplierUnarmed() {
            return damageMultiplierUnarmed;
        }

        @SuppressWarnings("UnusedDeclaration")
        public int getSpeedEffectLevel() {
            return speedEffectLevel;
        }

        public boolean isRestrictSkillUse() {
            return restrictSkillUse;
        }

        public Set<String> getAllowedSkills() {
            return allowedSkills;
        }

        public boolean isRemovingEffect() {
            return removingEffect;
        }
    }
}
