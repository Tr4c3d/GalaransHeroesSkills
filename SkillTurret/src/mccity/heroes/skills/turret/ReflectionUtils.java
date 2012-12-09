package mccity.heroes.skills.turret;

import net.minecraft.server.EntityArrow;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static void setArrowDamage(EntityArrow mcArrow, float damage) {
        try {
            Field damageField = mcArrow.getClass().getDeclaredField("damage");
            damageField.setAccessible(true);
            damageField.setDouble(mcArrow, damage);
        } catch (Exception ex) {
            System.out.println("Reflection exception: set arrow damage");
            ex.printStackTrace();
        }
    }
}
