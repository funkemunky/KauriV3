package dev.brighten.ac.check;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.api.check.CheckType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String name();
    String checkId();
    String description() default "A check that detects cheats";
    CheckType type();

    boolean enabled() default true;
    boolean punishable() default true;
    boolean cancellable() default true;
    boolean experimental() default false;
    int punishVl() default 10;

    ClientVersion minVersion() default ClientVersion.V_1_8;
    ClientVersion maxVersion() default ClientVersion.V_1_21_7;
}
