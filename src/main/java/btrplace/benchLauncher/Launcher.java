package btrplace.benchLauncher;

import btrplace.fromEntropy.ConfigurationConverter;
import btrplace.json.JSONConverterException;
import btrplace.json.model.InstanceConverter;
import btrplace.model.Attributes;
import btrplace.model.Instance;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.*;
import btrplace.model.view.ModelView;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.MigrateVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import btrplace.solver.choco.duration.DurationEvaluators;
import btrplace.solver.choco.duration.LinearToAResourceActionDuration;
import btrplace.solver.choco.runner.SolvingStatistics;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * Created by vkherbac on 16/09/14.
 */
public class Launcher {

    public static void main(String[] args) {

        String src = null, dst = null;
        if (args.length < 3 || args.length > 4 || !args[args.length - 2].equals("-o")) {
            usage(1);
        }

        // Create and customize a reconfiguration algorithm
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        DurationEvaluators dev = cra.getDurationEvaluators();
        // TODO: manage with cmdline options
        cra.setTimeLimit(300);
        cra.setVerbosity(0);

        if (args[0].equals("--repair")) {
            cra.doRepair(true);
            src = args[1];
        } else {
            cra.doRepair(false);
            src = args[0];
        }
        dst = args[args.length - 1];

        // Read the input JSON instance
        JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
        Object obj = null;
        try {
            // Check for gzip extension
            if (src.endsWith(".gz")) {
                obj = parser.parse(new InputStreamReader(new GZIPInputStream(new FileInputStream(src))));
            } else {
                obj = parser.parse(new FileReader(src));
            }

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject o = (JSONObject) obj;

        // Convert the json object to an instance
        InstanceConverter conv = new InstanceConverter();
        Instance i = null;
        try {
            i = conv.fromJSON(o);
        } catch (JSONConverterException e) {
            e.printStackTrace();
        }

        //Set custom actions durations
        setAttributes(i, dev);

        // Try to solve
        ReconfigurationPlan plan = null;

        /************** PATCH **************/
        ModelView v = i.getModel().getView(ShareableResource.VIEW_ID_BASE + ConfigurationConverter.NB_CPUS);
        i.getModel().detach(v);
        //State constraints;
        for (Node n : i.getModel().getMapping().getOnlineNodes()) {
            i.getSatConstraints().add(new Online(n));
        }
        for (Node n : i.getModel().getMapping().getOfflineNodes()) {
            i.getSatConstraints().add(new Offline(n));
        }
        for (VM vm : i.getModel().getMapping().getAllVMs()) {
            i.getSatConstraints().add(new NoDelay(vm));
        }
        /************************************/

        // PATCH: Remove preserve constraints
        for (Iterator<SatConstraint> ite = i.getSatConstraints().iterator(); ite.hasNext(); ) {
            SatConstraint s = ite.next();
            if (s instanceof Preserve && src.contains("nr")) {
                ite.remove();
            }
        }

        try {
            cra.setVerbosity(0);
            cra.doRepair(true);
            cra.doOptimize(false);
            plan = cra.solve(i.getModel(), i.getSatConstraints());
            if (plan == null) {
                System.err.println("No solution !");
                throw new RuntimeException();
            }
        } catch (SolverException e) {
            e.printStackTrace();
        } finally {
            System.out.println(cra.getStatistics());
        }

        // Save stats to a CSV file
        try {
            createCSV(dst, plan, cra);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (plan != null) {

            //Save the plan
            try {
                savePlan(stripExtension(dst) + ".plan", plan);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setAttributes(Instance i, DurationEvaluators dev) {

        Attributes attrs = i.getModel().getAttributes();
        for (VM vm : i.getModel().getMapping().getAllVMs()) {
            // Hypervisor
            //attrs.put(vm, "template", "kvm");

            // Can be re-instantiated
            attrs.put(vm, "clone", true);

            // Actions
            attrs.put(vm, "forge", 3);
            attrs.put(vm, "kill", 2);
            // Migration duration: Memory/100
            dev.register(MigrateVM.class, new LinearToAResourceActionDuration<VM>("memory", 0.01));
            //attrs.put(vm, "boot", 5);
            //attrs.put(vm, "shutdown", 2);
            //attrs.put(vm, "suspend", 4);
            //attrs.put(vm, "resume", 5);
            //attrs.put(vm, "allocate", 5);
        }
        /*for (Node n : i.getModel().getMapping().getAllNodes()) {
            attrs.put(n, "boot", 6);
            attrs.put(n, "shutdown", 6);
        }*/
    }

    public static void savePlan(String fileName, ReconfigurationPlan plan) throws IOException {
        // Write the plan in a specific file
        FileWriter writerPlan = new FileWriter(fileName);
        writerPlan.append(plan.toString());
        writerPlan.flush();
        writerPlan.close();
    }

    public static void createCSV(String fileName, ReconfigurationPlan plan, ChocoReconfigurationAlgorithm cra) throws IOException {

        FileWriter writer = new FileWriter(fileName);
        SolvingStatistics stats = cra.getStatistics();

        // Set header
        if (plan != null) {
            writer.append("planDuration;planSize;planActionsSize;");
        }
        if (stats != null) {
            writer.append("craStart;craNbSolutions;");
            if (stats.getSolutions().size() > 0) {
                writer.append("craSolutionTime;");
            }
            writer.append("craCoreRPBuildDuration;" +
                    "craSpeRPDuration;" +
                    "craSolvingDuration;" +
                    "craNbBacktracks;" +
                    "craNbConstraints;" +
                    "craNbManagedVMs;" +
                    "craNbNodes;" +
                    "craNbSearchNodes;" +
                    "craNbVMs" + '\n'
            );
        }

        // Store values
        if (plan != null) {
            writer.append(String.valueOf(plan.getDuration()) + ';' +
                    String.valueOf(plan.getSize()) + ';' +
                    String.valueOf(plan.getActions().size()) + ';'
            );
        }
        if (stats != null) {
            writer.append(String.valueOf(stats.getStart()) + ';' +
                    String.valueOf(stats.getSolutions().size()) + ';'
            );
            if (stats.getSolutions().size() > 0) {
                writer.append(String.valueOf(stats.getSolutions().get(0).getTime()) + ';');
            }
            writer.append(String.valueOf(stats.getCoreRPBuildDuration()) + ';' +
                    String.valueOf(stats.getSpeRPDuration()) + ';' +
                    String.valueOf(stats.getSolvingDuration()) + ';' +
                    String.valueOf(stats.getNbBacktracks()) + ';' +
                    String.valueOf(stats.getNbConstraints()) + ';' +
                    String.valueOf(stats.getNbManagedVMs()) + ';' +
                    String.valueOf(stats.getNbNodes()) + ';' +
                    String.valueOf(stats.getNbSearchNodes()) + ';' +
                    String.valueOf(stats.getNbVMs()) + '\n'
            );
        }

        // Close the file
        writer.flush();
        writer.close();
    }

    public static String stripExtension(final String s)
    {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }

    public static void usage(int code) {
        System.out.println("Usage: converter [--repair] src -o output");
        System.out.println("\t--repair: option to enable the 'repair' feature");
        System.out.println("\tsrc: the json instance to read");
        System.out.println("\toutput: the output statistics file.");
        System.exit(code);
    }
}
