package at.fhv.sysarch.lab4.physics;

import at.fhv.sysarch.lab4.game.Ball;
import at.fhv.sysarch.lab4.physics.utils.PhysicsUtils;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Step;
import org.dyn4j.dynamics.StepListener;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.contact.ContactListener;
import org.dyn4j.dynamics.contact.ContactPoint;
import org.dyn4j.dynamics.contact.PersistedContactPoint;
import org.dyn4j.dynamics.contact.SolvedContactPoint;
import org.dyn4j.geometry.Vector2;

public class Physics implements ContactListener, StepListener {
    private World world;

    // listener
    private BallPocketedListener ballPocketedListener;
    private BallsCollisionListener ballsCollisionListener;
    private ObjectsRestListener objectsRestListener;

    public Physics() {
        this.world = new World();
        this.world.setGravity(World.ZERO_GRAVITY);

        this.world.addListener(this);
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void begin(Step step, World world) {}

    @Override
    public void updatePerformed(Step step, World world) {
        // not implemented
    }

    @Override
    public void postSolve(Step step, World world) {
        // not implemented
    }

    @Override
    public void end(Step step, World world) {
        int ballsMoving = 0;

        for (Ball ball : Ball.values()) {
            if (!ball.getBody().getLinearVelocity().isZero())
                ballsMoving++;
        }

        if (ballsMoving > 0)
            objectsRestListener.onEndAllObjectsRest();
        else
            objectsRestListener.onStartAllObjectsRest();
    }

    @Override
    public void sensed(ContactPoint point) {
        // not implemented
    }

    @Override
    public boolean begin(ContactPoint point) {

        Body body1 = point.getBody1();
        Body body2 = point.getBody2();

        if (body1.getUserData() instanceof Ball && body2.getUserData() instanceof Ball) {
            ballsCollisionListener.onBallsCollide(
                    (Ball) body1.getUserData(),
                    (Ball) body2.getUserData()
            );
        }

        return true;
    }

    @Override
    public void end(ContactPoint point) {}

    @Override
    public boolean persist(PersistedContactPoint point) {
        if (point.isSensor()) {
            Body body1 = point.getBody1();
            Body body2 = point.getBody2();

            if (body1.getUserData() instanceof Ball) {
                Vector2 ballPosition = body1.getTransform().getTranslation();
                Vector2 pocketPosition = body2.getTransform().getTranslation();

                if (PhysicsUtils.isBallPocketed(ballPosition, pocketPosition, point)) {
                    ballPocketedListener.onBallPocketed((Ball) body1.getUserData());
                }
            } else {
                Vector2 ballPosition = body2.getTransform().getTranslation();
                Vector2 pocketPosition = body1.getTransform().getTranslation();

                if (PhysicsUtils.isBallPocketed(ballPosition, pocketPosition, point)) {
                    ballPocketedListener.onBallPocketed((Ball) body2.getUserData());
                }
            }
        }

        return true;
    }

    @Override
    public boolean preSolve(ContactPoint point) {
        return true;
    }

    @Override
    public void postSolve(SolvedContactPoint point) {
        // not implemented
    }

    public void removeBodyFromGame(Body body) {
        world.removeBody(body);
    }

    public void addBodyFromGame(Body body) {
        world.addBody(body);
    }

    public void setBallPocketedListener(BallPocketedListener ballPocketedListener) {
        this.ballPocketedListener = ballPocketedListener;
    }

    public void setBallsCollisionListener(BallsCollisionListener ballsCollisionListener) {
        this.ballsCollisionListener = ballsCollisionListener;
    }

    public void setObjectsRestListener(ObjectsRestListener objectsRestListener) {
        this.objectsRestListener = objectsRestListener;
    }
}
