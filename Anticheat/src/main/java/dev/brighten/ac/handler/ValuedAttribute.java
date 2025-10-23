package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import lombok.Getter;
import lombok.Setter;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.Objects;

@Getter
public class ValuedAttribute {
    private final Attribute attribute;
    @Setter
    private WrapperPlayServerUpdateAttributes.Property property;
    private double value;

    public ValuedAttribute(Attribute attribute) {
        this.attribute = attribute;
        this.value = attribute.getDefaultValue();
    }

    public void updateAttribute(WrapperPlayServerUpdateAttributes.Property property) {
        this.property = property;

        double multiplier = 1, additional = 0, base = 0;
        for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : property.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADDITION -> additional += modifier.getAmount();
                case MULTIPLY_BASE -> base +=  modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplier*= (1 + modifier.getAmount());
            }
        }

        this.value = MathHelper.clamp_double((property.getValue() + additional) * (1 - base) * multiplier,
                attribute.getMinValue(), attribute.getMaxValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValuedAttribute that)) return false;
        return Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute);
    }
}
