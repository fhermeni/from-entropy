package btrplace.fromEntropy;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 10/09/14.
 */
public class ConverterTest {
    @Test
    public void testSrc() throws IOException {

        String[] params = new String[3];
        params[0] = "src/test/resources/0-src.pbd";
        params[1] = "-o";
        params[2] = "src/test/resources/0-src.json";
        Converter.main(params);
    }

    @Test
    public void testSrcDst() throws IOException {

        String[] params = new String[4];
        params[0] = "src/test/resources/0-src.pbd";
        params[1] = "src/test/resources/0-dst.pbd";
        params[2] = "-o";
        params[3] = "src/test/resources/0.json";
        Converter.main(params);
    }

    @Test
    public void testAll() throws IOException {

        String[] params = new String[6];
        params[0] = "src/test/resources/0-src.pbd";
        params[1] = "src/test/resources/0-dst.pbd";
        params[2] = "src/test/resources/datacenter.btrp";
        params[3] = "src/test/resources/c9.btrp";
        params[4] = "-o";
        params[5] = "src/test/resources/0-c33p5000-c9.json";
        Converter.main(params);
    }

}
