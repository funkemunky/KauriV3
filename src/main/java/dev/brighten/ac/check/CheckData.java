package dev.brighten.ac.check;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.packet.ProtocolVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CheckData {
    String name();
    String checkId();
    CheckType type();

    boolean enabled() default true;
    boolean punishable() default true;
    boolean cancellable() default true;
    boolean experimental() default false;
    int punishVl() default 10;

    ProtocolVersion minVersion() default ProtocolVersion.V1_7;
    ProtocolVersion maxVersion() default ProtocolVersion.v1_19_1;
}
