package Classes;

// A static class containing variables that affect the entire program, such as resolution.
public class GameSettings {
    public static final long HIGHRES_MEMORY_CUTOFF = 402653184; // 348 megabytes (3*2^7) * 2^20
    public static final long LOWRES_MEMORY_CUTOFF = 268435456; // 256 megabytes (2^8 * 2^20)
    static ImageResolution imageResolution;

    public static void setImageResolution(ImageResolution resolution){
        imageResolution = resolution;
    }

    public static ImageResolution getImageResolution(){
        return imageResolution;
    }

    public enum ImageResolution {
        LOW, HIGH
    }

}


