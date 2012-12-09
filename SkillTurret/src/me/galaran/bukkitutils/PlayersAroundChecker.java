package me.galaran.bukkitutils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayersAroundChecker {

    private final Plugin plugin;
    private final int period;
    private int taskId;

    private final List<Location> playersLoc = new ArrayList<Location>();

    private final Map<Location, Integer> minDistanceCache = new HashMap<Location, Integer>();

    public PlayersAroundChecker(Plugin plugin, int period) {
        this.plugin = plugin;
        this.period = period;
        taskId = -1;
    }

    public void startPolling(int delay) throws IllegalStateException {
        if (taskId != -1) {
            throw new IllegalStateException("Already started");
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new PlayersLocPoller(), delay, period);
    }

    /** Thread unsafe */
    public boolean isPlayerNearby(Location loc, int radius) {
        if (playersLoc.isEmpty()) {
            return false;
        }

        Integer cachedMinDist = minDistanceCache.get(loc);
        if (cachedMinDist == null) {
            int minDist = Integer.MAX_VALUE;
            for (Location playerLoc : playersLoc) {
                if (playerLoc.getWorld().equals(loc.getWorld())) {
                    int curDist = (int) playerLoc.distance(loc);
                    if (curDist < minDist) {
                        minDist = curDist;
                    }
                }
            }
            cachedMinDist = minDist;
            minDistanceCache.put(loc, cachedMinDist);
        }

        return cachedMinDist <= radius;
    }

    private class PlayersLocPoller implements Runnable {

        @Override
        public void run() {
            playersLoc.clear();
            minDistanceCache.clear();

            Player[] players = Bukkit.getServer().getOnlinePlayers();
            for (Player player : players) {
                playersLoc.add(player.getLocation());
            }
        }
    }
}
