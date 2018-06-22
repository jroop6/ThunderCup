package Classes.Images;

import javafx.scene.image.ImageView;

public enum PuzzleSetSelectorImages {

    PUZZLE_SET_1("res/images/buttons/PUZZLE_SET_1.png","res/images/buttons/PUZZLE_SET_1_SEL.png",1),
    PUZZLE_SET_2("res/images/buttons/PUZZLE_SET_1.png","res/images/buttons/PUZZLE_SET_1_SEL.png",2);


    private String unselectedImageUrl;
    private String selectedImageUrl;
    private int puzzleGroupIndex;

    PuzzleSetSelectorImages(String unselectedImageUrl, String selectedImageUrl, int puzzleGroupIndex){
        this.unselectedImageUrl = unselectedImageUrl;
        this.selectedImageUrl = selectedImageUrl;
        this.puzzleGroupIndex = puzzleGroupIndex;
    }

    public ImageView getUnselectedImage(){
        ImageView newImageView = new ImageView(unselectedImageUrl);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }

    public ImageView getSelectedImage(){
        ImageView newImageView = new ImageView(selectedImageUrl);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }

    public int getPuzzleGroupIndex(){
        return puzzleGroupIndex;
    }
}
