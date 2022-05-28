package at.fhv.sysarch.lab4.physics.utils;

import at.fhv.sysarch.lab4.game.Ball;
import org.dyn4j.dynamics.contact.PersistedContactPoint;
import org.dyn4j.geometry.Vector2;

public class PhysicsUtils {

    public static boolean isBallPocketed(Vector2 ballPosition, Vector2 pocketPosition, PersistedContactPoint point) {
        // get pocket center
        Vector2 pocketCenter = point.getFixture2().getShape().getCenter();

        // get pocket position in world
        Vector2 pocketPositionInWorld = pocketPosition.add(pocketCenter);

        return ballPosition.difference(pocketPositionInWorld).getMagnitude() <= Ball.Constants.RADIUS;
    }
}
