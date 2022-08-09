package dev.brighten.ac.data.obj;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InstantAction {
    private final short startId, endId;
    private final boolean end;
    private final long stamp = System.currentTimeMillis();

}
