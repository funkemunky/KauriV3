package dev.brighten.ac.utils.annotation;

import dev.brighten.ac.utils.Priority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Init {
    Priority priority() default Priority.NORMAL;
    String[] requirePlugins() default {};
    RequireType requireType() default RequireType.ALL;

    enum RequireType {
        ALL,
        ONE
    }
}