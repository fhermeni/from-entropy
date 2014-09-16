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
import btrplace.model.Attributes;
import btrplace.model.Instance;
import btrplace.model.Node;
import btrplace.model.VM;
import net.minidev.json.JSONObject;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * CLI to convert configurations.
 *
 * @author Fabien Hermenier
 */
public class Converter {

    public static void main(String[] args) {
        String src, dst = null, output, scriptDC = null, scriptCL = null;

        if (args.length < 3 || args.length > 6 || args.length == 5 || !args[args.length-2].equals("-o")) {
            usage(1);
        }
        src = args[0];
        output = args[args.length - 1];
        if (args.length > 3) {
            dst = args[1];
            if (args.length > 5) {
                scriptDC = args[2];
                scriptCL = args[3];
            }
        }

        OutputStreamWriter out = null;
        try {
            // Convert the src file
            ConfigurationConverter conv = new ConfigurationConverter(src);
            Instance i = conv.getInstance();

            //***** Set custom attributes (static) *******
            Attributes attrs = i.getModel().getAttributes();
            for (VM vm : i.getModel().getMapping().getAllVMs()) {
                // Hypervisor
                //attrs.put(vm, "template", "kvm");
                // Cannot be re-instantiate
                attrs.put(vm, "clone", false);
                // Actions
                //attrs.put(vm, "boot", 5);
                // => halt on entropy
                //attrs.put(vm, "shutdown", 2);
                attrs.put(vm, "forge", 3);
                // Migration duration: Memory / 10 :::: BUG (npe)
                //attrs.put(vm, "migrate",model.getAttributes().getInteger(vm, "memory")/10);
                attrs.put(vm, "suspend", 4);
                attrs.put(vm, "resume", 5);
                attrs.put(vm, "allocate", 5);
                attrs.put(vm, "kill", 2);

            }
            for (Node n : i.getModel().getMapping().getAllNodes()) {
                attrs.put(n, "boot", 6);
                attrs.put(n, "shutdown", 6);
            }
            //**********************************************/

            /* ReconfigurationAlgorithm global parameters
            ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
            cra.setTimeLimit(300);
            cra.doRepair(true);
            */

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

            // Read the client script file if exists
            if (scriptCL != null) {
                String strScriptCL = null;
                try {
                    strScriptCL = readFile(scriptCL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Script scrCL = null;
                try {
                    // Build the DC script
                    scrCL = scriptBuilder.build(strScriptCL);

                } catch (ScriptBuilderException sbe) {
                    System.out.println(sbe);
                }

                // Add the resulting constraints
                if (scrCL.getConstraints() != null) {
                    i.getSatConstraints().addAll(scrCL.getConstraints());
                }
            }

            // Convert to JSON
            InstanceConverter iConv = new InstanceConverter();
            JSONObject o = iConv.toJSON(i);

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
        System.out.println("Usage: converter src [dst] [scriptDC] [scriptCL] -o output");
        System.out.println("\tsrc: the configuration in protobuf format to convert");
        System.out.println("\tdst: an optional configuration that will be used to get the VMs and nodes state change");
        System.out.println("\tscriptDC: an optional btrpsl script file that describe the datacenter");
        System.out.println("\tscriptCL: an optional btrpsl script file that describe constraints");
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
