package stickfight2d.animation;


import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class FrameData {

    /**
     * Container which holds all the relevant information of a SINGLE frame
     */

    private int frameNumber;
    private Image image;
    private BufferedImage bufferedImage;
    private ArrayList<Point2D> hitBox;                  // Left and Right outline of the player
    private ArrayList<Point2D> hitBoxInverted;          // Same as hitBox but inverted
    private Point2D swordStartPoint, swordEndPoint;
    private Point2D swordStartPointInverted, swordEndPointInverted;
    private int angle = 0;

    public FrameData(BufferedImage bufferedImage) {
        this.image = convertToFxImage(bufferedImage);
        this.bufferedImage = bufferedImage;
    }

    public static Image convertToFxImage(BufferedImage image) {
        return SwingFXUtils.toFXImage(image, null);
    }

    public static void drawHorizontallyFlipped(GraphicsContext gc, Image image, int x, int y) {
        double width = image.getWidth();
        double height = image.getHeight();
        gc.drawImage(image, x + (width / 2), y, -width, height);
    }


    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        this.image = convertToFxImage(bufferedImage);
    }

    public Image getImage() {
        return image;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public ArrayList<Point2D> getHitBox() {
        return hitBox;
    }

    public void setHitBox(ArrayList<Point2D> hitBox) {
        this.hitBox = hitBox;
    }

    public ArrayList<Point2D> getHitBoxInverted() {
        return hitBoxInverted;
    }

    public void setHitBoxInverted(ArrayList<Point2D> hitBoxInverted) {
        this.hitBoxInverted = hitBoxInverted;
    }

    public Point2D getSwordStartPoint() {
        if(swordStartPoint != null){
            return swordStartPoint;
        }
        throw new IllegalArgumentException("There is no swordStartPoint in Frame "+frameNumber+"! Possible Issues: wrong color code / missing green dot in sprite");
    }

    public void setSwordStartPoint(Point2D swordStartPoint) {
        this.swordStartPoint = swordStartPoint;
    }

    public Point2D getSwordStartPointInverted(){
        if(swordStartPointInverted != null){
            return swordStartPointInverted;
        }
        throw new IllegalArgumentException("There is no swordStartPointInverted in Frame "+frameNumber+"!");
    }

    public void setSwordStartPointInverted(Point2D swordStartPointInverted) {
        this.swordStartPointInverted = swordStartPointInverted;
    }

    public Point2D getSwordEndPoint() {
        if(swordEndPoint != null){
            return swordEndPoint;
        }
        throw new IllegalArgumentException("Sword end Point is null in frame"+frameNumber+"!");
    }

    public void setSwordEndPoint(Point2D swordEndPoint) {
        this.swordEndPoint = swordEndPoint;
    }

    public Point2D getSwordEndPointInverted() {
        return swordEndPointInverted;
    }

    public void setSwordEndPointInverted(Point2D swordEndPointInverted) {
        this.swordEndPointInverted = swordEndPointInverted;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public String toString(){
        return "Frame="+frameNumber+", Angle="+angle;
    }

}
