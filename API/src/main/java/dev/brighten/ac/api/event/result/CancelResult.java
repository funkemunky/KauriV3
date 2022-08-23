package dev.brighten.ac.api.event.result;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CancelResult {
    private final boolean cancelled;
}
