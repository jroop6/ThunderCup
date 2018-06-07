package Classes;

import Classes.Images.ButtonImages;
import Classes.Images.StaticBgImages;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by HydrusBeta on 8/10/2017.
 *
 * Here's how items are ordered in the contentsContainer. Let T = item of type T; || = spacer; S = special item:
 * || T || T || T || S S S
 */
public class ScrollableView<T extends Node> extends AnchorPane {

    // The contents of this ScrollableView:
    List<T> contents = new LinkedList<>();
    HBox contentsContainer = new HBox();
    Node background;
    Node midground;
    StaticBgImages spacerEnum = null;
    private int numSpecialItems = 0; // special items that are *not* of type T within the contentsContainer.

    // Values used for scrolling:
    private int scrollingDirection = 0; // -1 is left, 1 is right, 0 is no scroll.
    private long scrollStartTime; // nanoseconds
    private final double SCROLL_SPEED = 600; // pixels per second
    private final double MIDGROUND_RELTAIVE_SPEED = 0.9;
    private final double BACKGROUND_RELATIVE_SPEED = 0.3;
    private double foregroundScrollStartPos; // starting position when mouse enters the scroll button.
    private double backgroundScrollStartPos;
    private double midgroundScrollStartPos;

    // Note: the midground image must be flipped vertically (upside-down).
    // Todo: consider adding an automatic image flipper so that all assets can be oriented the natural way in the resources folder.
    public ScrollableView(Node background, Node midground) {
        // Place the background:
        this.background = background;
        getChildren().add(background);

        // Place the midground:
        this.midground = midground;
        midground.setScaleY(-1.0);
        getChildren().add(midground);
        setBottomAnchor(midground,0.0);

        // Put an initial spacer in the contentsContainer, so the scroll buttons don't overlap and obscure the important nodes:
        contentsContainer.getChildren().add(getSpacer());
        contentsContainer.setMaxHeight(Double.MAX_VALUE);
        contentsContainer.setFillHeight(true);
        getChildren().add(contentsContainer);

        // Create the scroll buttons:
        Button leftScrollButton = createScrollButton(false);
        Button rightScrollButton = createScrollButton(true);
        getChildren().addAll(leftScrollButton,rightScrollButton);
        setLeftAnchor(leftScrollButton,0.0);
        setRightAnchor(rightScrollButton,0.0);

        // Resize all child nodes appropriately when the ScrollableView is resized:
        Scale scaler = new Scale();
        leftScrollButton.getTransforms().add(scaler);
        rightScrollButton.getTransforms().add(scaler);
        contentsContainer.getTransforms().add(scaler);
        background.getTransforms().add(scaler);
        midground.getTransforms().add(scaler);
        heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                System.out.println("New height of ScrollableView: " + newValue);
                double scaleValue = (double)newValue/1080.0;
                scaler.setX(scaleValue);
                scaler.setY(scaleValue);
            }
        });
        setMinWidth(1.0); // The ScrollableView itself should grow to fill its parent Node.
        setMinHeight(1.0);

        // Activate the scroll functionality:
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateScroll(now);
            }
        };
        animationTimer.start();

