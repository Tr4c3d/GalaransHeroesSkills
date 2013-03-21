package mccity.heroes.skills.transform;

import com.google.common.collect.Maps;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
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
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.api.PlayerUndisguiseEvent;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;
import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

import java.util.*;

public class SkillTransform extends TargettedSkill implements Listener {

    private final DisguiseCraftAPI dcApi;

    private static final Map<Class<? extends LivingEntity>, DisguiseType> mobMap = Maps.newLinkedHashMap();
    static {
        mobMap.put(Zombie.class, DisguiseType.Zombie);
        mobMap.put(Skeleton.class, DisguiseType.Skeleton);
        mobMap.put(Slime.class, DisguiseType.Slime);
        mobMap.put(CaveSpider.class, DisguiseType.CaveSpider);
        mobMap.put(Spider.class, DisguiseType.Spider);
        mobMap.put(Creeper.class, DisguiseType.Creeper);
        mobMap.put(Silverfish.class, DisguiseType.Silverfish);
        mobMap.put(Witch.class, DisguiseType.Witch);

        mobMap.put(Enderman.class, DisguiseType.Enderman);
        mobMap.put(PigZombie.class, DisguiseType.PigZombie);
        mobMap.put(Ghast.class, DisguiseType.Ghast);
        mobMap.put(Blaze.class, DisguiseType.Blaze);
        mobMap.put(MagmaCube.class, DisguiseType.MagmaCube);

        mobMap.put(Pig.class, DisguiseType.Pig);
        mobMap.put(Sheep.class, DisguiseType.Sheep);
        mobMap.put(MushroomCow.class, DisguiseType.MushroomCow);
        mobMap.put(Cow.class, DisguiseType.Cow);
        mobMap.put(Chicken.class, DisguiseType.Chicken);
        mobMap.put(Squid.class, DisguiseType.Squid);
        mobMap.put(Bat.class, DisguiseType.Bat);

        mobMap.put(IronGolem.class, DisguiseType.IronGolem);
        mobMap.put(Villager.class, DisguiseType.Villager);
        mobMap.put(Snowman.class, DisguiseType.Snowman);
        mobMap.put(Ocelot.class, DisguiseType.Ocelot);
        mobMap.put(Wolf.class, DisguiseType.Wolf);

        mobMap.put(EnderDragon.class, DisguiseType.EnderDragon);
        mobMap.put(Giant.class, DisguiseType.Giant);
        mobMap.put(Wither.class, DisguiseType.Wither);
    }

    private static final String LOW_LEVEL_TEXT_DEFAULT = "You must be at least level %level% to transform into %creature%";
    private static final String SKILL_DISABLED_TEXT_DEFAULT = "Unable to use this skill in other form";
    private static final String APPLY_TEXT_DEFAULT = "You have transformed into %creature%";
    private static final String EXPIRE_TEXT_DEFAULT = "Transform expires";

    private String lowLevelText;
    private String skillDisabledText;
    private String applyText;
    private String expireText;

