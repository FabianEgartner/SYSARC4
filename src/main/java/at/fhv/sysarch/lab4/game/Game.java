package at.fhv.sysarch.lab4.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.fhv.sysarch.lab4.physics.BallPocketedListener;
import at.fhv.sysarch.lab4.physics.BallsCollisionListener;
import at.fhv.sysarch.lab4.physics.ObjectsRestListener;
import at.fhv.sysarch.lab4.physics.Physics;
import at.fhv.sysarch.lab4.rendering.Renderer;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

public class Game implements BallPocketedListener, BallsCollisionListener, ObjectsRestListener {
    private final Renderer renderer;
    private Physics physics;

    // cue
    private Point2D startPointPhysical;
    private Point2D startPointScreen;

    // player
    private int currentPlayer = 1;
    private boolean hasAlreadyPlayed;

    private boolean ballsMoving;
    private boolean isFoul;
    private boolean whiteBallDidNotCollideWithOtherBall;

    public Game(Renderer renderer, Physics physics) {
        this.renderer = renderer;
        this.physics = physics;
        this.initWorld();

        physics.setBallPocketedListener(this);
        physics.setBallsCollisionListener(this);
        physics.setObjectsRestListener(this);
    }

    public void onMousePressed(MouseEvent e) {

        renderer.setStrikeMessage("");
        renderer.setActionMessage("");
        renderer.setFoulMessage("");

        if (ballsMoving)
            return;

        double x = e.getX();
        double y = e.getY();

        double pX = this.renderer.screenToPhysicsX(x);
        double pY = this.renderer.screenToPhysicsY(y);

        startPointScreen = new Point2D(x, y);
        startPointPhysical = new Point2D(pX, pY);

        renderer.setCueStartPoint(startPointScreen);
    }

    public void setOnMouseDragged(MouseEvent e) {

        if (ballsMoving)
            return;

        double x = e.getX();
        double y = e.getY();

        Point2D dragPointScreen = new Point2D(x, y);

        renderer.setCueEndPoint(dragPointScreen);
    }

    public void onMouseReleased(MouseEvent e) {

        if (ballsMoving)
            return;

        double x = e.getX();
        double y = e.getY();

        double pX = renderer.screenToPhysicsX(x);
        double pY = renderer.screenToPhysicsY(y);

        Point2D endPointPhysical = new Point2D(pX, pY);

        // create 2D vectors from start-/endpoint
        Vector2 start = new Vector2(startPointPhysical.getX(), startPointPhysical.getY());
        Vector2 end = new Vector2(endPointPhysical.getX(), endPointPhysical.getY());

        // difference (direction) between start-/endpoint
        Vector2 difference = start.difference(end);

        if (!difference.isZero()) {
            Ray ray = new Ray(start, difference);

            ArrayList<RaycastResult> results = new ArrayList<>();
            boolean result = this.physics.getWorld().raycast(ray, 0.1,false, false, results);

            if (result) {
                Body body = results.get(0).getBody();
                Ball ball = (Ball) body.getUserData();

                if (body.getUserData() instanceof Ball) {
                    body.applyImpulse(difference.multiply(15));
                    hasAlreadyPlayed = true;
                }

                if (ball != Ball.WHITE) {
                    this.isFoul = true;
                    this.decreasePlayerScore();
                    renderer.setFoulMessage("Foul! Striking only the white ball is allowed");
                }
                else
                    this.isFoul = false;
            }
        }

        renderer.removeCue();
    }

    private void placeBalls(List<Ball> balls) {
        Collections.shuffle(balls);

        // positioning the billard balls IN WORLD COORDINATES: meters
        int row = 0;
        int col = 0;
        int colSize = 5;

        double y0 = -2*Ball.Constants.RADIUS*2;
        double x0 = -Table.Constants.WIDTH * 0.25 - Ball.Constants.RADIUS;

        for (Ball b : balls) {
            double y = y0 + (2 * Ball.Constants.RADIUS * row) + (col * Ball.Constants.RADIUS);
            double x = x0 + (2 * Ball.Constants.RADIUS * col);

            b.setPosition(x, y);
            b.getBody().setLinearVelocity(0, 0);
            renderer.addBall(b);

            row++;

            if (row == colSize) {
                row = 0;
                col++;
                colSize--;
            }
        }
    }

    private void initWorld() {
        List<Ball> balls = new ArrayList<>();
        
        for (Ball b : Ball.values()) {
            if (b == Ball.WHITE)
                continue;

            balls.add(b);
            physics.getWorld().addBody(b.getBody());
        }
       
        this.placeBalls(balls);

        Ball.WHITE.setPosition(Table.Constants.WIDTH * 0.25, 0);
        physics.getWorld().addBody(Ball.WHITE.getBody());
        renderer.addBall(Ball.WHITE);
        
        Table table = new Table();
        physics.getWorld().addBody(table.getBody());
        renderer.setTable(table);

        renderer.setActionMessage("Player 1 starts");
    }

    @Override
    public void onBallPocketed(Ball b) {
        b.getBody().setLinearVelocity(0, 0);

        if (b == Ball.WHITE) {
            isFoul = true;
            renderer.setFoulMessage("Foul! White ball pocketed by Player " + currentPlayer + "!");
            this.decreasePlayerScore();

            this.moveWhiteBallToStartPosition();
        } else {
            renderer.removeBall(b);
            physics.getWorld().removeBody(b.getBody());

            if (!isFoul)
                this.increasePlayerScore();
        }
    }

    @Override
    public void onBallsCollide(Ball b1, Ball b2) {

        if (b1 == Ball.WHITE && b2 != Ball.WHITE || b1 != Ball.WHITE && b2 == Ball.WHITE)
            whiteBallDidNotCollideWithOtherBall = false;
    }

    @Override
    public void onEndAllObjectsRest() {
        ballsMoving = true;
    }

    @Override
    public void onStartAllObjectsRest() {

        if (whiteBallDidNotCollideWithOtherBall && hasAlreadyPlayed) {
            isFoul = true;
            this.decreasePlayerScore();
            renderer.setFoulMessage("Foul! White ball did not touch any other ball");
        }

        if (isFoul)
            this.switchPlayers();

        ballsMoving = false;
        whiteBallDidNotCollideWithOtherBall = true;
        isFoul = false;
        hasAlreadyPlayed = false;
    }

    private void switchPlayers() {

        if (currentPlayer == 1)
            currentPlayer = 2;
        else
            currentPlayer = 1;

        renderer.setActionMessage("Player " + currentPlayer + "'s Turn");
    }

    private void increasePlayerScore() {

        if (currentPlayer == 1)
            renderer.setPlayer1Score(renderer.getPlayer1Score() + 1);
        else
            renderer.setPlayer2Score(renderer.getPlayer2Score() + 1);

        renderer.setStrikeMessage("Player " + currentPlayer + " scored");
    }

    private void decreasePlayerScore() {

        if (currentPlayer == 1)
            renderer.setPlayer1Score(renderer.getPlayer1Score() - 1);
        else
            renderer.setPlayer2Score(renderer.getPlayer2Score() - 1);
    }

    private void moveWhiteBallToStartPosition() {
        Ball.WHITE.setPosition(Table.Constants.WIDTH * 0.25, 0);
    }
}