package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Message {

    TOTEM_PLACED_BY("totem-placed-by", "Totem placed by %player%"),
    TOTEM_REPLACED("totem-replaced", "Totem replaced"),
    YOUR_TOTEM_LOST_MAGIC_POWER("totem-lost-magic-power", "Your totem lost magic power"),
    TOTEM_PROTECTED("totem-protected", "This totem has protection");

    private final String key;
    private final String defaultVal;
    private final String string;

    private static class ParamRegexp {
        public static final Pattern value = Pattern.compile("%.+?%");
    }

    private Message(String key, String defaultVal) {
        this.key = key + "-text";
        this.defaultVal = defaultVal;

        String value = SkillConfigManager.getRaw(SkillTotem.ref, key, defaultVal);
        Matcher m = ParamRegexp.value.matcher(value);
        int nextParamIndex = 1;
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String replacement = "\\$" + String.valueOf(nextParamIndex++); // escape $
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        string = sb.toString();
    }

    public static void fillDefaults(ConfigurationSection defaultConfig) {
        for (Message cur : Message.values()) {
            defaultConfig.set(cur.key, cur.defaultVal);
        }
    }

    public void send(Player player, String... params) {
        Messaging.send(player, string, params);
    }

    public void broadcast(Location source, String... params) {
        SkillTotem.ref.broadcast(source, string, params);
    }
}
