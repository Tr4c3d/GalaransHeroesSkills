package mccity.heroes.skills.turret.integration;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import mccity.heroes.skills.turret.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class PluginsIntegration implements Runnable {

    private boolean checkTowny;
    private boolean checkFactions;

    private boolean usingTowny = false;
    private TownyUniverse townyUniv;
    private boolean usingFactions = false;

    public static final PluginsIntegration instance = new PluginsIntegration();

    private PluginsIntegration() { }

    public void init(boolean checkTowny, boolean checkFactions) {
        this.checkTowny = checkTowny;
        this.checkFactions = checkFactions;
    }

    public boolean isEnabled() {
        return checkTowny || checkFactions;
    }

    public boolean isFriendly(String playerName, Player targetPlayer) {
        if (!isEnabled()) return false;
        // debug
//        if (isTownyFriendly(playerName, targetPlayer)) {
//            Utils.log(targetPlayer.getName() + " is Towny friendly with " + playerName, Level.INFO);
//        }
//        if (isFactionsFriendly(playerName, targetPlayer)) {
//            Utils.log(targetPlayer.getName() + " is Factions friendly with " + playerName, Level.INFO);
//        }
        // debug - end

        return isTownyFriendly(playerName, targetPlayer) || isFactionsFriendly(playerName, targetPlayer);
    }

    private boolean isTownyFriendly(String playerName, Player targetPlayer) {
        if (!usingTowny) return false;

        Resident playerResident = townyUniv.getResidentMap().get(playerName.toLowerCase());
        Resident targetResident = townyUniv.getResidentMap().get(targetPlayer.getName().toLowerCase());
        if (playerResident == null || targetResident == null) return false;

        if (playerResident.hasFriend(targetResident)) return true;

        try {
            if (playerResident.hasTown() && targetResident.hasTown()) {
                Town playerTown = playerResident.getTown();
                Town targetTown = targetResident.getTown();
                if (playerTown == targetTown) {
                    return true;
                } else if (playerTown.hasNation() && targetTown.hasNation()) {
                    Nation playerNation = playerTown.getNation();
                    Nation targetNation = targetTown.getNation();
                    if (playerNation == targetNation || playerNation.hasAlly(targetNation)) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            // never happens
        }

        return false;
    }

    private boolean isFactionsFriendly(String playerName, Player targetPlayer) {
        if (!usingFactions) return false;

        FPlayer player = FPlayers.i.get(playerName);
        FPlayer target = FPlayers.i.get(targetPlayer.getName());

        if (player != null && target != null) {
            Faction playerFaction = player.getFaction();
            Faction targetFaction = target.getFaction();
            // isNone - is Wilderness
            if (playerFaction != null && targetFaction != null && !playerFaction.isNone() && !targetFaction.isNone()) {
                if (playerFaction == targetFaction || playerFaction.getRelationWish(targetFaction) == Relation.ALLY) {
                    return true;
                }
            }
        }

        return false;
    }

    /* Run after all plugins had been loaded */
    @Override
    public void run() {
        if (checkTowny) {
            Plugin townyPlugin = Bukkit.getPluginManager().getPlugin("Towny");
            if (townyPlugin != null && townyPlugin instanceof Towny) {
                townyUniv = ((Towny) townyPlugin).getTownyUniverse();
                usingTowny = true;
                Utils.log("Using Towny", Level.INFO);
            } else {
                Utils.log("Towny integration enabled in config, but there is no Towny", Level.INFO);
            }
        }

        if (checkFactions) {
            Plugin factionsPlugin = Bukkit.getPluginManager().getPlugin("Factions");
            if (factionsPlugin != null && factionsPlugin instanceof com.massivecraft.factions.P) {
                usingFactions = true;
                Utils.log("Using Factions", Level.INFO);
            } else {
                Utils.log("Factions integration enabled in config, but there is no Factions", Level.INFO);
            }
        }
    }
}
