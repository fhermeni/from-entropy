/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.fromEntropy;

import btrplace.btrpsl.Script;
import btrplace.btrpsl.ScriptBuilder;
import btrplace.btrpsl.ScriptBuilderException;
import btrplace.btrpsl.includes.BasicIncludes;
import btrplace.json.model.InstanceConverter;
import btrplace.model.Instance;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

/**
 * CLI to convert configurations.
 *
 * @author Fabien Hermenier
 */
public class Converter {

    public static void main(String[] args) {
        String src, dst = null, output, scriptDC = null, dirScriptsCL = null;

        if (args.length < 5 || args.length > 6 || !args[args.length-2].equals("-o")) { usage(1); }
        src = args[0];
        output = args[args.length - 1];
        if (args.length > 5) {
            dst = args[1];
        }
        scriptDC = args[args.length - 4];
        dirScriptsCL = args[args.length - 3];

        OutputStreamWriter out = null;
        try {
            // Convert the src file
            ConfigurationConverter conv = new ConfigurationConverter(src);
            Instance i = conv.getInstance();

            // Read the dst file, deduce and add the states constraints
            if (dst != null) {
                i.getSatConstraints().addAll(conv.getNextStates(dst));
            }

            // Read the script files
            ScriptBuilder scriptBuilder = new ScriptBuilder(i.getModel());
            //scriptBuilder.setIncludes(new PathBasedIncludes(scriptBuilder,
            //        new File("src/test/resources")));

            // Read the datacenter script file if exists
            if (scriptDC != null) {
                String strScriptDC = null;
                try {
                    strScriptDC = readFile(scriptDC);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Script scrDC = null;
                try {
                    // Build the DC script
                    scrDC = scriptBuilder.build(strScriptDC);

                } catch (ScriptBuilderException sbe) {
                    System.out.println(sbe);
                }

                // Set the DC script as an include
                BasicIncludes bi = new BasicIncludes();
                bi.add(scrDC);
                scriptBuilder.setIncludes(bi);
            }

            // Read all the client script files
            String scriptCL = null, strScriptCL = null;
            Script scrCL = null;
            Iterator it = FileUtils.iterateFiles(new File(dirScriptsCL), null, false);
            while(it.hasNext()) {
                scriptCL = dirScriptsCL + "/" + ((File) it.next()).getName();

                if (scriptCL != null) {
                    // Read
                    try {
                        strScriptCL = readFile(scriptCL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Parse
                    try {
                        scrCL = scriptBuilder.build(strScriptCL);

                    } catch (ScriptBuilderException sbe) {
                        System.out.println(sbe);
                        sbe.printStackTrace();
                    }

                    // Add the resulting constraints
                    if (scrCL.getConstraints() != null) {
                        i.getSatConstraints().addAll(scrCL.getConstraints());
                    }
                }
            }

            // Convert to JSON
            InstanceConverter iConv = new InstanceConverter();
            JSONObject o = iConv.toJSON(i);

            // Check for gzip extension
            if (output.endsWith(".gz")) {
                out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(output)));
            } else {
                out = new FileWriter(output);
            }

            // Write the output file
            o.writeJSONString(out);
            out.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
            }
        }
    }

    public static void usage(int code) {
        System.out.println("Usage: converter src [dst] scriptDC dirScriptsCL -o output");
        System.out.println("\tsrc: the configuration in protobuf format to convert");
        System.out.println("\tdst: an optional dst configuration in protobuf format");
        System.out.println("\tscriptDC: the btrpsl script file that describe the datacenter");
        System.out.println("\tdirScriptsCL: the directory where are located the client btrpsl script files");
        System.out.println("\toutput: the output JSON file. Ends with '.gz' for an automatic compression");
        System.exit(code);
    }

    /**
     * read a file
     *
     * @param fileName
     * @return the file content as a String
     * @throws IOException
     */
    private static String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
