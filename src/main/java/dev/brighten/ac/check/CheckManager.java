package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CheckManager {
    private final List<CheckStatic> checkClasses = new ArrayList<>();

    public CheckManager() {
        for (WrappedClass aClass : ClassScanner.getClasses(CheckData.class,
                "dev.brighten.ac.check.impl")) {
            addCheck(aClass);
        }
    }

    public void addCheck(WrappedClass checkClass) {
        CheckStatic check = new CheckStatic(checkClass);

        if(!check.getCheckClass().isAnnotationPresent(CheckData.class)) {
            return;
        }

        CheckData checkData = check.getCheckClass().getAnnotation(CheckData.class);

        Anticheat.INSTANCE.alog(true, "&7Adding check to CheckManager: " + checkData.name());

        checkClasses.add(check);
    }

    public boolean isCheck(String name) {
        final String formattedName = name.replace("_", " ");
        return checkClasses.stream().anyMatch(c -> c.getCheckClass().getAnnotation(CheckData.class).name()
                .equalsIgnoreCase(formattedName));
    }
}
