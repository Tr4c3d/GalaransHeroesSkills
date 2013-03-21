package mccity.heroes.skills.poisonedgrass;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.Util;
import me.galaran.bukkitutils.BlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class SkillPoisonedGrass extends ActiveSkill implements Listener {

    private static final EnumSet<Material> plantBlocks = EnumSet.of(Material.GRASS, Material.LEAVES, Material.CACTUS, Material.MELON_BLOCK,
            Material.PUMPKIN);
    private static final EnumSet<Material> plants = EnumSet.of(Material.SAPLING, Material.LONG_GRASS, Material.DEAD_BUSH,
            Material.YELLOW_FLOWER, Material.RED_ROSE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.WHEAT,
            Material.SUGAR_CANE_BLOCK, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.VINE, Material.WATER_LILY);

    private final Map<BlockLocation, GrassPoisonTrap> trapBlocks = new HashMap<BlockLocation, GrassPoisonTrap>();

    private String trapPlacedText;
    private String trapPlacingFailedText;
    private String applyText;
    private String expireText;

    public SkillPoisonedGrass(Heroes plugin) {
        super(plugin, "PoisonedGrass");
        setDescription("Poison plants and grass blocks. Player (except group members), who touch it will be poisoned for $1s, taking $2 damage every $3s.");
        setUsage("/skill poisonedgrass");
        setArgumentRange(0, 0);
        setIdentifiers("skill poisonedgrass");
        setTypes(SkillType.ILLUSION, SkillType.DARK);

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void init() {
        super.init();
        trapPlacedText = SkillConfigManager.getRaw(this, "trap-placed-text", "$1 blocks poisoned");
        trapPlacingFailedText = SkillConfigManager.getRaw(this, "place-fail-text", "No plants to poison");
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "$1 have been poisoned by plant!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Poison expired");
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription()
                .replace("$1", Util.stringDouble(getDurationFor(hero) / 1000.0))
                .replace("$2", String.valueOf(getTickDamageFor(hero)))
                .replace("$3", Util.stringDouble(getPeriodFor(hero) / 1000.0))
        );

        if (isDispellableFor(hero)) {
            descr.append(" Poison is dispellable.");
        }

        double diameter = 1 + getRadiusFor(hero) * 2;
        descr.append(" Trap diameter:");
        descr.append(Util.formatDouble(diameter));

        descr.append(" Max Distance:");
        descr.append(Util.formatDouble(getMaxDistanceFor(hero)));

        double cdSec = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 10000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        return descr.toString();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(SkillSetting.REAGENT.node(), Material.SPIDER_EYE.getId());
        defaultConfig.set(SkillSetting.REAGENT_COST.node(), 1);
        defaultConfig.set(SkillSetting.COOLDOWN.node(), 10000);
        defaultConfig.set(SkillSetting.MANA.node(), 10);
        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        defaultConfig.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.12);

        defaultConfig.set(SkillSetting.RADIUS.node(), 1.0);
        defaultConfig.set(SkillSetting.RADIUS_INCREASE.node(), 0.03);

        defaultConfig.set(SkillSetting.DURATION.node(), 6000);
        defaultConfig.set(SkillSetting.DURATION_INCREASE.node(), 80);
        defaultConfig.set(SkillSetting.PERIOD.node(), 2000);
        defaultConfig.set(SkillSetting.DAMAGE_TICK.node(), 4.0);
        defaultConfig.set(SkillSetting.DAMAGE_INCREASE.node(), 0.08);
        defaultConfig.set("dispellable", true);

        defaultConfig.set("trap-placed-text", "$1 blocks poisoned");
        defaultConfig.set("place-fail-text", "No plants to poison");
        defaultConfig.set(SkillSetting.APPLY_TEXT.node(), "$1 have been poisoned by plant!");
        defaultConfig.set(SkillSetting.EXPIRE_TEXT.node(), "Poison expired");
        return defaultConfig;
    }

    public double getMaxDistanceFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10.0, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE, 0.12, false) * hero.getSkillLevel(this);
    }

    public int getRadiusFor(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 1.0, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0.03, false) * hero.getSkillLevel(this);
        return (int) Math.floor(Math.max(0.0, Math.min(radius, 4.0)));
    }

    public int getDurationFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 80, false) * hero.getSkillLevel(this);
    }
    
    public int getPeriodFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
    }

    public int getTickDamageFor(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 4.0, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.08, false) * hero.getSkillLevel(this));
    }

    public boolean isDispellableFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "dispellable", true);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Block targetBlock = player.getTargetBlock(null, (int) getMaxDistanceFor(hero));
        if (targetBlock == null) return SkillResult.INVALID_TARGET_NO_MSG;

        int radius = getRadiusFor(hero);
        Set<BlockLocation> blocksToPoison = new HashSet<BlockLocation>();
        for (int rl = 0; rl <= radius; rl++) {
            int ch = (int) Math.floor(Math.sqrt(radius * radius - rl * rl));
            for (int chl = 0; chl <= ch; chl++) {
                for (int y = targetBlock.getY() - 2; y <= targetBlock.getY() + 2; y++) {
                    for (int sectorIter = 0; sectorIter < 4; sectorIter++) {
                        Block curBlock = targetBlock.getWorld().getBlockAt(targetBlock.getX() + chl * ((sectorIter & 2) == 0 ? -1 : 1),
                                y, targetBlock.getZ() + rl * ((sectorIter & 1) == 0 ? -1 : 1));
                        Material curMat = curBlock.getType();
                        if (plantBlocks.contains(curMat) || plants.contains(curMat)) {
                            blocksToPoison.add(new BlockLocation(curBlock));
                        }
                    }
                }
            }
        }

        if (blocksToPoison.isEmpty()) {
            Messaging.send(player, trapPlacingFailedText);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        GrassPoisonTrap trap = new GrassPoisonTrap(this, getPeriodFor(hero), getDurationFor(hero),
                getTickDamageFor(hero), isDispellableFor(hero), player, blocksToPoison);
        for (BlockLocation poisonBlock : blocksToPoison) {
            trapBlocks.put(poisonBlock, trap);
        }
        Messaging.send(player, trapPlacedText, blocksToPoison.size());
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (trapBlocks.isEmpty()) return;
        BlockLocation legsLoc = new BlockLocation(event.getTo());
        GrassPoisonTrap trapLegs = trapBlocks.get(legsLoc);
        if (trapLegs != null) {
            stepOnTrap(trapLegs, event.getPlayer());
        } else {
            BlockLocation belowLoc = legsLoc.add(0, -1, 0);
            GrassPoisonTrap trapBelow = trapBlocks.get(belowLoc);
            if (trapBelow != null) {
                stepOnTrap(trapBelow, event.getPlayer());
            }
        }
    }

    public void stepOnTrap(GrassPoisonTrap trap, Player target) {
        Player player = trap.getApplier();
        if (!player.isOnline()) {
            removeTrap(trap);
            return;
        }
        if (player.getName().equals(target.getName())) return;
        if (!damageCheck(player, target)) return;

        Hero hero = plugin.getCharacterManager().getHero(player);
        Hero targetHero = plugin.getCharacterManager().getHero(target);

        HeroParty heroParty = hero.getParty();
        if (heroParty != null && heroParty.isPartyMember(targetHero)) return;

        if (targetHero.hasEffect("GrassPoison")) {
            GrassPoisonTrap prevEffect = (GrassPoisonTrap) targetHero.getEffect("GrassPoison");
            trap.setTickDamage(Math.max(trap.getTickDamage(), prevEffect.getTickDamage()));
            targetHero.removeEffect(prevEffect);
        }
        targetHero.addEffect(trap);

        Location effectLoc = target.getLocation().add(0, 0.8, 0);
        effectLoc.getWorld().playEffect(effectLoc, Effect.POTION_BREAK, 4);

        removeTrap(trap);
    }

    public void removeTrap(GrassPoisonTrap trap) {
        for (BlockLocation trapBlock : trap.getBlocks()) {
            trapBlocks.remove(trapBlock);
        }
    }

    public class GrassPoisonTrap extends PeriodicDamageEffect {

        private final Set<BlockLocation> blocks;

        public GrassPoisonTrap(Skill skill, long period, long duration, int tickDamage, boolean dispellable,
                               Player applier, Set<BlockLocation> blocks) {
            super(skill, "GrassPoison", period, duration, tickDamage, applier);
            this.blocks = Collections.unmodifiableSet(blocks);
            types.add(EffectType.POISON);
            if (dispellable) {
                types.add(EffectType.DISPELLABLE);
            }
        }

        public Set<BlockLocation> getBlocks() {
            return blocks;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            Messaging.send(hero.getPlayer(), expireText);
        }
    }
}
