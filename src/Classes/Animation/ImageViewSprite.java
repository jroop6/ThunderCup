package Classes.Animation;

import javafx.scene.image.ImageView;

/** Note: I don't think this class will be used, after all. See notes in AnimationManager */
public class ImageViewSprite implements AnimationView {
    ImageView imageView;
    public ImageViewSprite(ImageView imageView){
        this.imageView = imageView;
    }
    public int drawFrame(AnimationData animationData){
        return 0;
    }
    public void setScale(double scale){

    }
    public void centerTheAnchorPoint(){

    }
}
