package Classes.Images;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum MiscImages {
    SCROLL_SPACER("res/images/misc/ScrollBarSpacer.png"),
    PLAYPANEL_SPACER("res/images/misc/PlayPanelSpacer.png"),
    PLAYPANEL_LIVE_BOUNDARY("res/images/misc/PlayPanelLiveBoundary.png"),
    PLAYPANEL_SEPARATOR_1("res/images/misc/PlayPanelSeparator1.png");

    private Image image;

    MiscImages(String url){
        image = new Image(url);
    }

    public ImageView getImageView(){
        ImageView newImageView = new ImageView(image);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }
}