    public SkillTransform(Heroes plugin) {
        super(plugin, "Transform");
        setDescription("You transform into your target for $1s.");
        setUsage("/skill transform");
        setArgumentRange(0, 1);
        setIdentifiers("skill transform");
        setTypes(SkillType.SILENCABLE, SkillType.ILLUSION);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        dcApi = DisguiseCraft.getAPI();

    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.REAGENT.node(), Material.EYE_OF_ENDER.getId());
        node.set(SkillSetting.REAGENT_COST.node(), 0);
        node.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.1);
        node.set(SkillSetting.DURATION.node(), 90000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 2000);
        node.set("disabled-skills", Collections.emptyList());

        node.set("level-requirements." + DisguiseType.Player, 1);
        for (DisguiseType mobType : mobMap.values()) {
            node.set("level-requirements." + mobType.name(), 1);
        }
        // disable bosses by default
        for (Class clazz : new Class[] { EnderDragon.class, Giant.class, Wither.class }) {
            node.set("level-requirements." + mobMap.get(clazz).name(), 200);
        }

        node.set("low-level-text", LOW_LEVEL_TEXT_DEFAULT);
        node.set("skill-disabled-text", SKILL_DISABLED_TEXT_DEFAULT);
        node.set(SkillSetting.APPLY_TEXT.node(), APPLY_TEXT_DEFAULT);
        node.set(SkillSetting.EXPIRE_TEXT.node(), EXPIRE_TEXT_DEFAULT);
        return node;
    }

    @Override
    public void init() {
        lowLevelText = SkillConfigManager.getRaw(this, "low-level-text", LOW_LEVEL_TEXT_DEFAULT)
                .replace("%level%", "$1").replace("%creature%", "$2");
        skillDisabledText = SkillConfigManager.getRaw(this, "skill-disabled-text", SKILL_DISABLED_TEXT_DEFAULT);
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, APPLY_TEXT_DEFAULT)
                .replace("%creature%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, EXPIRE_TEXT_DEFAULT);
    }

    @Override
    public String getDescription(Hero hero) {
        Player player = hero.getPlayer();

        StringBuilder desc = new StringBuilder(getDescription()
                .replace("$1", Util.stringDouble(getDuration(hero) / 1000.0))
        );

        if (dcApi.isDisguised(player)) {
            desc.append(ChatColor.DARK_PURPLE);
            desc.append(" Current form: ");
            desc.append(disguiseToString(dcApi.getDisguise(player)));
            desc.append(ChatColor.YELLOW);
        }

        desc.append(" MaxDistance:");
        desc.append(Util.stringDouble(getMaxDistance(hero)));

        double cdSec = getCooldown(hero) / 1000.0;
        if (cdSec > 0) {
            desc.append(" CD:");
            desc.append(Util.formatDouble(cdSec));
            desc.append("s");
        }

        int mana = getMana(hero);
        if (mana > 0) {
            desc.append(" M:");
            desc.append(mana);
        }

        List<String> disabledSkills = getDisabledSkills(hero);
        if (!disabledSkills.isEmpty()) {
            desc.append(ChatColor.RED);
            desc.append(" Disabled Skills: ");
            Iterator<String> itr = disabledSkills.iterator();
            while (itr.hasNext()) {
                desc.append(itr.next());
                if (itr.hasNext()) {
                    desc.append(", ");
                }
            }
            desc.append(ChatColor.YELLOW);
        }

        return desc.toString();
    }

    public List<String> getDisabledSkills(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "disabled-skills", new ArrayList<String>());
    }

    public int getMinLevelForDisguise(Hero hero, DisguiseType disguiseType) {
        return SkillConfigManager.getUseSetting(hero, this, "level-requirements." + disguiseType.name(), 1, true);
    }

    public int getMaxDistance(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10.0, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE, 0.1, false) * hero.getSkillLevel(this));
    }

    public int getDuration(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 90000, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 2000, false) * hero.getSkillLevel(this);
    }

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, true) -
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return (int) Math.max(0.0, SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0.0, true) -
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0.0, false) * hero.getSkillLevel(this));
    }

    public SkillResult use(Hero hero, LivingEntity target, String args[]) {
        if (target == hero.getPlayer()) return SkillResult.INVALID_TARGET_NO_MSG;

        if (target instanceof Player) {
            if (canTransformIntoWithNotify(hero, DisguiseType.Player)) {
                Player targetPlayer = (Player) target;
                Disguise dis = new Disguise(dcApi.newEntityID(), targetPlayer.getName(), DisguiseType.Player);
                hero.addEffect(new TransformEffect(this, dis, getDuration(hero), getDisabledSkills(hero)));
            }
        } else {
            DisguiseType disType = null;
            for (Class<? extends LivingEntity> curClass : mobMap.keySet()) {
                if (curClass.isAssignableFrom(target.getClass())) {
                    disType = mobMap.get(curClass);
                    break;
                }
            }
            if (disType != null) {
                if (canTransformIntoWithNotify(hero, disType)) {
                    Disguise dis = new Disguise(dcApi.newEntityID(), disType);
                    hero.addEffect(new TransformEffect(this, dis, getDuration(hero), getDisabledSkills(hero)));
                }
            } else {
                return SkillResult.INVALID_TARGET;
            }
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public String disguiseToString(Disguise disg) {
        return disg.type == DisguiseType.Player ? "Player " + disg.data.getFirst() : disg.type.name();
    }

    private boolean canTransformIntoWithNotify(Hero hero, DisguiseType disguiseType) {
        int minLevel = getMinLevelForDisguise(hero, disguiseType);
        if (hero.getSkillLevel(this) < minLevel) {
            Messaging.send(hero.getPlayer(), lowLevelText, minLevel, disguiseType.name());
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSkillUse(SkillUseEvent event) {
        Hero hero = event.getHero();
        if (!hero.hasEffect("Transform")) return;
        TransformEffect transformEffect = (TransformEffect) hero.getEffect("Transform");

        if (transformEffect.isSkillDisabled(event.getSkill())) {
            event.setCancelled(true);
            Messaging.send(hero.getPlayer(), skillDisabledText);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUndisguise(PlayerUndisguiseEvent event) {
        Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
        if (!hero.hasEffect("Transform")) return;
        TransformEffect transformEffect = (TransformEffect) hero.getEffect("Transform");

        if (!transformEffect.isNowRemovingEffect()) {
            hero.removeEffect(transformEffect);
        }
    }

    public class TransformEffect extends ExpirableEffect {

        private final Set<String> disabledSkillsLowCase = new HashSet<String>();
        private final Disguise disguise;

        private boolean nowRemovingEffect = false;

        public TransformEffect(Skill skill, Disguise dis, long duration, Collection<String> disabledSkills) {
            super(skill, "Transform", duration);
            disguise = dis;
            for (String disabledSkill : disabledSkills) {
                disabledSkillsLowCase.add(disabledSkill.toLowerCase());
            }

            types.add(EffectType.FORM);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            if (dcApi.isDisguised(player)) {
                dcApi.changePlayerDisguise(player, disguise);
            } else {
                dcApi.disguisePlayer(player, disguise);
            }

            Messaging.send(player, applyText, disguiseToString(disguise));
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            nowRemovingEffect = true; // prevent removing this effect again in the undisguise event handler
            if (dcApi.isDisguised(player) && dcApi.getDisguise(player) == disguise) { // remove only effect's disguise
                dcApi.undisguisePlayer(player);
                Messaging.send(player, expireText);
            }
        }

        public boolean isSkillDisabled(Skill skill) {
            return disabledSkillsLowCase.contains(skill.getName().toLowerCase());
        }

        public boolean isNowRemovingEffect() {
            return nowRemovingEffect;
        }
    }
}
