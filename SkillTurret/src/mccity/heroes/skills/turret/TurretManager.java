package mccity.heroes.skills.turret;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Messaging;
import me.galaran.bukkitutils.PlayersAroundChecker;
import me.galaran.bukkitutils.TempEntityManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class TurretManager implements Runnable {

    private final TurretMap turretMap;

    private final SkillTurret skill;
    private final TurretStorage turretStorage;

    private final TempEntityManager eyeManager;
    private final PlayersAroundChecker playerChecker;

    private static final String PERM_BYPASS_PROTECTION = "skillturret.bypassprotection";

    public TurretManager(SkillTurret skill) {
        this.skill = skill;
        turretStorage = new TurretStorage(skill.plugin.getDataFolder());
        turretMap = new TurretMap();
        eyeManager = new TempEntityManager();

        playerChecker = new PlayersAroundChecker(skill.plugin, 50);
        playerChecker.startPolling(1);
    }

    private boolean isReplaceOldestOnLimitFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, skill, "replace-oldest-on-limit", false);
    }

    public boolean canAddFor(Hero hero) {
        List<Turret> playerTurrets = turretMap.getByPlayer(hero.getPlayer().getName());
        return (isReplaceOldestOnLimitFor(hero) || playerTurrets.size() < skill.maxTurretsFor(hero));
    }

    public int getTotalTurrets(Hero hero) {
        return turretMap.getByPlayer(hero.getPlayer().getName()).size();
    }

    public boolean addFor(Hero hero, Block base) {
        List<Turret> playerTurrets = turretMap.getByPlayer(hero.getPlayer().getName());

        boolean replace = false;
        if (playerTurrets.size() >= skill.maxTurretsFor(hero)) {
            if (isReplaceOldestOnLimitFor(hero)) {
                // remove oldest
                turretMap.removeByBase(playerTurrets.get(0).getBaseBlock());
                replace = true;
            } else {
                return false;
            }
        }

        turretMap.add(new Turret(hero, base, skill));
        if (replace) {
            Messaging.send(hero.getPlayer(), Messages.turretReplaced);
        }
        return true;
    }

    /**
     * @return cancel event ?
     */
    public boolean onDispenserDestroyed(Block dispenser, Player destroyer) {
        Turret turret = turretMap.getAt(dispenser);
        if (turret == null) return false;

        boolean isOwner = turret.getOwnerName().equals(destroyer.getName());

        if (isOwner || hasAccess(destroyer, turret)) {
            Turret destroyed = turretMap.removeByBase(dispenser);
            if (isOwner) {
                Messaging.send(destroyer, Messages.yourTurretDestroyed);
            } else {
                Messaging.send(destroyer, Messages.youDestroyOtherPlayerTurret, destroyed.getOwnerName());
                Utils.safeSend(destroyed.getOwnerName(), Messages.yourTurretDestroyedBy, destroyer.getName());
            }
            return false;
        } else {
            Messaging.send(destroyer, Messages.turretProtected);
            return true;
        }
    }

    /**
     * @return cancel event ?
     */
    public boolean onDispenserOpen(Block dispenser, Player player) {
        Turret turret = turretMap.getAt(dispenser);
        if (turret == null) return false;

        boolean isOwner = turret.getOwnerName().equals(player.getName());

        if (isOwner || hasAccess(player, turret)) {
            return false;
        } else {
            Messaging.send(player, Messages.turretProtected);
            return true;
        }
    }

    private boolean hasAccess(Player player, Turret turret) {
        return !turret.isProtected() || player.isOp() || player.hasPermission(PERM_BYPASS_PROTECTION);
    }

    public void removeAll(Hero hero) {
        turretMap.removeByPlayer(hero.getPlayer().getName());
    }

    @Override
    public void run() {
        eyeManager.update();
        if (turretMap.size() == 0) return;

        Collection<Turret> turrets = turretMap.getContent();
        List<Turret> forRemove = new ArrayList<Turret>();

        for (Turret curTurret : turrets) {
            if (curTurret.isAlive()) {
                if (curTurret.isChunkLoaded() && playerChecker.isPlayerNearby(curTurret.getBaseBlock().getLocation(), 60)) {
                    if (curTurret.checkBaseBlock()) {
                        curTurret.onAiTick(eyeManager);
                    } else {
                        forRemove.add(curTurret);
                    }
                }
            } else {
                forRemove.add(curTurret);
            }
        }

        for (Turret rTurret : forRemove) {
            turretMap.removeByBase(rTurret.getBaseBlock());
            Utils.safeSend(rTurret.getOwnerName(), Messages.yourTurretLostMagicPower);
        }
    }

    public void onPlayerJoin(Player player) {
        List<Turret> playerTurrets = turretMap.getByPlayer(player.getName());
        if (!playerTurrets.isEmpty()) {
            Hero owner = skill.plugin.getCharacterManager().getHero(player);
            for (Turret playerTurret : playerTurrets) {
                playerTurret.ownerJoined(owner);
            }
        }
    }

    public void onPlayerQuit(Player player) {
        List<Turret> playerTurrets = turretMap.getByPlayer(player.getName());
        for (Turret playerTurret : playerTurrets) {
            playerTurret.ownerQuited();
        }
    }

    public void loadData() {
        List<Turret> loaded = turretStorage.load();
        Utils.log(loaded.size() + " turrets loaded", Level.INFO);
        turretMap.setContent(loaded);
    }

    public void saveData() {
        turretStorage.save(turretMap.getContent());
    }

    public void despawnAllEyes() {
        eyeManager.despawnAll();
    }
}
