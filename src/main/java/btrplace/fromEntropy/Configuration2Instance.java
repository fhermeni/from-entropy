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
import btrplace.model.constraint.Preserve;
import btrplace.model.view.ShareableResource;
import entropy.configuration.parser.PBConfiguration;
import entropy.configuration.parser.PBNode;
import entropy.configuration.parser.PBVirtualMachine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Convert an entropy configuration to a Btrplace model.
 * <p/>
 * As in entropy, resource demand was a part of the configuration, the demands are converted
 * to {@link Preserve} constraints.
 *
 * @author Fabien Hermenier
 */
public class Configuration2Instance {

    private Mapping map;

    private Model model;

    private ShareableResource rcMem, rcCpu, rcNbCPUs;

    private Map<String, UUID> registry;

    private Set<SatConstraint> cstrs;

    public Configuration2Instance(String cfg) throws IOException {
        map = new DefaultMapping();
        model = new DefaultModel(map);

        rcMem = new ShareableResource("mem");
        rcCpu = new ShareableResource("uCpu");
        rcNbCPUs = new ShareableResource("nbCpus");
        model.attach(rcCpu);
        model.attach(rcMem);
        model.attach(rcNbCPUs);

        registry = new HashMap<String, UUID>();

        cstrs = new HashSet<SatConstraint>();

        makeMapping(PBConfiguration.Configuration.parseFrom(new FileInputStream(cfg)));
    }

    public Collection<SatConstraint> getConstraint() {
        return cstrs;
    }

    public Map<String, UUID> getRegistry() {
        return registry;
    }

    public Model getModel() {
        return model;
    }

    /**
     * Un-serialize the protobuf version of a virtual machine.
     *
     * @param pbVM the virtual machine to convert.
     * @return the entropy VirtualMachine.
     */
    private UUID parse(PBVirtualMachine.VirtualMachine pbVM) {

        String name = pbVM.getName();
        if (!registry.containsKey(name)) {
            registry.put(name, UUID.randomUUID());
        }
        UUID u = registry.get(name);

        if (pbVM.hasCpuConsumption()) {
            rcCpu.set(u, pbVM.getCpuConsumption());
        }

        if (pbVM.hasCpuDemand()) {
            cstrs.add(new Preserve(Collections.singleton(u), "ucpu", pbVM.getCpuDemand()));
        }

        if (pbVM.hasCpuMax()) {
            model.getAttributes().put(u, "uCpuMax", pbVM.getCpuMax());
        }

        if (pbVM.hasMemoryConsumption()) {
            rcMem.set(u, pbVM.getMemoryConsumption());
        }

        if (pbVM.hasMemoryDemand()) {
            cstrs.add(new Preserve(Collections.singleton(u), "memory", pbVM.getCpuDemand()));
        }

        if (pbVM.hasTemplate()) {
            model.getAttributes().put(u, "template", pbVM.getTemplate());
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

    private UUID parse(PBNode.Node pbNode) {

        String name = pbNode.getName();
        if (!registry.containsKey(name)) {
            registry.put(name, UUID.randomUUID());
        }
        UUID u = registry.get(name);


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
            model.getAttributes().put(u, "ip", pbNode.getIp());
        }

        if (pbNode.hasMac()) {
            model.getAttributes().put(u, "mac", pbNode.getMac());
        }

        for (PBNode.Node.Platform p : pbNode.getPlatformsList()) {
            String id = p.getName();
            Map<String, String> opts = new HashMap<String, String>();
            for (PBNode.Node.Platform.Option o : p.getOptionsList()) {
                String k = o.getKey();
                if (o.hasValue()) {
                    model.getAttributes().put(u, id, o.getValue());
                    opts.put(k, o.getValue());
                } else {
                    model.getAttributes().put(u, id, true);
                }
            }
        }

        if (pbNode.hasCurrentPlatform()) {
            model.getAttributes().put(u, "template", pbNode.getCurrentPlatform());
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
