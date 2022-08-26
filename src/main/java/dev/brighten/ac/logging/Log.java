package dev.brighten.ac.logging;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class Log {
    public UUID uuid;
    private float vl;
    private long time;
    private String data;
    private String checkId;
}
