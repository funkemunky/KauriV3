package dev.brighten.ac.utils;

import lombok.Getter;

@Getter
public enum Priority {
    LOWEST(1), LOW(2), NORMAL(3), HIGH(4), HIGHEST(5);

    int priority;

    Priority(int priority) {
        this.priority = priority;
    }
}
