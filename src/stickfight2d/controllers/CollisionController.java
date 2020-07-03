package stickfight2d.controllers;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import stickfight2d.GameLoop;
import stickfight2d.enums.AnimationType;
import stickfight2d.enums.DirectionType;
import stickfight2d.enums.PlayerType;
import stickfight2d.misc.Debugger;
import stickfight2d.world.GameObject;
import stickfight2d.world.PlayerObject;
import stickfight2d.world.RectangleObstacle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollisionController extends Controller {

    // ----------------------------------------------------------------------------------------------------
    // --- Instance & Constructor

    private static CollisionController instance;

    public static CollisionController getInstance() {
        if (instance == null) {
            Debugger.log("Collision Controller instantiated");
            instance = new CollisionController();
        }
        return instance;
    }

    // --- World Data
    // --- --- Obstacle Data
    private final ArrayList<RectangleObstacle> obstacles = new ArrayList<>();
    // --- --- Player Data
    private final ArrayList<PlayerObject> players = new ArrayList<>();
    // --- --- --- Attack related data
    private final HashSet<AnimationType> nonStabAnimations = Stream.of(
            AnimationType.PLAYER_WALK,
            AnimationType.PLAYER_IDLE_HOLD_UP,
            AnimationType.PLAYER_JUMP_START,
            AnimationType.PLAYER_JUMP_PEAK,
            AnimationType.PLAYER_JUMP_END,
            AnimationType.PLAYER_DYING,
            AnimationType.SWORD).collect(Collectors.toCollection(HashSet::new));
    private static int swordLength = 0; // swordLength for sword-tip-calculation
    // --- --- --- Obstacle-Collision related data
    private final Point2D[] rectHitBoxP1_P2 = new Point2D[2]; // Contains upper left X,Y and bottom right X,Y of both players
    private final int[] playersWidthHeight = new int[2]; // Contains Width and Height of both players
    // ----------------------------------------------------------------------------------------------------

    // --- Player states
    // --- --- Obstacle related states
    private boolean player1_onGround = true;
    private boolean player1_hitsWall_Left = false;
    private boolean player1_hitsWall_Right = false;
    private boolean player1_headBump = false;
    private boolean player2_onGround = true;
    private boolean player2_hitsWall_Left = false;
    private boolean player2_hitsWall_Right = false;
    private boolean player2_headBump;
    // --- --- Attack related states
    private boolean player1_hit_player2 = false;
    private boolean player2_hit_player1 = false;
    private boolean swordsHitting = false;
    private boolean attackBlocked = false;
    private AnimationType p1_prevState = AnimationType.PLAYER_IDLE_MEDIUM;
    private AnimationType p2_prevState = AnimationType.PLAYER_IDLE_MEDIUM;
    private int disarming = 0;
    // ----------------------------------------------------------------------------------------------------

    private CollisionController() {
        for (GameObject obj : GameLoop.currentLevel.getGameObjects()) { // Collect PlayerObjects
            if (obj instanceof PlayerObject) {
                players.add((PlayerObject) obj);
                ((PlayerObject) obj).currentObstacleStanding = GameLoop.currentLevel.getGround();
            }

            if (obj instanceof RectangleObstacle) { // Collect RectangleObjects
                obstacles.add((RectangleObstacle) obj);
            }
        }

        fillPlayerRectangleHitBox();
    }

    /**
     * Updates information on player states
     */
    @Override
    public void update(long diffMillis) {
        updatePlayerObstacleCollisions(players.get(0));
        updatePlayerObstacleCollisions(players.get(1));

        player1_hit_player2 = collisionSwordAvatar(players.get(0), players.get(1));
        player2_hit_player1 = collisionSwordAvatar(players.get(1), players.get(0));

        swordsHitting = checkCollisionSwordSword();
        disarming = checkDisarm();
    }


    // ----------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------
    // --- Attack collisions

    /**
     * Returns true, if there is a collision between the sword of player1 and the character of player2
     * Logic >> Detects, where the swordTip-point (player1) is between two points of the player2-hitBox on the same y-level
     */
    private boolean collisionSwordAvatar(PlayerObject player1, PlayerObject player2) {
        if (nonStabAnimations.contains(player1.getAnimation().getAnimationType()) || player1.getSwordObject() == null) // Prevent horizontal stabbing in specific animations
            return false;

        Point2D swordTip;
        ArrayList<Point2D> hitBox_Player2;
        int offsetHitBox = 0;

        // Blocking
        Point2D player2_block;

        // Get relevant hitBox of player2 and swordTip-position of player1
        if (player2.getDirectionType() == DirectionType.RIGHT) { // --> player1 direction must be Direction.LEFT
            hitBox_Player2 = player2.getAnimation().getCurrentFrame().getHitBox();

            Point2D swordStartPoint = player1.getAnimation().getCurrentFrame().getSwordStartPointInverted();
            swordTip = new Point2D(player1.getX() + swordStartPoint.getX() - playersWidthHeight[0] - swordLength, player1.getY() + swordStartPoint.getY());
            player2_block = player2.getAnimation().getCurrentFrame().getSwordStartPoint();

        } else { // --> player1 direction must be Direction.RIGHT
            hitBox_Player2 = player2.getAnimation().getCurrentFrame().getHitBoxInverted();
            offsetHitBox = playersWidthHeight[0] + 2; // TODO :: has to be updated, if playerRotation-implementation is changed

            Point2D swordStartPoint = player1.getAnimation().getCurrentFrame().getSwordStartPoint();
            swordTip = new Point2D(player1.getX() + swordStartPoint.getX() + swordLength, player1.getY() + swordStartPoint.getY());
            player2_block = player2.getAnimation().getCurrentFrame().getSwordStartPointInverted();
        }

        // Blocking
        if (player2.getAnimation().getAnimationType() == AnimationType.PLAYER_IDLE_HOLD_UP) {
            if (swordTip.getY() <= player2.getY() + player2_block.getY() && swordTip.getY() >= player2.getY() + player2_block.getY() - swordLength) {

                if ((player2.getDirectionType() == DirectionType.RIGHT && swordTip.getX() <= player2.getX() + player2_block.getX())
                        || (player2.getDirectionType() == DirectionType.LEFT && swordTip.getX() >= player2.getX() + player2_block.getX() - offsetHitBox))
                    attackBlocked = true;
                else
                    attackBlocked = false;
            }
        }

        // Get points of interest for hitBox detection
        Point2D[] points = new Point2D[2];
        int idx = 0;
        for (Point2D p : hitBox_Player2) {
            if (swordTip.getY() == player2.getY() + p.getY()) { // X and Y must be ints
                points[idx++] = p;
                if (idx > 1)
                    break;
            }
        }

        if (idx == 0) { // No collision possible, sword isn't on the same y position as the player
            return false;
        }

        int firstSign = ((int) swordTip.getX() - ((int) points[0].getX() + player2.getX() - offsetHitBox));
        int secondSign = ((int) swordTip.getX() - ((int) points[1].getX() + player2.getX() - offsetHitBox));

        return firstSign * secondSign <= 0; // Hit: negative or zero ; Miss: positive
    }


    /**
     * returns true if the swords collide
     */
    private boolean checkCollisionSwordSword() {
        if (players.get(0).getSwordObject() == null || players.get(1).getSwordObject() == null)
            return false;

        Point2D swordTip1, swordGrip2, swordTip2;
        int offsetSword = 4; // TODO :: has to be updated, if playerRotation-implementation is changed

        if (players.get(0).getDirectionType() == DirectionType.RIGHT) { // --> p2 LEFT
            Point2D swordStartPoint = players.get(0).getAnimation().getCurrentFrame().getSwordStartPoint();
            swordTip1 = new Point2D(players.get(0).getX() + swordStartPoint.getX() + swordLength, players.get(0).getY() + swordStartPoint.getY());

            Point2D swordStartPoint2 = players.get(1).getAnimation().getCurrentFrame().getSwordStartPointInverted();
            swordTip2 = new Point2D(players.get(1).getX() + swordStartPoint2.getX() - 2 * swordLength + offsetSword, players.get(1).getY() + swordStartPoint2.getY());
            swordGrip2 = new Point2D(players.get(1).getX() + swordStartPoint2.getX() - swordLength + offsetSword, players.get(1).getY() + swordStartPoint2.getY());

        } else { // --> p2 RIGHT
            Point2D swordStartPoint = players.get(1).getAnimation().getCurrentFrame().getSwordStartPoint();
            swordTip1 = new Point2D(players.get(1).getX() + swordStartPoint.getX() + swordLength, players.get(1).getY() + swordStartPoint.getY());

            Point2D swordStartPoint2 = players.get(0).getAnimation().getCurrentFrame().getSwordStartPointInverted();
            swordTip2 = new Point2D(players.get(0).getX() + swordStartPoint2.getX() - 2 * swordLength + offsetSword, players.get(0).getY() + swordStartPoint2.getY());
            swordGrip2 = new Point2D(players.get(0).getX() + swordStartPoint2.getX() - swordLength + offsetSword, players.get(0).getY() + swordStartPoint2.getY());
        }

        // Offset of 2 pixels in each direction, assuming that the sword is 2-5 pixels wide
        boolean onSameY = (swordTip1.getY() - 2 <= swordTip2.getY() && swordTip2.getY() <= swordTip1.getY() + 2);
        boolean onSameXInterval = (swordTip1.getX() - swordTip2.getX()) * (swordTip1.getX() - swordGrip2.getX()) <= 0;

        return onSameY && onSameXInterval;
    }

    /**
     * Checks, if players should disarm each other
     * returns:     0 if no one's disarmed
     * 1 if player1 disarms player2
     * 2 if player2 disarms player1
     */
    private int checkDisarm() {
        if(players.get(0).getSwordObject() == null || players.get(1).getSwordObject() == null)
            return 0;

        AnimationType p1_anim = players.get(0).getAnimation().getAnimationType();
        AnimationType p2_anim = players.get(1).getAnimation().getAnimationType();
        int type = 0;

        if (!(p1_anim != p2_anim || p1_prevState == p2_prevState)) {
            // Invariant: p1 and p2 are in the same animation, one of both had a different previous animation
            if (!(p1_prevState == AnimationType.PLAYER_WALK || p2_prevState == AnimationType.PLAYER_WALK)) {

                if (p1_anim == p1_prevState && swordsHitting)
                    type = 2;

                else if (p2_anim == p2_prevState && swordsHitting)
                    type = 1;
            }
        }
        // Update player states
        p1_prevState = p1_anim;
        p2_prevState = p2_anim;
        return type;
    }


    // ----------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------
    // --- Obstacle collisions:

    private void updatePlayerObstacleCollisions(PlayerObject player) {
        boolean onGround = false;
        boolean hitsWallRight = false;
        boolean hitsWallLeft = false;
        boolean headBump = false;
        int pixelOffsetX = 8;
        int pixelOffsetX_2 = 10;
        int pixelOffsetY = 12;

        // Check Avatar-Obstacle collisions
        for (RectangleObstacle obstacle : obstacles) {
            if (obstacle.getColor() == Color.LIGHTGREY) // TODO :: Add boolean "canCollide" to RectangleObstacle
                continue;

            if (!collisionRectRect(player, obstacle, 0, 0, 0, 0)) // Rect-Rect collision
                continue; // Obstacle not near the player >> irrelevant for collision detection

            if (collisionRectRect(player, obstacle, pixelOffsetX, pixelOffsetX, playersWidthHeight[1], 0)) {// Rect-Line collision >> Ground
                onGround = true;
                player.currentObstacleStanding = obstacle;
            }

            if (collisionRectRect(player, obstacle, pixelOffsetX_2, pixelOffsetX_2, 0, playersWidthHeight[1])) {
                headBump = true;
            }

            if (collisionRectRect(player, obstacle, playersWidthHeight[0], 0, pixelOffsetY, pixelOffsetY))  // Rect-Line collision >> Wall-right
                hitsWallRight = true;
            else if (collisionRectRect(player, obstacle, 0, playersWidthHeight[0], pixelOffsetY, pixelOffsetY)) // Rect-Line collision >> Wall-left
                hitsWallLeft = true;

            if ((hitsWallRight || hitsWallLeft) && onGround) // States have been set, no need to continue
                break;
        }

        // Update states of player
        if (player.getPlayerNumber() == PlayerType.PLAYER_ONE) {
            player1_onGround = onGround;
            player1_hitsWall_Left = hitsWallRight;
            player1_hitsWall_Right = hitsWallLeft;
            player1_headBump = headBump;
        } else {
            player2_onGround = onGround;
            player2_hitsWall_Left = hitsWallRight;
            player2_hitsWall_Right = hitsWallLeft;
            player2_headBump = headBump;
        }
    }


    /**
     * Returns true, if there is a collision between the player and the given obstacle
     */
    private boolean collisionRectRect(PlayerObject player, RectangleObstacle obstacle, int x1, int x2, int y1, int y2) {
        return (player.getX() + x1 <= obstacle.getX() + obstacle.getWidth() // Checks, that rect2 is close enough from the left side
                && player.getX() + playersWidthHeight[0] - x2 >= obstacle.getX() // Checks, that rect2 is close enough from the right side
                && player.getY() + y1 <= obstacle.getY() + obstacle.getHeight() // Checks, --*-- from above
                && player.getY() + playersWidthHeight[1] - y2 >= obstacle.getY()); // Checks, --*-- from below
    }


    // ----------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------
    // ---  calcRectHitBox

    private void fillPlayerRectangleHitBox() {
        int x_min = Integer.MAX_VALUE, x_max = Integer.MIN_VALUE, y_min = Integer.MAX_VALUE, y_max = Integer.MIN_VALUE;
        ArrayList<Point2D> hitBoxPoints;

        hitBoxPoints = players.get(0).getAnimation().getCurrentFrame().getHitBox();
        for (Point2D currentPoint : hitBoxPoints) {

            if (currentPoint.getX() < x_min)
                x_min = (int) currentPoint.getX();
            else if (currentPoint.getX() > x_max)
                x_max = (int) currentPoint.getX();

            if (currentPoint.getY() < y_min)
                y_min = (int) currentPoint.getY();
            else if (currentPoint.getY() > y_max)
                y_max = (int) currentPoint.getY();
        }

        rectHitBoxP1_P2[0] = new Point2D(x_min, y_min);
        rectHitBoxP1_P2[1] = new Point2D(x_max, y_max);

        // Player 1
        playersWidthHeight[0] = (int) rectHitBoxP1_P2[1].getX() - (int) rectHitBoxP1_P2[0].getY();
        playersWidthHeight[1] = (int) rectHitBoxP1_P2[1].getY() - (int) rectHitBoxP1_P2[0].getY();
    }


    // ----------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------
    // --- Getter / Setter

    public static void setSwordLength(int length) { // static to allow call before Instance is constructed
        swordLength = length;
    }

    public int[] getPlayersWidthHeight() {
        return playersWidthHeight;
    }

    public boolean getPlayerOnGround(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player1_onGround : player2_onGround);
    }

    public boolean getPlayerHeadBump(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player1_headBump : player2_headBump);
    }

    public boolean getPlayerHitsWallLeft(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player1_hitsWall_Left : player2_hitsWall_Left);
    }

    public boolean getPlayerHitsWallRight(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player1_hitsWall_Right : player2_hitsWall_Right);
    }

    public boolean getPlayerHitOtherPlayer(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player1_hit_player2 : player2_hit_player1);
    }

    public boolean getPlayerHit(PlayerType type) {
        return ((type == PlayerType.PLAYER_ONE) ? player2_hit_player1 : player1_hit_player2);
    }

    public HashSet<AnimationType> getNonStabAnimations() {
        return nonStabAnimations;
    }

    public boolean getSwordsHitting() {
        return swordsHitting;
    }

    public boolean isAttackBlocked(){
        return attackBlocked;
    }

    public boolean getPlayerBeingDisarmed(PlayerType type) {
        if(disarming == 0)
            return false;

        return (type == PlayerType.PLAYER_TWO && disarming == 1) || (type == PlayerType.PLAYER_ONE && disarming == 2);
    }

    public int getSwordLength() {
        return swordLength;
    }

    public Point2D[] getRectHitBoxP1_P2() {
        return rectHitBoxP1_P2;
    }
}