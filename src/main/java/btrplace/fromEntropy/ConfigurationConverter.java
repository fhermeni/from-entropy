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

import btrplace.model.*;
import btrplace.model.constraint.*;
import btrplace.model.view.NamingService;
import btrplace.model.view.ShareableResource;
import entropy.configuration.parser.PBConfiguration;
import entropy.configuration.parser.PBNode;
import entropy.configuration.parser.PBVirtualMachine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Convert an entropy configuration to a Btrplace model and a set of constraints.
 * In practice the following conversion are performed:
 * <ul>
 * <li>The memory usage (consumption) of the nodes (VMs) is converted to a {@link ShareableResource} view with
 * a resource identifier equals to {@link #MEMORY_USAGE}. The mapping between the VM consumption and the node
 * capacity is performed with an {@link Overbook} constraint having an overloading factor of 1.
 * </li>
 * <li>The uCPU usage (consumption) of the nodes (VMs) is converted to a {@link ShareableResource} view with
 * a resource identifier equals to {@link #UCPU_USAGE}. The mapping between the VM consumption and the node
 * capacity is performed with an {@link Overbook} constraint having an overloading factor of 1.</li>
 * <li>The number of CPUs for the nodes and the VMs is converted to a {@link ShareableResource} view with
 * a resource identifier equals to {@link #NB_CPUS}. No mapping is performed by default as Entropy ignore this resource.</li>
 * <li>VM and node templates are converted into an attribute {@link #TEMPLATE}.</li>
 * <li>Node IP and Max are converted into attributes {@link #IP} and {@link #MAC} respectively.</li>
 * <li>For each VM and node, the attribute {@link #ENTROPY_ID} indicates the element name</li>
 * </ul>
 * <p/>
 * In addition, when the next state for the elements is provided through another configuration, {@link Running},
 * {@link Sleeping}, {@link Ready}, {@link Killed}, {@link Online}, {@link Offline} constraints are inserted to
 * indicate the state changes.
 *
 * @author Fabien Hermenier
 */
public class ConfigurationConverter {

    private Mapping map;

    private Model model;

    private ShareableResource rcMem, rcCpu, rcNbCPUs;

    private NamingService<Node> registryNodes;
    private int nodeId = 0;
    private NamingService<VM> registryVMs;
    private int vmId = 0;


    private List<SatConstraint> cstrs;

    /**
     * The amount of memory available on a node, or the memory consumption of a VM.
     */
    public static final String MEMORY_USAGE = "memory";

    /**
     * The number of CPUs on a node or a VM.
     */
    public static final String NB_CPUS = "nbCpus";

    /**
     * The global uCPU capacity of a node or the global uCPU consumption of a VM.
     */
    public static final String UCPU_USAGE = "uCpu";

    /**
     * The attribute identifier to denotes a VM maximum uCPU usage.
     */
    public static final String UCPU_MAX = "uCpu";

    /**
     * The VMs and the nodes attribute to indicate their template.
     */
    public static final String TEMPLATE = "template";

    /**
     * Attribute identifier to get an node IP.
     */
    public static final String IP = "ip";

    /**
     * Attribute identifier to get an node MAC.
     */
    public static final String MAC = "mac";

    /**
     * Attribute identifier to get the entropy identifier.
     */
    public static final String ENTROPY_ID = "entropy_id";

    /**
     * Convert an Entropy Configuration.
     *
     * @param src the configuration to convert
     * @throws IOException if an error occurred while parsing the configuration
     */
    public ConfigurationConverter(String src) throws IOException {

        model = new DefaultModel();
        map = model.getMapping();

        rcMem = new ShareableResource(MEMORY_USAGE);
        rcCpu = new ShareableResource(UCPU_USAGE);
        rcNbCPUs = new ShareableResource(NB_CPUS);

        model.attach(rcCpu);
        model.attach(rcMem);
        model.attach(rcNbCPUs);

        registryNodes = NamingService.newNodeNS();
        registryVMs = NamingService.newVMNS();

        model.attach(registryNodes);
        model.attach(registryVMs);

        cstrs = new ArrayList<SatConstraint>();

        makeMapping(PBConfiguration.Configuration.parseFrom(new FileInputStream(src)));
    }

    /**
     * Convert an Entropy Configuration and the element next states.
     *
     * @param src the configuration to convert
     * @param dst the next state for the elements. Every other parameters are ignored
     * @throws IOException if an error occurred while parsing the configuration
     */
    public ConfigurationConverter(String src, String dst) throws IOException {
        this(src);
        cstrs.addAll(getNextStates(dst));
    }

    /**
     * Convert the elements state change.
     *
     * @param dst the entropy destination configuration.
     * @return the corresponding set of constraints
     * @throws IOException if an error occurred while reading the configuration
     */
    public Collection<SatConstraint> getNextStates(String dst) throws IOException {
        List<SatConstraint> states = new ArrayList<SatConstraint>();

        PBConfiguration.Configuration cfg = PBConfiguration.Configuration.parseFrom(new FileInputStream(dst));

        //Nodes states
        List<Node> on = new ArrayList<Node>();
        List<Node> off = new ArrayList<Node>();

        //VMs states
        List<VM> ready = new ArrayList<VM>();
        List<VM> running = new ArrayList<VM>();
        List<VM> sleeping = new ArrayList<VM>();
        List<VM> killed = new ArrayList<VM>();

        nextNodeStates(cfg, on, off);
        nextVMStates(cfg, ready, running, sleeping, killed);

        if (!on.isEmpty()) {
            for (SatConstraint nc : Online.newOnline(on)){
                states.add(nc);
            }
        }
        if (!off.isEmpty()) {
            for (SatConstraint nc : Offline.newOffline(off)){
                states.add(nc);
            }
        }

        if (!ready.isEmpty()) {
            for (SatConstraint r : Ready.newReady(ready)){
                states.add(r);
            }
        }
        if (!running.isEmpty()) {
            for (SatConstraint r : Running.newRunning(running)){
                states.add(r);
            }
        }
        if (!killed.isEmpty()) {
            for (SatConstraint k : Killed.newKilled(killed)){
                states.add(k);
            }
        }
        if (!sleeping.isEmpty()) {
            for (SatConstraint s : Sleeping.newSleeping(sleeping)){
                states.add(s);
            }
        }

        return states;
    }

    private void nextVMStates(PBConfiguration.Configuration cfg, List<VM> ready, List<VM> running, List<VM> sleeping, List<VM> killed) {

        /*
          (none || running) -> ready : ready()
          (ready || sleeping) -> running  : running()
          * -> none : killed()
          running -> sleeping : sleeping()
         */

        Set<VM> seen = new HashSet<VM>();
        for (PBVirtualMachine.VirtualMachine v : cfg.getWaitingsList()) {
            VM vm = registryVMs.resolve(v.getName());
            if (map.getRunningVMs().contains(vm) || !map.getAllVMs().contains(vm)) {
                ready.add(vm);
            }
            seen.add(vm);
        }

        for (PBConfiguration.Configuration.Hoster hoster : cfg.getOnlinesList()) {
            for (PBConfiguration.Configuration.Hosted hosted : hoster.getHostedList()) {
                PBConfiguration.Configuration.HostedVMState st = hosted.getState();
                String vname = hosted.getVm().getName();
                VM vm = registryVMs.resolve(vname);
                if (st == PBConfiguration.Configuration.HostedVMState.RUNNING) {
                    if (map.getReadyVMs().contains(vm) || map.getSleepingVMs().contains(vm)) {
                        running.add(vm);
                    }
                } else { //Sleeping state
                    if (map.getRunningVMs().contains(vm)) {
                        sleeping.add(vm);
                    }
                }
                seen.add(vm);
            }
        }
        //The killed VMs
        for (VM vm : map.getAllVMs()) {
            if (!seen.contains(vm)) {
                killed.add(vm);
            }
        }
    }

    private void nextNodeStates(PBConfiguration.Configuration cfg, List<Node> on, List<Node> off) {
        //Check for the online nodes that go offline
        for (PBNode.Node n : cfg.getOfflinesList()) {
            Node node = registryNodes.resolve("@"+n.getName());
            off.add(node);
        }

        //Check for the offline nodes that go online
        for (PBConfiguration.Configuration.Hoster n : cfg.getOnlinesList()) {
            Node node = registryNodes.resolve("@"+n.getNode().getName());
            on.add(node);
        }
    }

    /**
     * Get the constraint that results from the configuration conversion.
     *
     * @return a list of constraints that may be empty
     */
    public Collection<SatConstraint> getConstraint() {
        return cstrs;
    }

    /**
     * Get the registry that convert entropy to btrplace nodes identifiers.
     *
     * @return a mapping of elements
     */
    public NamingService<Node> getRegistryNodes() {
        return registryNodes;
    }

    /**
     * Get the registry that convert entropy to btrplace vms identifiers.
     *
     * @return a mapping of elements
     */
    public NamingService<VM> getRegistryVMs() {
        return registryVMs;
    }

    /**
     * Get the model that results from the configuration conversion.
     *
     * @return the resulting model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Un-serialize the protobuf version of a VM
     *
     * @param pbVM the virtual machine to convert.
     * @return the VM identifier
     */
    private VM parse(PBVirtualMachine.VirtualMachine pbVM) {

        String name = pbVM.getName();
        VM vm = registryVMs.resolve(name);
        if (vm == null) {
            vm = new VM(vmId);
            vmId++;
            registryVMs.register(vm, name);
        }
        model.getAttributes().put(vm, ENTROPY_ID, name);
        if (pbVM.hasCpuConsumption()) {
            rcCpu.setConsumption(vm, pbVM.getCpuConsumption());
        }

        if (pbVM.hasCpuDemand() && pbVM.getCpuDemand() != rcCpu.getConsumption(vm)) {
            cstrs.add(new Preserve(vm, UCPU_USAGE, pbVM.getCpuDemand()));
        }

        if (pbVM.hasCpuMax()) {
            model.getAttributes().put(vm, UCPU_MAX, pbVM.getCpuMax());
        }

        if (pbVM.hasMemoryConsumption()) {
            rcMem.setConsumption(vm, pbVM.getMemoryConsumption());
        }

        if (pbVM.hasMemoryDemand() && pbVM.getMemoryDemand() != rcMem.getConsumption(vm)) {
            cstrs.add(new Preserve(vm, MEMORY_USAGE, pbVM.getMemoryDemand()));
        }

        if (pbVM.hasTemplate()) {
            model.getAttributes().put(vm, TEMPLATE, pbVM.getTemplate());
        }

        if (pbVM.hasNbOfCPUs()) {
            rcNbCPUs.setConsumption(vm, pbVM.getNbOfCPUs());
        }

        for (PBVirtualMachine.VirtualMachine.Option opt : pbVM.getOptionsList()) {
            String k = opt.getKey();
            if (opt.hasValue()) {
                model.getAttributes().castAndPut(vm, k, opt.getValue());
            } else {
                model.getAttributes().put(vm, k, true);
            }
        }

        return vm;
    }

    /**
     * Un-serialize the protobuf version of a node
     *
     * @param pbNode the node to convert.
     * @return the Node
     */
    private Node parse(PBNode.Node pbNode) {

        String name = pbNode.getName();
        Node n = registryNodes.resolve("@"+name);
        if (n == null) {
            n = new Node(nodeId);
            nodeId++;
            registryNodes.register(n, "@"+name);
        }
        model.getAttributes().put(n, ENTROPY_ID, name);

        if (pbNode.hasNbOfCPUs()) {
            rcNbCPUs.setCapacity(n, pbNode.getNbOfCPUs());
        }

        if (pbNode.hasCpuCapacity()) {
            rcCpu.setCapacity(n, pbNode.getCpuCapacity());
        }

        if (pbNode.hasMemoryCapacity()) {
            rcMem.setCapacity(n, pbNode.getMemoryCapacity());
        }

        if (pbNode.hasIp()) {
            model.getAttributes().put(n, IP, pbNode.getIp());
        }

        if (pbNode.hasMac()) {
            model.getAttributes().put(n, MAC, pbNode.getMac());
        }

        for (PBNode.Node.Platform p : pbNode.getPlatformsList()) {
            for (PBNode.Node.Platform.Option o : p.getOptionsList()) {
                String k = o.getKey();
                if (o.hasValue()) {
                    model.getAttributes().castAndPut(n, k, o.getValue());
                } else {
                    model.getAttributes().put(n, k, true);
                }
            }
        }

        if (pbNode.hasCurrentPlatform()) {
            model.getAttributes().put(n, TEMPLATE, pbNode.getCurrentPlatform());
        }
        return n;
    }

    private void makeMapping(PBConfiguration.Configuration c) {

        for (PBNode.Node n : c.getOfflinesList()) {
            map.addOfflineNode(parse(n));
        }
        for (PBVirtualMachine.VirtualMachine vm : c.getWaitingsList()) {
            map.addReadyVM(parse(vm));
        }
        for (PBConfiguration.Configuration.Hoster h : c.getOnlinesList()) {
            Node n = parse(h.getNode());
            map.addOnlineNode(n);
            for (PBConfiguration.Configuration.Hosted hosted : h.getHostedList()) {
                PBConfiguration.Configuration.HostedVMState st = hosted.getState();
                VM vm = parse(hosted.getVm());
                switch (st) {
                    case RUNNING:
                        map.addRunningVM(vm, n);
                        break;
                    case SLEEPING:
                        map.addSleepingVM(vm, n);
                        break;
                }
            }
        }
    }

    /**
     * Get the conversion result as an instance.
     *
     * @return an instance
     */
    public Instance getInstance() {
        return new Instance(model, cstrs, new MinMTTR());
    }
}
