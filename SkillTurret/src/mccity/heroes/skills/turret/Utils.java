package mccity.heroes.skills.turret;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class Utils {

    private static Heroes plugin;

    public static void init(Heroes heroes) {
        plugin = heroes;
    }

    public static boolean isInLineOfSight(Location pos1, Location pos2) {
        Vector direction = pos2.toVector().subtract(pos1.toVector());
        Iterator<Block> itr = new BlockIterator(pos1.getWorld(), pos1.toVector(), direction, 0, (int) direction.length());
        while (itr.hasNext()) {
            Block block = itr.next();
            if (block.getType().isSolid()) return false;
        }
        return true;
    }

    public static boolean tryConsumeItem(Inventory inv, Material mat) {
        int firstMatIndex = inv.first(mat);
        if (firstMatIndex != -1) {
            ItemStack targetStack = inv.getItem(firstMatIndex);
            if (targetStack.getAmount() == 1) {
                inv.setItem(firstMatIndex, null);
            } else {
                targetStack.setAmount(targetStack.getAmount() - 1);
            }
            return true;
        }
        return false;
    }

    public static Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> locData = new LinkedHashMap<String, Object>();
        locData.put("x", loc.getX());
        locData.put("y", loc.getY());
        locData.put("z", loc.getZ());
        locData.put("world", loc.getWorld().getName());
        return locData;
    }

    public static Location deserializeLocation(Map<?, ?> locData) {
        World world = Bukkit.getServer().getWorld((String) locData.get("world"));
        if (world == null) {
            throw new IllegalArgumentException("Non-existent world: " + locData.get("world"));
        }
        return new Location(world, (Double) locData.get("x"), (Double) locData.get("y"), (Double) locData.get("z"));
    }
    
    public static void safeSend(String playerName, String msg, Object... params) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            Messaging.send(player, msg, params);
        }
    }

    public static void log(String message, Level level) {
        plugin.getLogger().log(level, "[SkillTurret] " + message);
    }

    public static Hero getHero(Player player) {
        return plugin.getCharacterManager().getHero(player);
    }
}