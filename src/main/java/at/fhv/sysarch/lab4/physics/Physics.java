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

    public Physics(){
        this.world = new World();
        this.world.setGravity(World.ZERO_GRAVITY);

        this.world.addListener(this);
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void begin(Step step, World world) {

    }

    @Override
    public void updatePerformed(Step step, World world) {

    }

    @Override
    public void postSolve(Step step, World world) {

    }

    @Override
    public void end(Step step, World world) {

    }

    @Override
    public void sensed(ContactPoint point) {

    }

    @Override
    public boolean begin(ContactPoint point) {

//        System.out.println("Contact!");
        return true;
    }

    @Override
    public void end(ContactPoint point) {

    }

    @Override
    public boolean persist(PersistedContactPoint point) {
        if (point.isSensor()) {
            Body body1 = point.getBody1();
            Body body2 = point.getBody2();

            boolean isBallPocketed;

            if (body1.getUserData() instanceof Ball) {
                isBallPocketed = PhysicsUtils.isBallPocketed(body1, body2, point);
            } else {
                isBallPocketed = PhysicsUtils.isBallPocketed(body2, body1, point);
            }

            Vector2 body1Vector = body1.getTransform().getTranslation();
            Vector2 body2Vector = body2.getTransform().getTranslation();



            Vector2 diffVector = body1Vector.difference(body2Vector);
            System.out.println(diffVector.getMagnitude());

//            System.out.println("x: " + diffVector.x);
//            System.out.println("y: " + diffVector.y);
//
//            if (diffVector.x >= 1.2) {
//                System.out.println("Pocketed");
//            } else {
//                System.out.println("Not Enough");
//            }
            double x2 = body2.getTransform().getTranslation().x;
            double y2 = body2.getTransform().getTranslation().y;



//            if (body1.getUserData() instanceof Ball) {
//
//            } else {
//
//            }
//
//            System.out.println("sensor");
        }
        return true;
    }

    @Override
    public boolean preSolve(ContactPoint point) {
        return true;
    }

    @Override
    public void postSolve(SolvedContactPoint point) {

    }
}
