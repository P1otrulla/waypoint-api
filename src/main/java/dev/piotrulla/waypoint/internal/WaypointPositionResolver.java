package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.Waypoint;

public final class WaypointPositionResolver {

    public ResolvedWaypointPosition resolve(Position3d eye, Waypoint waypoint) {
        double deltaX = waypoint.x() - eye.x();
        double deltaY = waypoint.y() - eye.y();
        double deltaZ = waypoint.z() - eye.z();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (distance <= waypoint.displayDistance()) {
            return new ResolvedWaypointPosition(new Position3d(waypoint.x(), waypoint.y(), waypoint.z()), distance);
        }

        double projectionFactor = waypoint.displayDistance() / distance;
        Position3d projected = new Position3d(
                eye.x() + deltaX * projectionFactor,
                eye.y() + deltaY * projectionFactor,
                eye.z() + deltaZ * projectionFactor
        );
        return new ResolvedWaypointPosition(projected, distance);
    }
}
