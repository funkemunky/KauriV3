package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import dev.brighten.ac.utils.math.IntVector;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WrappedBlock {
    private IntVector location;
    private StateType type;
    private WrappedBlockState blockState;
}
