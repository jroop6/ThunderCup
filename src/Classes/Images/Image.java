package Classes.Images;

import javafx.scene.image.ImageView;

public enum Image {

    BASIC_CANNON_BACKGROUND("res/animations/cannons/BasicCannonBackground_spritesheet.png"),
    BASIC_CANNON3_FOREGROUND("res/animations/cannons/BasicCannonForeground_spritesheet.png");

    private javafx.scene.image.Image image;

    Image(String url){
        image = new javafx.scene.image.Image(url);
    }

    public javafx.scene.image.Image getImage(){
        return image;
    }

    public ImageView getImageView(){
        ImageView newImageView = new ImageView(image);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }
}
