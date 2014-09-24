package btrplace.benchLauncher;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 16/09/14.
 */
public class LauncherTest {

    @Test
    public void test() throws IOException {

        Launcher.main(new String[]{"--repair", "src/test/resources/r3-c33-p5000-nr-0c.json",
                "-o", "src/test/resources/r3-c33-p5000-nr-0c.csv"});
    }

    @Test
    public void testGZip() throws IOException {

        Launcher.main(new String[]{"--repair", "src/test/resources/wkld-tdsc_p5000/r6-c33p5000-nr/0-c.gz",
                "-o", "src/test/resources/r3-c33-p5000-li-0c.csv"});
    }

    @Test
    public void testR6() throws IOException {
        Launcher.main(new String[]{"--repair",
                "src/test/resources/wkld-tdsc_p5000/r6-c100p5000-nr/0-c.gz",
                "-o", "src/test/resources/r6-c100-p5000-nr-0c.csv"
        });
    }
}
