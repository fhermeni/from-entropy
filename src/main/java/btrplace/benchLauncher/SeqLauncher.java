package btrplace.benchLauncher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Created by vkherbac on 18/09/14.
 */
public class SeqLauncher {

    public static void main(String[] args) {

        //TODO: more arguments
        String inputFile = args[0];

        try
        {
            FileInputStream in = new FileInputStream(inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while((strLine = br.readLine())!= null)
            {
                String[] params = new String[4];
                params[0] = "--repair";
                params[1] = strLine;
                params[2] = "-o";
                params[3] = strLine.substring(0, strLine.lastIndexOf('.')) + ".csv";

                Launcher.main(params);
            }

        }catch(Exception e){
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
