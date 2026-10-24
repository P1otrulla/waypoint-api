package dev.piotrulla.waypoint.internal;

public record Position3d(double x, double y, double z) {

    public double distanceSquared(Position3d other) {
        double deltaX = this.x - other.x;
        double deltaY = this.y - other.y;
        double deltaZ = this.z - other.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public double distance(Position3d other) {
        return Math.sqrt(this.distanceSquared(other));
    }
}
