package mccity.heroes.skills.herbalismextra;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.listeners.HBlockListener;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SkillHerbalismExtra extends PassiveSkill {

    public SkillHerbalismExtra(Heroes plugin) {
        super(plugin, "HerbalismExtra");
        setDescription("You have a $1% chance to harvest Dead Bush from leaves and a $2% chance to harvest Fern from tall grass");
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.KNOWLEDGE, SkillType.EARTH, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("deadbush-chance-base", 0.1);
        node.set("deadbush-chance-per-level", 0.002);
        node.set("fern-chance-base", 0.1);
        node.set("fern-chance-per-level", 0.002);
        return node;
    }

    public String getDescription(Hero hero) {
        double chanceDeadbush = getChance(hero, "deadbush");
        double chanceFern = getChance(hero, "fern");

        return getDescription()
                .replace("$1", Util.stringDouble(Math.min(chanceDeadbush * 100, 100.0)))
                .replace("$2", Util.stringDouble(Math.min(chanceFern * 100, 100.0)));
    }

    private double getChance(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, key + "-chance-base", 0.1, false) +
                SkillConfigManager.getUseSetting(hero, this, key + "-chance-per-level", 0.002, false) * hero.getSkillLevel(this);
    }

    private boolean roll(Hero hero, String key) {
        return Util.nextRand() < getChance(hero, key);
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            Hero hero = SkillHerbalismExtra.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("HerbalismExtra")) return;

            Block block = event.getBlock();
            if (HBlockListener.placedBlocks.containsKey(block.getLocation())) return; // recently placed block

            Material extraMaterial;
            if (block.getType() == Material.LEAVES) {
                extraMaterial = Material.DEAD_BUSH;
            } else if (block.getType() == Material.LONG_GRASS && block.getData() == 1) {
                extraMaterial = Material.LONG_GRASS;
            } else {
                return;
            }

            ItemStack extraStack = null;
            if (extraMaterial == Material.DEAD_BUSH) {
                if (roll(hero, "deadbush")) extraStack = new ItemStack(Material.DEAD_BUSH, 1);
            } else if (extraMaterial == Material.LONG_GRASS) {
                if (roll(hero, "fern")) extraStack = new ItemStack(Material.LONG_GRASS, 1, (short) 2);
            } else {
                return;
            }

            if (extraStack != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), extraStack);
            }
        }
    }
}
