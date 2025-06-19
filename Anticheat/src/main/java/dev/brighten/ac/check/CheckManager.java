package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import lombok.Getter;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class CheckManager {
    private final Map<String, CheckStatic> checkClasses = new HashMap<>();
    private final Map<String, String> idToName = new HashMap<>();
    private final Map<String, CheckSettings> checkSettings = new HashMap<>();
    public CheckManager() {
        synchronized (checkClasses) {
            for (WrappedClass aClass : new ClassScanner().getClasses(CheckData.class)) {
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

        Optional<CheckSettings> settings = getConfigSettings(checkData.checkId());

        if(settings.isEmpty()) {
            generateConfigSettings(checkData);
            settings = Optional.of(CheckSettings.settingsFromData(checkData));
        }
        checkSettings.put(checkData.checkId(), settings.get());

        checkClasses.put(checkData.name(), check);
        idToName.put(checkData.checkId(), checkData.name());
    }

    private Optional<CheckSettings> getConfigSettings(String checkId) {
        String basePath = "checks." + checkId + ".";
        if(Anticheat.INSTANCE.getAnticheatConfig().contains(basePath + "enabled")) {
            val config = Anticheat.INSTANCE.getAnticheatConfig();
            return Optional.of(CheckSettings.builder()
                    .enabled(config.getBoolean(basePath + "enabled"))
                    .punishable(config.getBoolean(basePath + "punishable"))
                    .cancellable(config.getBoolean(basePath + "cancellable"))
                    .punishVl(config.getInt(basePath + "punishVl"))
                    .build());
        }

        return Optional.empty();
    }

    public CheckSettings getCheckSettings(String checkId) {
        return checkSettings.get(checkId);
    }

    private void generateConfigSettings(CheckData data) {
        val config = Anticheat.INSTANCE.getAnticheatConfig();
        String basePath = "checks." + data.checkId() + ".";

        config.set(basePath + "enabled", data.enabled());
        config.set(basePath + "punishable", data.punishable());
        config.set(basePath + "cancellable", data.cancellable());
        config.set(basePath + "punishVl", data.punishVl());

        Anticheat.INSTANCE.saveConfig();
    }

    public boolean isCheck(String name) {
        final String formattedName = name.replace("_", " ");
        return checkClasses.containsKey(formattedName);
    }
}
