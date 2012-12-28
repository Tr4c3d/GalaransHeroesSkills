package mccity.heroes.skills.turret;

import org.bukkit.entity.Arrow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

    private static Method getArrowHandleMethod = null;
    
    public static Object getNMSArrow(Arrow arrow) throws Exception {
        if (getArrowHandleMethod == null) {
            getArrowHandleMethod = arrow.getClass().getMethod("getHandle");
        }
        return getArrowHandleMethod.invoke(arrow);
    }

    private static Field arrowDamageField = null;
    
    // arrow.getHandle().damage = damage
    public static void setArrowDamage(Arrow arrow, float damage) {
        try {
            Object nmsArrow = getNMSArrow(arrow);
            if (arrowDamageField == null) {
                arrowDamageField = nmsArrow.getClass().getDeclaredField("damage");
                arrowDamageField.setAccessible(true);
            }
            arrowDamageField.setDouble(nmsArrow, damage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static Field arrowFromPlayerField = null;

    // arrow.getHandle().fromPlayer = 0
    public static void setNotPickupable(Arrow arrow) {
        try {
            Object nmsArrow = getNMSArrow(arrow);
            if (arrowFromPlayerField == null) {
                arrowFromPlayerField = nmsArrow.getClass().getDeclaredField("fromPlayer");
                arrowFromPlayerField.setAccessible(true);
            }
            arrowFromPlayerField.setInt(nmsArrow, 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
