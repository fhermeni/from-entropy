package btrplace.fromEntropy;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 10/09/14.
 */
public class ConverterTest {

    @Test
    public void test() throws IOException {

        String[] params = new String[6];
        params[0] = "src/test/resources/r3-nr0-src.pbd";
        params[1] = "src/test/resources/r3-nr0-dst.pbd";
        params[2] = "src/test/resources/datacenter.btrp";
        params[3] = "src/test/resources/clients";
        params[4] = "-o";
        params[5] = "src/test/resources/r3-nr0-c33-p5000.json";
        Converter.main(params);
    }

    @Test
    public void testGZip() throws IOException {

        String[] params = new String[6];
        params[0] = "src/test/resources/r3-nr0-src.pbd";
        params[1] = "src/test/resources/r3-nr0-dst.pbd";
        params[2] = "src/test/resources/datacenter.btrp";
        params[3] = "src/test/resources/clients";
        params[4] = "-o";
        params[5] = "src/test/resources/r3-nr0-c33-p5000.gz";
        Converter.main(params);
    }
}