package btrplace.benchLauncher;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 16/09/14.
 */
public class LauncherTest {

    @Test
    public void testSrc() throws IOException {

        String[] params = new String[3];
        params[0] = "src/test/resources/0-src.json";
        params[1] = "-o";
        params[2] = "src/test/resources/stats_0-src.csv";
        //Launcher.main(params);
    }

    @Test
    public void testSrcDst() throws IOException {

        String[] params = new String[3];
        params[0] = "src/test/resources/0.json";
        params[1] = "-o";
        params[2] = "src/test/resources/stats_0.csv";
        //Launcher.main(params);
    }

    @Test
    public void testAll() throws IOException {

        String[] params = new String[3];
        params[0] = "src/test/resources/0-c33p5000-c9.json";
        params[1] = "-o";
        params[2] = "src/test/resources/stats_0-c33p5000-c9.csv";
        //Launcher.main(params);
    }
}
