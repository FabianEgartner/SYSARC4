package at.fhv.sysarch.lab4.game;

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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game implements BallPocketedListener, BallsCollisionListener, ObjectsRestListener {
    private final Renderer renderer;
    private Physics physics;

    // table
    private Table table;

    // cue
    private Point2D startPointPhysical;
    private Point2D startPointScreen;

    // player
    private int currentPlayer = 1;
    private boolean hasAlreadyPlayed;

    // balls
    List<Ball> pocketedBallsInGame = new ArrayList<>();
    private int ballsPocketedInRound;
    private boolean ballsMoving;

    // fouls
    private boolean coloredBallHitFirstFoul = false;
    private boolean whiteBallPocketedFoul = false;
    private boolean noCollidingFoul = true;

    // testing only

    public Game(Renderer renderer, Physics physics) {
        this.renderer = renderer;
        this.physics = physics;
        this.initWorld();

        physics.setBallPocketedListener(this);
        physics.setBallsCollisionListener(this);
        physics.setObjectsRestListener(this);
    }

    public void onMousePressed(MouseEvent e) {

        // reset messages
        this.resetMessages();

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

        // ignore simple mouse clicks
        if (!difference.isZero()) {
            // ensure that balls stay on table
            if (difference.getMagnitude() < 1.35) {
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

                    if (ball != Ball.WHITE)
                        coloredBallHitFirstFoul = true;
                }
            } else {
                System.out.println("Aggression is no solution... :)");
            }
        }

        renderer.removeCue();
    }

    private void placeBalls(List<Ball> balls) {
        Collections.shuffle(balls);

        // positioning the billiard balls IN WORLD COORDINATES: meters
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
        
        this.table = new Table();
        physics.getWorld().addBody(table.getBody());
        renderer.setTable(table);

        renderer.setActionMessage("Player 1 starts the game!");
    }

    @Override
    public void onBallPocketed(Ball b) {
        b.getBody().setLinearVelocity(0, 0);

        if (b == Ball.WHITE) {
            whiteBallPocketedFoul = true;
        } else {
            // testing only
            pocketedBallsInGame.add(b);
            ballsPocketedInRound++;

            renderer.removeBall(b);
            physics.getWorld().removeBody(b.getBody());
        }
    }

    @Override
    public void onBallsCollide(Ball b1, Ball b2) {
        if (b1 == Ball.WHITE && b2 != Ball.WHITE || b1 != Ball.WHITE && b2 == Ball.WHITE)
            noCollidingFoul = false;
    }

    @Override
    public void onEndAllObjectsRest() {
        ballsMoving = true;
    }

    @Override
    public void onStartAllObjectsRest() {
        if (hasAlreadyPlayed) {
            // noCollidingFoul & whiteBallPocketedFoul - decrease player score by 1
            if (noCollidingFoul && whiteBallPocketedFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! No collision with other balls &\nwhite ball pocketed!");
            }

            // noCollidingFoul & coloredBallHitFirstFoul - decrease player score by 1
            else if (noCollidingFoul && coloredBallHitFirstFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! No collision with other balls &\ncolored ball hit first!");
            }

            // noCollidingFoul - decrease player score by 1
            else if (noCollidingFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! No collision with other balls!");
            }

            // coloredBallHitFirstFoul & whiteBallPocketedFoul - decrease player score by 1
            else if (coloredBallHitFirstFoul && whiteBallPocketedFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! Colored ball hit first &\nwhite ball pocketed!");
            }

            // coloredBallHitFirstFoul - decrease player score by 1
            else if (coloredBallHitFirstFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! Colored ball hit first!");
            }

            // whiteBallPocketedFoul - decrease player score by 1
            else if (whiteBallPocketedFoul) {
                this.decreasePlayerScore();
                renderer.setFoulMessage("Foul! White ball pocketed!");
            }

            // reset white ball position & switch players
            if (coloredBallHitFirstFoul || whiteBallPocketedFoul || noCollidingFoul) {
                this.moveWhiteBallToStartPosition();
                this.switchPlayers();
            } else if (ballsPocketedInRound == 0) {
                renderer.setStrikeMessage("No balls pocketed!");
                this.switchPlayers();
            } else {
                this.increasePlayerScoreByAmount(ballsPocketedInRound);
            }

            // reset fouls
            coloredBallHitFirstFoul = false;
            whiteBallPocketedFoul = false;
            noCollidingFoul = true;

            // reset balls
            ballsMoving = false;
            ballsPocketedInRound = 0;

            hasAlreadyPlayed = false;

            // reset game if no balls left
            if (pocketedBallsInGame.size() > 14) {
                this.resetGame();
            }
        }
    }

    private void switchPlayers() {
        if (currentPlayer == 1)
            currentPlayer = 2;
        else
            currentPlayer = 1;

        renderer.setActionMessage("Switching players: Player " + currentPlayer + " is now on!");
    }

    private void increasePlayerScoreByAmount(int amount) {
        if (currentPlayer == 1)
            renderer.setPlayer1Score(renderer.getPlayer1Score() + amount);
        else
            renderer.setPlayer2Score(renderer.getPlayer2Score() + amount);

        renderer.setStrikeMessage("Player " + currentPlayer + " scored " + amount + " point(s)!");
    }

    private void decreasePlayerScore() {
        if (currentPlayer == 1)
            renderer.setPlayer1Score(renderer.getPlayer1Score() - 1);
        else
            renderer.setPlayer2Score(renderer.getPlayer2Score() - 1);

        renderer.setStrikeMessage("Due to the foul(s), Player " + currentPlayer + " lost " + 1 + " point!");
    }

    private void moveWhiteBallToStartPosition() {
        Ball.WHITE.setPosition(Table.Constants.WIDTH * 0.25, 0);
    }

    private void resetGame() {

        // display winner score
        this.showWinner();

        // start new game or quit
        this.showNewGameDialog();

        // reset white ball & messages
        this.moveWhiteBallToStartPosition();
        this.resetMessages();

        // reset player scores
        renderer.setPlayer1Score(0);
        renderer.setPlayer2Score(0);

        renderer.setActionMessage("Player 1 starts the game!");
        currentPlayer = 1;

        // place (pocketed) balls & add to renderer
        this.placeBalls(pocketedBallsInGame);

        // add all (pocketed) balls to physics
        for (Ball b : pocketedBallsInGame) {
            physics.getWorld().addBody(b.getBody());
        }

        // reset table
        this.table = new Table();
        physics.removeBodyFromGame(table.getBody());
        physics.addBodyFromGame(table.getBody());
        renderer.setTable(table);

        // reset pocketedBalls
        pocketedBallsInGame.clear();
    }

    void showNewGameDialog() {
        JPanel panel = new JPanel();
        panel.setSize(new Dimension(200, 50));
        panel.setLayout(null);

        JLabel label = new JLabel("Do you want to start a new game?");
        label.setVerticalAlignment(SwingConstants.BOTTOM);
        label.setBounds(0, 0, 200, 30);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(label);

        UIManager.put("OptionPane.minimumSize", new Dimension(250, 100));
        int res = JOptionPane.showConfirmDialog(
                null, panel, "New Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // start new game
        if (res == 0)
            System.out.println("New Game started...");

        // quit game
        else if (res == 1)
            System.exit(0);
    }

    private void resetMessages() {
        renderer.setStrikeMessage("");
        renderer.setActionMessage("");
        renderer.setFoulMessage("");
    }

    private void showWinner() {
        int player1Score = this.renderer.getPlayer1Score();
        int player2Score = this.renderer.getPlayer2Score();

        if (player1Score > player2Score)
            this.renderer.setActionMessage("Player 1 wins!");
        else if (player2Score > player1Score)
            this.renderer.setActionMessage("Player 2 wins!");
        else
            this.renderer.setActionMessage("draw!");
    }
}