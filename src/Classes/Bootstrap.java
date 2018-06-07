package Classes;

import java.io.*;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static Classes.GameSettings.HIGHRES_MEMORY_CUTOFF;
import static Classes.GameSettings.LOWRES_MEMORY_CUTOFF;

// Note for future reference: trying to allocate 2gb ram on 32-bit java version did not work. According to Oracle, the
// cutoff depends on several outside factors. It is often between 1.4gb and 1.6 gb, but can go as high as 4gb if running
// on a 64-bit system.

// todo: credit to Benjamin: http://www.silentsoftware.co.uk
// todo: if launching the program completely fails, display a message to the user.
// Ensures that the program is launching with enough memory, and redirects all output to a file.
public class Bootstrap {

    private static final boolean logOutput = false;

    public static void main(String[] args){

        // Retrieve the path to the jar file:
        String pathToJar = "";
        try {
            pathToJar = SceneManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch(URISyntaxException e){
            e.printStackTrace();
        }

        // Redirect output to a file in the same directory, so I can easily test the program on other machines and save
        // a log of the console output:
        File log = null;
        if(logOutput){
            int index = 0;
            do{
                log = new File(new File(pathToJar).getParent() + File.separator + "log" + index + ".txt");
                index++;
            }while(log.exists());
            try{
                PrintStream printStream = new PrintStream(new FileOutputStream(log));
                System.setOut(printStream);
                System.setErr(printStream);
            } catch(FileNotFoundException e){
                e.printStackTrace();
            }
            // log the machine name to the file
            try{
                System.out.println("************** " + InetAddress.getLocalHost().getHostName() + " **************");
            } catch(UnknownHostException e){
                System.out.println("************** (Could not determine machine's name) **************");
            }
        }

        // Determine the default max heap size that java has launched with:
        long max = Runtime.getRuntime().maxMemory();
        System.out.println("initial max heap = " + max);

        // If we already have enough memory, just launch the game:
        if(max >= HIGHRES_MEMORY_CUTOFF){
            System.err.println("we already have enough memory");
            SceneManager.main(args);
        }

        // If not, then try launching another instance, with more memory allocated to java:
        // Note: 1048576 is 2^20.
        else{
            if(!launchWithLargerHeap("-Xmx" + HIGHRES_MEMORY_CUTOFF/1048576 + "m", pathToJar, log)){
                System.err.println("Failed to launch java with enough memory to display highres graphics. Trying again with less memory...");
                if(!launchWithLargerHeap("-Xmx" + LOWRES_MEMORY_CUTOFF/1048576 + "m",pathToJar, log)){
                    System.err.println("Failed to launch java with sufficient memory");
                }
            }
        }
    }

    private static boolean launchWithLargerHeap(String xmxArgument, String pathToJar, File log){
        ProcessBuilder pb = new ProcessBuilder("java",xmxArgument, "-classpath", pathToJar, "Classes.SceneManager");
        if(logOutput && log != null){
            pb.redirectOutput(log);
            pb.redirectError(log);
        }
        else{
            pb.inheritIO();
        }
        try {
            Process p = pb.start();
            p.waitFor();
            System.out.println("exit value was " + p.exitValue());
            return p.exitValue()==0;
        } catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
        return false;
    }
}
