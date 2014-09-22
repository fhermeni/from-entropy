package btrplace.benchLauncher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Created by vkherbac on 18/09/14.
 */
public class SeqLauncher {

    public static void main(String[] args) {

        //TODO: to improve + more arguments
        String inputFile = null;
        Boolean repair = false;
        if (args.length < 1 || args.length > 2 || (args.length == 1 && args[0].equals("--repair"))) {
            usage(1);
        }
        if (args[0].equals("--repair")) { repair = true; inputFile = args[1]; }
        else { inputFile = args[0]; }

        try
        {
            FileInputStream in = new FileInputStream(inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while((strLine = br.readLine())!= null)
            {
                if (repair) {
                    Launcher.main(new String[]{"--repair", strLine, "-o",
                            strLine.substring(0, strLine.lastIndexOf('.')) + ".csv"});
                }
                else {
                    Launcher.main(new String[]{strLine, "-o",
                            strLine.substring(0, strLine.lastIndexOf('.')) + ".csv"});
                }
            }

        }catch(Exception e){
            e.printStackTrace();
            System.out.println(e);
        }
    }

    public static void usage(int code) {
        System.out.println("Usage: launcher [--repair] srcFile");
        System.out.println("\t--repair: option to enable the 'repair' feature");
        System.out.println("\tsrcFile: the json instance to read, it can also be compressed to a .gz file");
        System.exit(code);
    }
}
