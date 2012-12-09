package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import mccity.heroes.skills.totem.types.RecoveryTotemBuilder;
import mccity.heroes.skills.totem.types.SlowdownTotemBuilder;
import mccity.heroes.skills.totem.types.TempestTotemBuilder;
import me.galaran.bukkitutils.skilltotem.GUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class SkillTotem extends ActiveSkill {

    public static SkillTotem ref;

    private final TotemManager totemManager;
    private MaterialData groundMat;
    private MaterialData sideMat;

    public SkillTotem(Heroes plugin) {
        super(plugin, "Totem");
        setIdentifiers("skill totem");
        setTypes(SkillType.SUMMON, SkillType.KNOWLEDGE, SkillType.UNBINDABLE);
        setArgumentRange(0, 0);
        ref = this;

        totemManager = new TotemManager(this, new TempestTotemBuilder(), new RecoveryTotemBuilder(), new SlowdownTotemBuilder());
    }

    @Override
    public void init() {
        super.init();
        groundMat = GUtils.parseMatData(SkillConfigManager.getRaw(this, "ground-mat", "17:3"), ":");
        sideMat = GUtils.parseMatData(SkillConfigManager.getRaw(this, "side-mat", "98:3"), ":");
        TotemStructure.init(groundMat, sideMat, totemManager.getBaseMats());

        Bukkit.getPluginManager().registerEvents(totemManager, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, totemManager, 0, 5);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        sendDescription(hero);
        return SkillResult.CANCELLED;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set("ground-mat", "17:3");
        section.set("side-mat", "98:3");
        section.set("drop-base-block-on-destroy", false);
        for (TotemBuilder curType : totemManager.getRegisteredTypes()) {
            section.set(curType.getName(), curType.getDefaultConfig());
        }
        Message.fillDefaults(section);
        return section;
    }

    public boolean isDropBaseBlock(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "drop-base-block-on-destroy", false);
    }

    public String getDescription(Hero hero) {
        return "You have the ability to place totems. For more info use /skill totem";
    }

    private void sendDescription(Hero hero) {
        Player player = hero.getPlayer();
        String x = ChatColor.WHITE + "X";
        String b = ChatColor.GOLD + "B";
        String g = ChatColor.DARK_AQUA + "G";
        Messaging.send(player, String.format("%s[ ] [%s%s] [ ]    %s%s - %s %s(Build this last)",
                ChatColor.GRAY, x, ChatColor.GRAY, x, ChatColor.WHITE,
                GUtils.matDataToStringReadable(sideMat, ChatColor.GREEN), ChatColor.RED));
        Messaging.send(player, String.format("%s[%s%s] [%s%s] [%s%s]    %s%s - %sbase",
                ChatColor.GRAY, x, ChatColor.GRAY, b, ChatColor.GRAY, x, ChatColor.GRAY,
                b, ChatColor.WHITE, ChatColor.BLUE));
        Messaging.send(player, String.format("%s[ ] [%s%s] [ ]    %s%s - %s",
                ChatColor.GRAY, g, ChatColor.GRAY, g, ChatColor.WHITE,
                GUtils.matDataToStringReadable(groundMat, ChatColor.GREEN)));
        Messaging.send(player, "");
        Messaging.send(player, "Totem types (by base):");

        for (TotemBuilder type : totemManager.getTypesFor(hero)) {
            Messaging.send(player, type.getDescriptionFor(hero));
        }

        Messaging.send(player, "");
        Messaging.send(player, ChatColor.GREEN + "Only one totem can be active at a time.");
        Messaging.send(player, ChatColor.GREEN + "All totem types shares same cooldown.");
    }
}
