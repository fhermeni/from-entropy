package btrplace.fromEntropy;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 10/09/14.
 */
public class ConverterTest {

    @Test
    public void test() throws IOException {

        Converter.main(new String[]{
                "src/test/resources/r3-nr0-src.pbd",
                "src/test/resources/r3-nr0-dst.pbd",
                "src/test/resources/datacenter.btrp",
                "src/test/resources/clients",
                "-o", "src/test/resources/nr-r3-p5000-c33-0.json"
        });
    }

    @Test
    public void testGZip() throws IOException {

        Converter.main(new String[]{
                "src/test/resources/r3-nr0-src.pbd",
                "src/test/resources/r3-nr0-dst.pbd",
                "src/test/resources/datacenter.btrp",
                "src/test/resources/clients",
                "-o", "src/test/resources/nr-r3-p5000-c33-0.gz"
        });
    }
}