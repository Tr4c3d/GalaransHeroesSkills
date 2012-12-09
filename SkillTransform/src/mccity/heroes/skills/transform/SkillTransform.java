package mccity.heroes.skills.transform;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;
import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillTransform extends TargettedSkill {

    private final DisguiseCraftAPI dcApi;

    private static final String PLAYER_CONFIG_KEY = "Player";
    private static final Map<Class, DisguiseType> mobMap = new LinkedHashMap<Class, DisguiseType>();
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

    private String cannotTransformIntoText;
    private String transformedIntoText;

    public SkillTransform(Heroes plugin) {
        super(plugin, "Transform");
        setDescription("You transform into your target.");
        setUsage("/skill transform");
        setArgumentRange(0, 1);
        setIdentifiers("skill transform");
        setTypes(SkillType.SILENCABLE, SkillType.ILLUSION);

        dcApi = DisguiseCraft.getAPI();
    }

    @Override
    public String getDescription(Hero hero) {
        Player player = hero.getPlayer();

        StringBuilder desc = new StringBuilder();
        desc.append(getDescription());

        if (dcApi.isDisguised(player)) {
            desc.append(ChatColor.DARK_PURPLE);
            desc.append(" Current form: ");
            desc.append(disguiseToString(dcApi.getDisguise(player)));
            desc.append(ChatColor.YELLOW);
        }

        desc.append(" Max Distance:");
        desc.append(Util.formatDouble(getMaxDistanceFor(hero)));

        double cdSec = getCooldownFor(hero) / 1000.0;
        if (cdSec > 0) {
            desc.append(" CD:");
            desc.append(Util.formatDouble(cdSec));
            desc.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 50, false);
        if (mana > 0) {
            desc.append(" M:");
            desc.append(mana);
        }

        return desc.toString();
    }

    public boolean canTransformInto(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "target." + key, true);
    }

    public double getMaxDistanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 10.0, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE, 0.1, false) * hero.getSkillLevel(this);
    }

    public int getCooldownFor(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 100000, false) -
                SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE, 1000, false) * hero.getSkillLevel(this));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.REAGENT.node(), Material.EYE_OF_ENDER.getId());
        node.set(Setting.REAGENT_COST.node(), 1);
        node.set(Setting.COOLDOWN.node(), 100000);
        node.set(Setting.COOLDOWN_REDUCE.node(), 1000);
        node.set(Setting.MANA.node(), 50);
        node.set(Setting.MAX_DISTANCE.node(), 10.0);
        node.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.1);

        node.set("target." + PLAYER_CONFIG_KEY, true);
        for (DisguiseType mobType : mobMap.values()) {
            node.set("target." + mobType.name(), true);
        }
        node.set("cannot-transform-type", "You can not transform into $1");
        node.set("transformed-into-type", "You have transformed into $1");
        return node;
    }

    @Override
    public void init() {
        cannotTransformIntoText = SkillConfigManager.getRaw(this, "cannot-transform-type", "You can not transform into $1");
        transformedIntoText = SkillConfigManager.getRaw(this, "transformed-into-type", "You have transformed into $1");
    }

    @SuppressWarnings("unchecked")
    public SkillResult use(Hero hero, LivingEntity target, String args[]) {
        Player player = hero.getPlayer();
        if (player == target) return SkillResult.INVALID_TARGET_NO_MSG;

        if (target instanceof Player) {
            if (canTransformInto(hero, PLAYER_CONFIG_KEY)) {
                Player targetPlayer = (Player) target;
                Disguise dis = new Disguise(dcApi.newEntityID(), targetPlayer.getName(), DisguiseType.Player);
                disguise(player, dis);
            } else {
                Messaging.send(player, cannotTransformIntoText, PLAYER_CONFIG_KEY);
            }
        } else {
            DisguiseType disType = null;
            for (Class curClass : mobMap.keySet()) {
                if (curClass.isAssignableFrom(target.getClass())) {
                    disType = mobMap.get(curClass);
                    break;
                }
            }
            if (disType != null) {
                if (canTransformInto(hero, disType.name())) {
                    disguise(player, new Disguise(dcApi.newEntityID(), disType));
                } else {
                    Messaging.send(player, cannotTransformIntoText, disType.name());
                }
            } else {
                return SkillResult.INVALID_TARGET;
            }
        }
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public void disguise(Player player, Disguise disg) {
        if (!dcApi.isDisguised(player)) {
            dcApi.disguisePlayer(player, disg);
        } else {
            dcApi.changePlayerDisguise(player, disg);
        }
        Messaging.send(player, transformedIntoText, disguiseToString(disg));
    }

    public String disguiseToString(Disguise disg) {
        return disg.type == DisguiseType.Player ? "Player " + disg.data.getFirst() : disg.type.name();
    }
}
