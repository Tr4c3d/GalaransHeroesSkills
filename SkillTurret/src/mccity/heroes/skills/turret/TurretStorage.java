package mccity.heroes.skills.turret;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TurretStorage {

    private final File dataFile;

    private static final String ROOT_KEY = "turrets";

    public TurretStorage(File pluginFolder) {
        dataFile = new File(pluginFolder, "turrets.yml");
    }

    public List<Turret> load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> turretsData = config.getMapList(ROOT_KEY);

        List<Turret> result = new ArrayList<Turret>();
        if (turretsData != null) {
            for (Map<?, ?> dataEntry : turretsData) {
                try {
                    Turret curTurret = new Turret(dataEntry);
                    result.add(curTurret);
                } catch (Exception ex) {
                    Utils.log("Failed to load turret. Skipping it", Level.SEVERE);
                }
            }
        }
        return result;
    }

    public void save(Collection<Turret> turrets) {
        List<Map<String, Object>> turretsData = new ArrayList<Map<String, Object>>();
        for (Turret curTurret : turrets) {
            Map<String, Object> dataEntry = curTurret.getDataEntry();
            if (dataEntry != null) {
                turretsData.add(dataEntry);
            }
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set(ROOT_KEY, turretsData);

        try {
            config.save(dataFile);
        } catch (IOException ex) {
            Utils.log("Failed to save turrets data", Level.SEVERE);
            ex.printStackTrace();
        }
    }
}
