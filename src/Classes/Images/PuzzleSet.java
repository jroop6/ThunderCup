package Classes.Images;

public enum PuzzleSet {

    PUZZLE_SET_1(ButtonType.PUZZLE_SET_1,1),
    PUZZLE_SET_2(ButtonType.PUZZLE_SET_2,2);

    private ButtonType buttonType;
    private int puzzleGroupIndex;

    PuzzleSet(ButtonType buttonType, int puzzleGroupIndex){
        this.buttonType = buttonType;
        this.puzzleGroupIndex = puzzleGroupIndex;
    }

    public ButtonType getButtonType(){
        return buttonType;
    }

    public int getPuzzleGroupIndex(){
        return puzzleGroupIndex;
    }
}
