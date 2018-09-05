package Classes.Animation;

/** Note: I don't think this interface will be used, after all.  See notes in AnimationManager */
public interface AnimationView {
    int drawFrame(AnimationData animationData);
    void setScale(double scale);
}
