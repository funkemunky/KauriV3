package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CheckManager {
    private final Map<String, CheckStatic> checkClasses = new HashMap<>();
    private final Map<String, String> idToName = new HashMap<>();

    public CheckManager() {
        synchronized (checkClasses) {
            for (WrappedClass aClass : ClassScanner.getClasses(CheckData.class,
                    "dev.brighten.ac.check.impl")) {
                addCheck(aClass);
            }
        }
    }

    public void addCheck(WrappedClass checkClass) {
        CheckStatic check = new CheckStatic(checkClass);

        if(!check.getCheckClass().isAnnotationPresent(CheckData.class)) {
            return;
        }

        CheckData checkData = check.getCheckClass().getAnnotation(CheckData.class);

        Anticheat.INSTANCE.alog(true, "&7Adding check to CheckManager: " + checkData.name());

        checkClasses.put(checkData.name(), check);
        idToName.put(checkData.checkId(), checkData.name());
    }

    public boolean isCheck(String name) {
        final String formattedName = name.replace("_", " ");
        return checkClasses.containsKey(formattedName);
    }
}
