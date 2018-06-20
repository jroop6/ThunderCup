package Classes;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Shape;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static Classes.PlayPanel.ROW_HEIGHT;

/**
 * A class for handling spritesheets. Includes methods for reading spritesheets and their metadata from disk and drawing
 * a specific frame on a given GraphicsContext. Each frame has its own "anchor point". When the program wishes to
 * display a frame from the spritesheet, it passes the desired location of the anchor point to the drawSprite method.
 * This allows for the frames to change in size (for example, if a character is swinging his/her arms while walking, the
 * frames may change in width) while still carefully controlling the position of the character to avoid jitter.
 */
public class SpriteSheet extends Image {

    private List<FrameBound> frameBounds = new ArrayList<>(); // Note: FrameBound is an inner class. Find it below.

    /**
     * Constructor used for spritesheets whose frames have variable sizes. The sprite sheet image file must be
     * accompanied by a metadata file describing the positions, dimensions, and anchor point locations of each sprite
     * image.
     * Todo: mention the SpriteCreatorUtility class here when it is ready.
     * @param spriteSheetURL The image file containing the spritesheet.
     */
    public SpriteSheet(String spriteSheetURL){
        super(spriteSheetURL);
        readSpriteMetadata(spriteSheetURL);
    }

    /**
     * Deprecated Constructor used for spritesheets whose frames are arranged on a regular grid. It is assumed that the
     *  sprites are arranged in the order you would read English text (left to right, then top to bottom). Anchor points
     *  are simply placed in the top-left corner of each frame.
     * @param spriteSheetURL The URL of the image file containing the spritesheet.
     * @param frameWidth The width of a single sprite
     * @param frameHeight The height of a single sprite
     */
    public SpriteSheet(String spriteSheetURL, int frameWidth, int frameHeight){
        super(spriteSheetURL);
        int numCols = (int)getWidth()/frameWidth;
        int numRows = (int)getHeight()/frameHeight;
        int numSprites = numCols*numRows;
        for(int i=0; i<numSprites; i++){
            int row = i/numCols;
            int col = i%numCols;
            Rectangle2D posAndDim = new Rectangle2D(col*frameWidth,row*frameHeight,frameWidth,frameHeight);
            Point2D anchorPoint = new Point2D(0, 0);
            FrameBound newFrameBound = new FrameBound(posAndDim, anchorPoint);
            frameBounds.add(newFrameBound);
        }
    }

    public void readSpriteMetadata(String spriteSheetURL){
        // Construct the metadata file url from the spritesheet url:
        String metaDataURL = spriteSheetURL.substring(0,spriteSheetURL.length()-15) + "metadata.csv";
        String line;
        try{
            InputStream stream = getClass().getClassLoader().getResourceAsStream(metaDataURL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            //BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(metaDataURL)));
            while((line = reader.readLine())!=null){
                String[] args =line.split(",");
                int frameX = Integer.parseInt(args[0]);
                int frameY = Integer.parseInt(args[1]);
                int frameWidth = Integer.parseInt(args[2]);
                int frameHeight = Integer.parseInt(args[3]);
                double anchorX = Double.parseDouble(args[4]);
                double anchorY = Double.parseDouble(args[5]);
                frameBounds.add(new FrameBound(new Rectangle2D(frameX, frameY, frameWidth, frameHeight), new Point2D(anchorX, anchorY)));
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public int getMaxFrameIndex(){
        return frameBounds.size()-1;
    }

    /**
     * A class for storing the position, dimensions, and anchor point location for a sprite in a Sprite. The
     * location of the anchorpoints are relative to the top-left corner of the sprite.
     */
    public class FrameBound {
        private Rectangle2D posAndDim;
        private Point2D anchorPoint;

        public FrameBound(Rectangle2D posAndDim, Point2D anchorPoint){
            this.posAndDim = posAndDim;
            this.anchorPoint = anchorPoint;
        }

        public Rectangle2D getPosAndDim() {
            return posAndDim;
        }
        public Point2D getAnchorPoint(){
            return anchorPoint;
        }
    }

    // For drawing with Sprites (ImageViews)
    public FrameBound getFrameBound(int index){
        // First, a sanity check:
        if(index >= frameBounds.size()){
            System.err.println("frame index " + index + " exceeds the maximum sprite index in spritesheet "
                    + this + ", which has only " + frameBounds.size() + "sprites. Note: frames are 0-indexed.");
            return new FrameBound(new Rectangle2D(0,0,1,1),new Point2D(0.5,0.5));
        }
        return frameBounds.get(index);
    }

    // For drawing directly on a GraphicsContext
    public void drawSprite(GraphicsContext graphicsContext, double anchorX, double anchorY, int frameIndex){
        // First, a sanity check
        if(frameIndex >= frameBounds.size()){
            System.err.println("frame index " + frameIndex + " exceeds the maximum sprite index in spritesheet "
                    + this + ", which has only " + frameBounds.size() + "sprites. Note: frames are 0-indexed.");
            return;
        }

        // Determine where the top-left corner of the sprite should be:
        Point2D anchorPoint = frameBounds.get(frameIndex).anchorPoint;

        double topLeftX = anchorX - anchorPoint.getX();
        double topLeftY = anchorY - anchorPoint.getY();

        // draw the sprite:
        Rectangle2D spritePosAndDim = frameBounds.get(frameIndex).posAndDim;
        graphicsContext.drawImage(this,
                spritePosAndDim.getMinX(),    // source x
                spritePosAndDim.getMinY(),    // source y
                spritePosAndDim.getWidth(),   // source width
                spritePosAndDim.getHeight(),  // source height
                topLeftX,                     // destination x
                topLeftY,                     // destination y
                spritePosAndDim.getWidth(),   // destination width
                spritePosAndDim.getHeight()); // destination height
    }
}
