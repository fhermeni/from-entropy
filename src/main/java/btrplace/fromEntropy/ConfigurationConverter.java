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

    private Map<String, UUID> registry;

    private Map<UUID, String> revRegistry;

    private Set<SatConstraint> cstrs;

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
        map = new DefaultMapping();
        model = new DefaultModel(map);

        rcMem = new ShareableResource(MEMORY_USAGE);
        rcCpu = new ShareableResource(UCPU_USAGE);
        rcNbCPUs = new ShareableResource(NB_CPUS);
        model.attach(rcCpu);
        model.attach(rcMem);
        model.attach(rcNbCPUs);

        registry = new HashMap<String, UUID>();
        revRegistry = new HashMap<UUID, String>();
        cstrs = new HashSet<SatConstraint>();

        makeMapping(PBConfiguration.Configuration.parseFrom(new FileInputStream(src)));

        cstrs.add(new Overbook(map.getAllNodes(), MEMORY_USAGE, 1));
        cstrs.add(new Overbook(map.getAllNodes(), UCPU_USAGE, 1));
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
        Online on = new Online(new HashSet<UUID>());
        Offline off = new Offline(new HashSet<UUID>());
        //VMs states
        Ready ready = new Ready(new HashSet<UUID>());
        Running running = new Running(new HashSet<UUID>());
        Sleeping sleeping = new Sleeping(new HashSet<UUID>());
        Killed killed = new Killed(new HashSet<UUID>());

        nextNodeStates(cfg, on, off);
        nextVMStates(cfg, ready, running, sleeping, killed);

        if (!on.getInvolvedNodes().isEmpty()) {
            states.add(on);
        }
        if (!off.getInvolvedNodes().isEmpty()) {
            states.add(off);
        }
        if (!ready.getInvolvedVMs().isEmpty()) {
            states.add(ready);
        }
        if (!running.getInvolvedVMs().isEmpty()) {
            states.add(running);
        }
        if (!killed.getInvolvedVMs().isEmpty()) {
            states.add(killed);
        }
        if (!sleeping.getInvolvedVMs().isEmpty()) {
            states.add(ready);
        }

        return states;
    }

    private void nextVMStates(PBConfiguration.Configuration cfg, Ready ready, Running running, Sleeping sleeping, Killed killed) {

        /*
          (none || running) -> ready : ready()
          (ready || sleeping) -> running  : running()
          * -> none : killed()
          running -> sleeping : sleeping()
         */

        Set<UUID> seen = new HashSet<UUID>();
        for (PBVirtualMachine.VirtualMachine vm : cfg.getWaitingsList()) {
            UUID u = registry.get(vm.getName());
            if (map.getRunningVMs().contains(u) || !map.getAllVMs().contains(u)) {
                ready.getInvolvedVMs().add(u);
                seen.add(u);
            }
        }

        for (PBConfiguration.Configuration.Hoster hoster : cfg.getOnlinesList()) {
            for (PBConfiguration.Configuration.Hosted hosted : hoster.getHostedList()) {
                PBConfiguration.Configuration.HostedVMState st = hosted.getState();
                String vm = hosted.getVm().getName();
                UUID u = registry.get(vm);
                if (st == PBConfiguration.Configuration.HostedVMState.RUNNING) {
                    if (map.getReadyVMs().contains(u) || map.getSleepingVMs().contains(u)) {
                        running.getInvolvedVMs().add(u);
                        seen.add(u);
                    }
                } else { //Sleeping state
                    if (map.getRunningVMs().contains(u)) {
                        sleeping.getInvolvedVMs().add(u);
                        seen.add(u);
                    }
                }
            }
        }
        //The killed VMs
        for (UUID vm : map.getAllVMs()) {
            if (!seen.contains(vm)) {
                killed.getInvolvedVMs().add(vm);
            }
        }

    }

    private void nextNodeStates(PBConfiguration.Configuration cfg, Online on, Offline off) {
        //Check for the online nodes that go offline
        for (PBNode.Node n : cfg.getOfflinesList()) {
            UUID u = registry.get(n.getName());
            if (map.getOnlineNodes().contains(u)) {
                off.getInvolvedNodes().add(u);
            }
        }

        //Check for the offline nodes that go online
        for (PBConfiguration.Configuration.Hoster n : cfg.getOnlinesList()) {
            UUID u = registry.get(n.getNode().getName());
            if (map.getOfflineNodes().contains(u)) {
                on.getInvolvedNodes().add(u);
            }
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
     * Get the registry that convert entropy to btrplace identifiers.
     *
     * @return a mapping of elements
     */
    public Map<String, UUID> getRegistry() {
        return registry;
    }

    /**
     * Get the registry that convert btrplace identifiers to entropy ones.
     *
     * @return a mapping of elements
     */
    public Map<UUID, String> getReverseRegistry() {
        return revRegistry;
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
    private UUID parse(PBVirtualMachine.VirtualMachine pbVM) {

        String name = pbVM.getName();
        UUID u = registry.get(name);
        model.getAttributes().put(u, ENTROPY_ID, name);
        if (u == null) {
            u = UUID.randomUUID();
            registry.put(name, u);
            revRegistry.put(u, name);
        }

        if (pbVM.hasCpuConsumption()) {
            rcCpu.set(u, pbVM.getCpuConsumption());
        }

        if (pbVM.hasCpuDemand()) {
            cstrs.add(new Preserve(Collections.singleton(u), UCPU_USAGE, pbVM.getCpuDemand()));
        }

        if (pbVM.hasCpuMax()) {
            model.getAttributes().put(u, UCPU_MAX, pbVM.getCpuMax());
        }

        if (pbVM.hasMemoryConsumption()) {
            rcMem.set(u, pbVM.getMemoryConsumption());
        }

        if (pbVM.hasMemoryDemand()) {
            cstrs.add(new Preserve(Collections.singleton(u), MEMORY_USAGE, pbVM.getCpuDemand()));
        }

        if (pbVM.hasTemplate()) {
            model.getAttributes().put(u, TEMPLATE, pbVM.getTemplate());
        }

        if (pbVM.hasNbOfCPUs()) {
            rcNbCPUs.set(u, pbVM.getNbOfCPUs());
        }

        for (PBVirtualMachine.VirtualMachine.Option opt : pbVM.getOptionsList()) {
            String k = opt.getKey();
            if (opt.hasValue()) {
                model.getAttributes().put(u, k, opt.getValue());
            } else {
                model.getAttributes().put(u, k, true);
            }
        }

        return u;
    }

    /**
     * Un-serialize the protobuf version of a node
     *
     * @param pbNode the node to convert.
     * @return the VM identifier
     */
    private UUID parse(PBNode.Node pbNode) {

        String name = pbNode.getName();
        UUID u = registry.get(name);
        model.getAttributes().put(u, ENTROPY_ID, name);
        if (u == null) {
            u = UUID.randomUUID();
            registry.put(name, u);
            revRegistry.put(u, name);
        }

        if (pbNode.hasNbOfCPUs()) {
            rcNbCPUs.set(u, pbNode.getNbOfCPUs());
        }

        if (pbNode.hasCpuCapacity()) {
            rcCpu.set(u, pbNode.getCpuCapacity());
        }

        if (pbNode.hasMemoryCapacity()) {
            rcMem.set(u, pbNode.getMemoryCapacity());
        }

        if (pbNode.hasIp()) {
            model.getAttributes().put(u, IP, pbNode.getIp());
        }

        if (pbNode.hasMac()) {
            model.getAttributes().put(u, MAC, pbNode.getMac());
        }

        for (PBNode.Node.Platform p : pbNode.getPlatformsList()) {
            for (PBNode.Node.Platform.Option o : p.getOptionsList()) {
                String k = o.getKey();
                if (o.hasValue()) {
                    model.getAttributes().put(u, k, o.getValue());
                } else {
                    model.getAttributes().put(u, k, true);
                }
            }
        }

        if (pbNode.hasCurrentPlatform()) {
            model.getAttributes().put(u, TEMPLATE, pbNode.getCurrentPlatform());
        }
        return u;
    }

    private void makeMapping(PBConfiguration.Configuration c) {

        for (PBNode.Node n : c.getOfflinesList()) {
            map.addOfflineNode(parse(n));
        }
        for (PBVirtualMachine.VirtualMachine vm : c.getWaitingsList()) {
            map.addReadyVM(parse(vm));
        }
        for (PBConfiguration.Configuration.Hoster h : c.getOnlinesList()) {
            UUID n = parse(h.getNode());
            map.addOnlineNode(n);
            for (PBConfiguration.Configuration.Hosted hosted : h.getHostedList()) {
                PBConfiguration.Configuration.HostedVMState st = hosted.getState();
                UUID vm = parse(hosted.getVm());
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
}
