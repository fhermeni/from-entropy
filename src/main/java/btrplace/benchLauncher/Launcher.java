package btrplace.benchLauncher;

import btrplace.json.JSONConverterException;
import btrplace.json.model.InstanceConverter;
import btrplace.model.Instance;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by vkherbac on 16/09/14.
 */
public class Launcher {

    public static void main(String[] args) {

        String src, dst = null;
        if (args.length < 3 || args.length > 3 || !args[args.length-2].equals("-o")) {
            usage(1);
        }
        src = args[0];
        dst = args[1];

        // Read the input JSON instance
        JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
        Object obj = null;
        try {
            obj = parser.parse(new FileReader(src));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JSONObject o =  (JSONObject) obj;

        // Convert the json object to an instance
        InstanceConverter conv = new InstanceConverter();
        Instance i = null;
        try {
            i = conv.fromJSON(o);
        } catch (JSONConverterException e) {
            e.printStackTrace();
        }

        /***** Set custom attributes (static) *******
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
            // Migration duration: Memory/10 => BUG (npe)
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
        **********************************************/

        // Create and customize the reconfiguration algorithm
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.setTimeLimit(300);
        cra.doRepair(true);
        cra.setVerbosity(2);

        /* Try to solve
        ReconfigurationPlan plan = null;
        try {
            plan = cra.solve(i.getModel(), i.getSatConstraints());
        } catch (SolverException e) {
            e.printStackTrace();
        }

        System.out.println(plan);
        */
    }

    public static void usage(int code) {
        System.out.println("Usage: converter src -o output");
        System.out.println("\tsrc: the json instance to read");
        System.out.println("\toutput: the output JSON reconfiguration plan.");
        System.exit(code);
    }
}
