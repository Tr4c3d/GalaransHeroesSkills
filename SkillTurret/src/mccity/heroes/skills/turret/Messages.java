package mccity.heroes.skills.turret;

import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.configuration.ConfigurationSection;

public class Messages {

    public static String turretPlacedBy;
    public static String turretReplaced;
    public static String alreadyHaveMaxTurrets;
    public static String yourTurretDestroyed;
    public static String yourTurretDestroyedBy;
    public static String youDestroyOtherPlayerTurret;
    public static String yourTurretLostMagicPower;
    public static String outOfAmmo;
    public static String turretProtected;

    public static void load(SkillTurret skill) {
        turretPlacedBy = SkillConfigManager.getRaw(skill, "turret-placed-by-text", "%player% build turret").replace("%player%", "$1");
        turretReplaced = SkillConfigManager.getRaw(skill, "turret-replaced-text", "Turret replaced");
        alreadyHaveMaxTurrets = SkillConfigManager.getRaw(skill, "max-turrets-text", "You already have max turrets");
        yourTurretDestroyed = SkillConfigManager.getRaw(skill, "your-turret-destroyed-text", "Your turret destroyed");
        yourTurretDestroyedBy = SkillConfigManager.getRaw(skill, "your-turret-destroyed-by-text", "Your turret destroyed by %player%").replace("%player%", "$1");
        youDestroyOtherPlayerTurret = SkillConfigManager.getRaw(skill, "you-destroy-other-player-turret-text", "You destroy %player%'s turret!").replace("%player%", "$1");
        yourTurretLostMagicPower = SkillConfigManager.getRaw(skill, "your-turret-lost-magic-power", "Your turret lost magic power");
        outOfAmmo = SkillConfigManager.getRaw(skill, "out-of-ammo-text", "Your turret is out of ammo");
        turretProtected = SkillConfigManager.getRaw(skill, "turret-protected-text", "This turret has protection for it's lifetime");
    }

    public static void fillDefaults(ConfigurationSection node) {
        node.set("turret-placed-by-text", "%player% build turret");
        node.set("turret-replaced-text", "Turret replaced");
        node.set("max-turrets-text", "You already have max turrets");
        node.set("your-turret-destroyed-text", "Your turret destroyed");
        node.set("your-turret-destroyed-by-text", "Your turret destroyed by %player%");
        node.set("you-destroy-other-player-turret-text", "You destroy %player%'s turret!");
        node.set("your-turret-lost-magic-power", "Your turret lost magic power");
        node.set("out-of-ammo-text", "Your turret is out of ammo");
        node.set("turret-protected-text", "This turret has protection for it's lifetime");
    }
}