//        background.setTranslateX(-contentsContainer.getBoundsInParent().getWidth()*BACKGROUND_RELATIVE_SPEED);
//        contentsContainer.setTranslateX(-contentsContainer.getBoundsInParent().getWidth());
//        midground.setTranslateX(-contentsContainer.getBoundsInParent().getWidth()*MIDGROUND_RELTAIVE_SPEED);

    }

    public ScrollableView(Node background, Node midground, StaticBgImages spacerEnum){
        this(background, midground);
        this.spacerEnum = spacerEnum;

        //replace the initial blank spacer with one from the spacerEnum type:
        contentsContainer.getChildren().remove(0);
        contentsContainer.getChildren().add(0,getSpacer());
    }

    private Node getSpacer(){
        if (spacerEnum == null) return new Rectangle(66,1080,Color.TRANSPARENT); // Exactly as wide as the scroll buttons
        else return spacerEnum.getImageView();
    }

    // Add an item of type <T> to the contentsContainer:
    public void addItem(T newItem){
        int index = contentsContainer.getChildren().size()-numSpecialItems;
        contentsContainer.getChildren().add(index,getSpacer());
        contentsContainer.getChildren().add(index,newItem);
        contents.add(newItem);
    }

    // Add an item of type <T> to the contentsContainer:
    public void addItems(Collection<T> newItems){
        int index = contentsContainer.getChildren().size()-numSpecialItems;
        for (T newItem: newItems) {
            contentsContainer.getChildren().add(index,getSpacer());
            contentsContainer.getChildren().add(index,newItem);
        }
        contents.addAll(newItems);
    }

    // Add an item of type <T> to a specified location in the contentsContainer:
    public void addItem(int index, T newItem){
        contentsContainer.getChildren().add(2*index,newItem);
        contentsContainer.getChildren().add(2*index,getSpacer());
        contents.add(index, newItem);
    }

    // Add a special item to the very end of the contentsContainer:
    public void addSpecialItem(Node newSpecialItem){
        contentsContainer.getChildren().add(newSpecialItem);
        ++numSpecialItems;
    }

    // If the item does not exist in the container, this method has no effect.
    public void removeItem(T item){
        if(!contents.contains(item)) return;
        int index = contentsContainer.getChildren().indexOf(item);
        contentsContainer.getChildren().remove(index); // remove the item
        contentsContainer.getChildren().remove(index); // remove the spacer after it.
        contents.remove(item);
    }

    // If the item does not exist in the container, this method has no effect.
    public void removeSpecialItem(T item){
        if(!contents.contains(item)) return;
        contentsContainer.getChildren().remove(item);
    }

    public List<T> getContents(){
        return contents;
    }

    private Button createScrollButton(boolean flipped){
        ImageView scrollImage = ButtonImages.SCROLL.getUnselectedImage();
        ImageView scrollImageEngaged = ButtonImages.SCROLL.getSelectedImage();
        Button scrollButton = new Button(null,scrollImage);
        if(flipped) scrollButton.setScaleX(-1.0);
        scrollButton.setBackground(null);
        scrollButton.setPadding(Insets.EMPTY);
        scrollButton.addEventHandler(MouseEvent.MOUSE_ENTERED,(event->{
            System.out.println("mouse is on " + (flipped?"right":"left") + "edge");
            scrollButton.setGraphic(scrollImageEngaged);
            scrollingDirection = flipped?-1:1;
            scrollStartTime = System.nanoTime();
            foregroundScrollStartPos = contentsContainer.getTranslateX();
            backgroundScrollStartPos = background.getTranslateX();
            midgroundScrollStartPos = midground.getTranslateX();
        }));
        scrollButton.addEventHandler(MouseEvent.MOUSE_EXITED,(event->{
            scrollButton.setGraphic(scrollImage);
            scrollingDirection = 0;
        }));
        return scrollButton;
    }

    // Called every frame to update the position of the contentsContainer.
    private void updateScroll(long now){
        if(scrollingDirection != 0){
            double additionalTranslation = SCROLL_SPEED*(now - scrollStartTime)/1000000000.0;
            // Don't let the view scroll beyond the left edge of the contents:
            if((foregroundScrollStartPos + scrollingDirection*additionalTranslation)>0) {
                contentsContainer.setTranslateX(0);
                background.setTranslateX(0);
                midground.setTranslateX(0);
            }
            // Don't let the view scroll beyond the right edge of the contents:
            else if((foregroundScrollStartPos + scrollingDirection*additionalTranslation)<-contentsContainer.getBoundsInParent().getWidth()){
                contentsContainer.setTranslateX(-contentsContainer.getBoundsInParent().getWidth());
                background.setTranslateX(-contentsContainer.getBoundsInParent().getWidth()*BACKGROUND_RELATIVE_SPEED);
                midground.setTranslateX(-contentsContainer.getBoundsInParent().getWidth()*MIDGROUND_RELTAIVE_SPEED);
            }
            else{
                contentsContainer.setTranslateX(foregroundScrollStartPos + scrollingDirection*additionalTranslation);
                background.setTranslateX(backgroundScrollStartPos + scrollingDirection*additionalTranslation*BACKGROUND_RELATIVE_SPEED);
                midground.setTranslateX(midgroundScrollStartPos + scrollingDirection*additionalTranslation*MIDGROUND_RELTAIVE_SPEED);
            }
        }
    }
}
