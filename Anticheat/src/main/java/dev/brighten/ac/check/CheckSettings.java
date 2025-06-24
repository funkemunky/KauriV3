package dev.brighten.ac.check;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CheckSettings {
    private boolean enabled, punishable, cancellable;
    private int punishVl;

    public static CheckSettings settingsFromData(CheckData data) {
        return CheckSettings.builder().enabled(data.enabled())
                .punishable(data.punishable())
                .cancellable(data.cancellable())
                .punishVl(data.punishVl())
                .build();
    }

    @Override
    public String toString() {
        return "CheckSettings{" +
                "enabled=" + enabled +
                ", punishable=" + punishable +
                ", cancellable=" + cancellable +
                ", punishVl=" + punishVl +
                '}';
    }
}
