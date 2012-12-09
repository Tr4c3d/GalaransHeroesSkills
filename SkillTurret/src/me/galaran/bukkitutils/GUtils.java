package me.galaran.bukkitutils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

import java.util.LinkedHashMap;
import java.util.Map;

public class GUtils {

    public static void setBlockMatData(Block block, MaterialData matData) {
        block.setType(matData.getItemType());
        block.setData(matData.getData());
    }

    public static boolean isBlockMatchsMatData(Block block, MaterialData matData) {
        return block.getType() == matData.getItemType() && block.getData() == matData.getData();
    }

    public static String matDataToString(MaterialData materialData, String delimiter) {
        StringBuilder sb = new StringBuilder();
        sb.append(materialData.getItemTypeId());
        sb.append(delimiter);
        sb.append(materialData.getData());
        return sb.toString();
    }

    public static MaterialData parseMatData(String idData, String delimiter) {
        if (idData == null || idData.isEmpty()) return null;
        idData = idData.trim();

        MaterialData result;
        try {
            if (idData.contains(delimiter)) {
                String[] idAndData = idData.split(delimiter);
                if (idAndData.length < 2) return null;
                result = new MaterialData(Integer.parseInt(idAndData[0]), Byte.parseByte(idAndData[1]));
            } else {
                result = new MaterialData(Integer.parseInt(idData));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return result;
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

    public static boolean isChunkLoaded(Location loc) {
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }
}
