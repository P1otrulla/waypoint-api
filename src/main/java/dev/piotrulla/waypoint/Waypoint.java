package dev.piotrulla.waypoint;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Objects;

public final class Waypoint {

    private static final double DEFAULT_DISPLAY_DISTANCE = 12.0;
    private static final double MIN_DISPLAY_DISTANCE = 2.0;
    private static final double MAX_DISPLAY_DISTANCE = 48.0;
    private static final float DEFAULT_SCALE = 1.0F;
    private static final double REACH_DISABLED = 0.0;
    private static final boolean DEFAULT_SHOW_DISTANCE = true;

    private final String id;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final Component label;
    private final double displayDistance;
    private final float scale;
    private final double reachRadius;
    private final boolean showDistance;

    private Waypoint(Builder builder) {
        this.id = builder.id;
        this.worldName = builder.worldName;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.label = builder.label;
        this.displayDistance = builder.displayDistance;
        this.scale = builder.scale;
        this.reachRadius = builder.reachRadius;
        this.showDistance = builder.showDistance;
    }

    public static Builder builder(String id, Location location) {
        return new Builder(id, location);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public String id() {
        return this.id;
    }

    public String worldName() {
        return this.worldName;
    }

    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public Component label() {
        return this.label;
    }

    public double displayDistance() {
        return this.displayDistance;
    }

    public float scale() {
        return this.scale;
    }

    public double reachRadius() {
        return this.reachRadius;
    }

    public boolean showDistance() {
        return this.showDistance;
    }

    public boolean isInWorld(World world) {
        return world != null && this.worldName.equals(world.getName());
    }

    public boolean isReachedAt(double distance) {
        return this.reachRadius > REACH_DISABLED && distance <= this.reachRadius;
    }

    public static final class Builder {

        private final String id;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private Component label;
        private double displayDistance = DEFAULT_DISPLAY_DISTANCE;
        private float scale = DEFAULT_SCALE;
        private double reachRadius = REACH_DISABLED;
        private boolean showDistance = DEFAULT_SHOW_DISTANCE;

        private Builder(String id, Location location) {
            Objects.requireNonNull(location, "location");
            World world = Objects.requireNonNull(location.getWorld(), "location.world");

            this.id = requireNotBlank(id);
            this.worldName = world.getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.label = Component.text(this.id);
        }

        private Builder(Waypoint source) {
            this.id = source.id;
            this.worldName = source.worldName;
            this.x = source.x;
            this.y = source.y;
            this.z = source.z;
            this.label = source.label;
            this.displayDistance = source.displayDistance;
            this.scale = source.scale;
            this.reachRadius = source.reachRadius;
            this.showDistance = source.showDistance;
        }

        public Builder label(Component label) {
            this.label = Objects.requireNonNull(label, "label");
            return this;
        }

        public Builder displayDistance(double displayDistance) {
            this.displayDistance = displayDistance;
            return this;
        }

        /**
         * Ignored by the armor stand fallback renderer (1.16 - 1.19.3).
         */
        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder reachRadius(double reachRadius) {
            this.reachRadius = reachRadius;
            return this;
        }

        public Builder showDistance(boolean showDistance) {
            this.showDistance = showDistance;
            return this;
        }

        public Waypoint build() {
            this.validate();
            return new Waypoint(this);
        }

        private void validate() {
            if (!Double.isFinite(this.x) || !Double.isFinite(this.y) || !Double.isFinite(this.z)) {
                throw new IllegalArgumentException("Waypoint '" + this.id + "' has non-finite coordinates");
            }
            if (this.displayDistance < MIN_DISPLAY_DISTANCE || this.displayDistance > MAX_DISPLAY_DISTANCE) {
                throw new IllegalArgumentException("displayDistance must be in <" + MIN_DISPLAY_DISTANCE + ", " + MAX_DISPLAY_DISTANCE + ">, got " + this.displayDistance);
            }
            if (!(this.scale > 0.0F) || !Float.isFinite(this.scale)) {
                throw new IllegalArgumentException("scale must be positive and finite, got " + this.scale);
            }
            if (this.reachRadius < REACH_DISABLED || !Double.isFinite(this.reachRadius)) {
                throw new IllegalArgumentException("reachRadius must be >= 0 and finite, got " + this.reachRadius);
            }
        }

        private static String requireNotBlank(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Waypoint id cannot be null or blank");
            }
            return id;
        }
    }
}
