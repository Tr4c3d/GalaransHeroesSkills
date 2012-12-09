package mccity.heroes.skills.rockslide;

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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class SkillRockslide extends TargettedSkill implements Listener {

    private String useFailedText;

    public SkillRockslide(Heroes plugin) {
        super(plugin, "Rockslide");
        setDescription("Drops down a Rockslide on the target.");
        setUsage("/skill rockslide <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill rockslide");
        setTypes(SkillType.EARTH, SkillType.HARMFUL, SkillType.SILENCABLE);

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void init() {
        super.init();
        useFailedText = SkillConfigManager.getRaw(this, "use-failed-text", "Can't use here");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.COOLDOWN.node(), 30000);
        defaultConfig.set(Setting.MANA.node(), 30);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 10);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.1);
        defaultConfig.set(Setting.DURATION.node(), 2000);
        defaultConfig.set(Setting.DURATION_INCREASE.node(), 60);
        defaultConfig.set("radius", 2);
        defaultConfig.set("height", 3);
        defaultConfig.set("check-area-protection", true);
        defaultConfig.set("use-failed-text", "Can't use here");
        return defaultConfig;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription());

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 30000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 30, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        double distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 10, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE.node(), 0.1, false) * hero.getSkillLevel(this);
        if (distance > 0) {
            descr.append(" Dist:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Location rockslideLoc = target.getLocation().getBlock().getLocation();
        rockslideLoc.add(0.0, Math.ceil(target.getEyeHeight()) + 1, 0.0);

        int radius = SkillConfigManager.getUseSetting(hero, this, "radius", 2, false);
        radius = Math.max(0, Math.min(radius, 3));

        boolean checkAreaProtection = SkillConfigManager.getUseSetting(hero, this, "check-area-protection", true);
        if (checkAreaProtection) {
            boolean canBuild = canBuild(player, rockslideLoc.clone().add(-radius, 0, -radius));
            canBuild = canBuild && canBuild(player, rockslideLoc.clone().add(radius, 0 , radius));
            canBuild = canBuild && canBuild(player, rockslideLoc.clone().add(-radius, 0 , radius));
            canBuild = canBuild && canBuild(player, rockslideLoc.clone().add(radius, 0 , -radius));
            if (!canBuild) {
                Messaging.send(player, useFailedText);
                return SkillResult.FAIL;
            }
        }

        int height = SkillConfigManager.getUseSetting(hero, this, "height", 3, false);
        height = Math.max(1, Math.min(height, 5));

        Rockslide rockslide;
        try {
            rockslide = new Rockslide(rockslideLoc, radius, height);
        } catch (IllegalStateException ex) { // crossing other rockslide
            Messaging.send(player, useFailedText);
            return SkillResult.FAIL;
        }

        if (rockslide.hasNoBlocks()) {
            Messaging.send(player, useFailedText);
            return SkillResult.FAIL;
        }

        rockslide.launch();

        // schedule rollback
        int durationMs = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 2000, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE.node(), 60, false) * hero.getSkillLevel(this);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, rockslide, durationMs / 50);

        return SkillResult.NORMAL;
    }

    public boolean canBuild(Player player, Location testLoc) {
        Block testBlock = testLoc.getWorld().getBlockAt(testLoc);
        BlockPlaceEvent bpe = new BlockPlaceEvent(testBlock, testBlock.getState(), testBlock, player.getItemInHand(), player, true);
        Bukkit.getPluginManager().callEvent(bpe);
        return !bpe.isCancelled();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (Rockslide.columnsGlobal.isEmpty()) return;

        ItemStack dropStack = event.getEntity().getItemStack();
        if (dropStack.getType() == Material.GRAVEL && dropStack.getAmount() == 1) {
            WorldColumn dropColumn = new WorldColumn(event.getLocation());
            if (Rockslide.columnsGlobal.containsKey(dropColumn)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        if (event.getMaterial() != Material.GRAVEL || Rockslide.columnsGlobal.isEmpty() || !event.isBuildable()) return;

        Block replacingBlock = event.getBlock();
        Rockslide rockslide = Rockslide.columnsGlobal.get(new WorldColumn(replacingBlock));
        if (rockslide != null) {
            rockslide.addReplacingBlock(replacingBlock, new MaterialData(replacingBlock.getType(), replacingBlock.getData()));
        }
    }
}
