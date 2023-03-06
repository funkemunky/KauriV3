package dev.brighten.ac.handler.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class WrappedBlock {
    private Location location;
    private Material type;
    private byte data;
}
