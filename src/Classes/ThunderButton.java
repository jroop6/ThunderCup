package Classes;

import Classes.Images.ButtonType;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public class ThunderButton extends Button {

    public ThunderButton(ButtonType buttonType, EventHandler<ActionEvent> clickEventHandler){
        setBackground(null);
        setButtonType(buttonType);
        setOnAction(clickEventHandler);
    }

    public void setButtonType(ButtonType buttonType){
        setGraphic(buttonType.getUnselectedImageView());
        setEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> setGraphic(buttonType.getSelectedImageView()));
        setEventHandler(MouseEvent.MOUSE_EXITED, (event) -> setGraphic(buttonType.getUnselectedImageView()));
    }
}
