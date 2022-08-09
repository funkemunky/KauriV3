package dev.brighten.ac.utils;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class
CollisionHandler {
	private List<Block> blocks;
	private List<Entity> entities;
	private APlayer data;
	private KLocation location;
	private List<Triad<Double[], Integer, Consumer<Boolean>>> intersects = new ArrayList<>(),
			collides = new ArrayList<>();

	private double width, height;
	private double shift;
	@Setter
	private boolean single = false;
	@Setter
	private boolean debugging;

	public CollisionHandler(List<Block> blocks, Collection<Entity> entities, KLocation to, APlayer data) {
		this.blocks = blocks;
		this.entities = new ArrayList<>(entities);
		this.location = to;
		this.data = data;
	}

	public void setSize(double width, double height) {
		this.width = width;
		this.height = height;
	}

	public void setOffset(double shift) {
		this.shift = shift;
	}

	public boolean containsFlag(int bitmask) {
		for (Block b : blocks) {
			if (Materials.checkFlag(b.getType(), bitmask)) return true;
		}
		return false;
	}

	public boolean contains(EntityType type) {
		return entities.stream().anyMatch(e -> e.getType() == type);
	}

	public void intersectsWithFuture(int bitMask, Consumer<Boolean> intersects) {
		String bitMaskString = String.valueOf(bitMask) + "%%__NONCE__%%";
		this.intersects.add(new Triad<>(new Double[] {width, height, shift}, bitMask, intersects));
	}

	public void collidesWithFuture(int bitMask, Consumer<Boolean> collides) {
		this.collides.add(new Triad<>(new Double[] {width, height, shift}, bitMask, collides));
	}

	public boolean isCollidedWithEntity(SimpleCollisionBox box) {
		for(Entity entity : entities) {
			if(EntityData.getEntityBox(entity.getLocation(), entity).isCollided(box))
				return true;
		}
		return false;
	}

	public boolean isCollidedWithEntity() {
		SimpleCollisionBox playerBox = new SimpleCollisionBox()
				.offset(location.x, location.y, location.z)
				.expandMin(0, shift, 0)
				.expandMax(0, height, 0)
				.expand(width / 2, 0, width / 2);

		return isCollidedWithEntity(playerBox);
	}

	public List<CollisionBox> getCollisionBoxes(SimpleCollisionBox playerBox) {
		List<CollisionBox> collided = new ArrayList<>();

		for (Block b : blocks) {
			Material material = b.getType();

			CollisionBox box;
			if((box = BlockData.getData(material).getBox(b, ProtocolVersion.getGameVersion())).isCollided(playerBox)) {
				collided.add(box);
			}
		}

		for(Entity entity : entities) {
			CollisionBox box = EntityData.getEntityBox(entity.getLocation(), entity);
			if(box.isCollided(playerBox))
				collided.add(box);
		}

		return collided;
	}
	public List<CollisionBox> getCollisionBoxes() {
		SimpleCollisionBox playerBox = new SimpleCollisionBox()
				.offset(location.x, location.y, location.z)
				.expandMin(0, shift, 0)
				.expandMax(0, height, 0)
				.expand(width / 2, 0, width / 2);

		return getCollisionBoxes(playerBox);
	}

	public boolean isCollidedWith(CollisionBox box) {
		SimpleCollisionBox playerBox = new SimpleCollisionBox()
				.offset(location.x, location.y, location.z)
				.expandMin(0, shift, 0)
				.expandMax(0, height, 0)
				.expand(width / 2, 0, width / 2);

		return box.isCollided(playerBox);
	}

	public boolean isCollidedWith(SimpleCollisionBox playerBox, Material... materials) {
		for (Block b : blocks) {
			Location block = b.getLocation();
			Material material = b.getType();

			if (materials.length == 0 || MiscUtils.contains(materials, material)) {
				if (BlockData.getData(material).getBox(b, ProtocolVersion.getGameVersion()).isCollided(playerBox))
					return true;
			}
		}

		if(materials.length == 0) {
			for(Entity entity : entities) {
				if(EntityData.getEntityBox(entity.getLocation(), entity).isCollided(playerBox))
					return true;
			}
		}

		return false;
	}
	public boolean isCollidedWith(Material... materials) {
		SimpleCollisionBox playerBox = new SimpleCollisionBox()
				.offset(location.x, location.y, location.z)
				.expandMin(0, shift, 0)
				.expandMax(0, height, 0)
				.expand(width / 2, 0, width / 2);

		return isCollidedWith(playerBox, materials);
	}

	public void runFutures() {
		Triad<Double[], Integer, Consumer<Boolean>> value = null;
		Queue<Consumer<Boolean>> successful = new LinkedList<>();
		//To remove objects
		Queue<Triad<Double[], Integer, Consumer<Boolean>>> collisionRemove = new LinkedList<>(),
				intersectsRemove = new LinkedList<>();

		for (Block b : blocks) {
			Location block = b.getLocation();
			Material material = b.getType();
			for (Triad<Double[], Integer, Consumer<Boolean>> intersect : intersects) {
				if(!Materials.checkFlag(material, intersect.second)) continue;

				SimpleCollisionBox playerBox = new SimpleCollisionBox()
						.offset(location.x, location.y, location.z)
						.expandMin(0, intersect.first[2], 0)
						.expandMax(0, intersect.first[1], 0)
						.expand(intersect.first[0] / 2, 0, intersect.first[0] / 2);

				if (BlockData.getData(material).getBox(b, ProtocolVersion.getGameVersion()).isIntersected(playerBox)) {
					successful.add(intersect.third);
					intersectsRemove.add(intersect);
				}
			}
			for (Triad<Double[], Integer, Consumer<Boolean>> collides : collides) {
				if(!Materials.checkFlag(material, collides.second)) continue;

				SimpleCollisionBox playerBox = new SimpleCollisionBox()
						.offset(location.x, location.y, location.z)
						.expandMin(0, collides.first[2], 0)
						.expandMax(0, collides.first[1], 0)
						.expand(collides.first[0] / 2, 0, collides.first[0] / 2);

				if (BlockData.getData(material).getBox(b, ProtocolVersion.getGameVersion()).isCollided(playerBox)) {
					successful.add(collides.third);
					intersectsRemove.add(collides);
				}
			}

			while((value = intersectsRemove.poll()) != null) {
				intersects.remove(value);
			}
			while((value = collisionRemove.poll()) != null) {
				collides.remove(value);
			}
		}
		collides.clear();
		intersects.clear();

		Consumer<Boolean> consumer = null;
		while((consumer = successful.poll()) != null) {
			consumer.accept(true);
		}
	}
}
