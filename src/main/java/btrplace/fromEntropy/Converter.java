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

import btrplace.model.Instance;
import btrplace.model.InstanceConverter;
import net.minidev.json.JSONObject;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

/**
 * CLI to convert configurations.
 *
 * @author Fabien Hermenier
 */
public class Converter {

    public static void main(String[] args) {
        String src, dst = null, output;
        if (args.length < 3) {
            usage(1);
        }
        src = args[0];
        output = args[args.length - 1];
        if (!args[1].equals("-o")) {
            dst = args[2];
        }

        OutputStreamWriter out = null;
        try {
            ConfigurationConverter conv = new ConfigurationConverter(src);
            Instance i = conv.getInstance();
            if (dst != null) {
                i.getConstraints().addAll(conv.getNextStates(dst));
            }

            InstanceConverter iConv = new InstanceConverter();
            JSONObject o = iConv.toJSON(i);

            if (output.endsWith(".gz")) {
                out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(output)));
            } else {
                out = new FileWriter(output);
            }
            o.writeJSONString(out);
            out.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
        System.out.println("Usage: converter src [dst] -o output");
        System.out.println("\tsrc: the configuration in protobuf format to convert");
        System.out.println("\tdst: an optional configuration that will be used to get the VMs and nodes state change");
        System.out.println("\toutput: the output JSON file. Ends with '.gz' for an automatic compression");
        System.exit(code);
    }
}
