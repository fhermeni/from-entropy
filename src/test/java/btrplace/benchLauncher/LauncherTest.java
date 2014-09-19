package btrplace.benchLauncher;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 16/09/14.
 */
public class LauncherTest {

    @Test
    public void test() throws IOException {

        String[] params = new String[4];
        params[0] = "--repair";
        params[1] = "src/test/resources/r3-nr0-c33-p5000.json";
        params[2] = "-o";
        params[3] = "src/test/resources/r3-nr0-c33-p5000.csv";
        Launcher.main(params);
    }

    @Test
    public void testGZip() throws IOException {

        String[] params = new String[4];
        params[0] = "--repair";
        params[1] = "src/test/resources/r3-nr0-c33-p5000.gz";
        params[2] = "-o";
        params[3] = "src/test/resources/r3-nr0-c33-p5000.gz.csv";
        Launcher.main(params);
    }
}
