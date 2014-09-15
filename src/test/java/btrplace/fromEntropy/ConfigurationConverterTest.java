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

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Overbook;
import btrplace.model.constraint.Preserve;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ShareableResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

/**
 * Unit Tests for {@link ConfigurationConverter}.
 * TODO: Howto test VM placement.
 *
 * @author Fabien Hermenier
 */
public class ConfigurationConverterTest {

    @Test
    public void test() throws IOException {

        ConfigurationConverter conv = new ConfigurationConverter("src/test/resources/configTest.pbd");

        Model mo = conv.getModel();
        Collection<SatConstraint> cstrs = conv.getConstraint();

        Mapping map = mo.getMapping();
        Assert.assertEquals(map.getOnlineNodes().size(), 7);
        Assert.assertEquals(map.getOfflineNodes().size(), 3);

        ShareableResource rcMem = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + ConfigurationConverter.MEMORY_USAGE);
        ShareableResource rcUcpu = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + ConfigurationConverter.UCPU_USAGE);
        ShareableResource rcNbCpus = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + ConfigurationConverter.NB_CPUS);

        /*
            The initial configuration
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 10; i++) {
            Node n = new SimpleNode("N" + i, i, i + 1, i + 2);
            if (i % 3 == 0) {
                cfg.addOffline(n);
            } else {
                cfg.addOnline(n);
            }
        }
        Random rnd = new Random();
        for (int i = 1; i <= 20; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, i, i + 1, i + 2);
            vm.setCPUDemand(i + 3);
            vm.setCPUMax(i + 3);
        }

        The mapping:
        N1: VM2 VM4 VM8 VM14 VM16
        N2: VM1 VM7 VM11 VM13 VM17 VM19
        (N3): -
        N4: -
        N5: (VM6) (VM9) (VM18)
        (N6): -
        N7: (VM3) (VM12)
        N8: -
        (N9): -
        N10: (VM15)
        FARM VM5 VM10 VM20
        */

        //Check the nodes resources
        for (Node n : map.getAllNodes()) {
            int nb = rcNbCpus.getCapacity(n);
            int mem = rcMem.getCapacity(n);
            int ucpu = rcUcpu.getCapacity(n);
            Assert.assertEquals(ucpu, nb + 1);
            Assert.assertEquals(mem, nb + 2);
        }

        //Check the VMs initial resource usage
        for (VM vm : map.getAllVMs()) {
            int nb = rcNbCpus.getConsumption(vm);
            int mem = rcMem.getConsumption(vm);
            int ucpu = rcUcpu.getConsumption(vm);
            Assert.assertEquals(ucpu, nb + 1);
            Assert.assertEquals(mem, nb + 2);
        }

        //Check the VMs next resource demand
        for (SatConstraint cstr : cstrs) {
            if (cstr instanceof Preserve) {
                Preserve p = (Preserve) cstr;
                VM vm = p.getInvolvedVMs().iterator().next();
                Assert.assertEquals(p.getAmount(), rcNbCpus.getConsumption(vm) + 3);
                Assert.assertEquals(mo.getAttributes().getInteger(vm, ConfigurationConverter.UCPU_MAX).intValue(), rcNbCpus.getConsumption(vm) + 3);
            } else if (cstr instanceof Overbook) {
                Overbook o = (Overbook) cstr;
                //Assert.assertEquals(o.getInvolvedNodes(), map.getAllNodes());
                Assert.assertEquals(o.getRatio(), 1.0);
                Assert.assertTrue(o.getResource().equals(ConfigurationConverter.MEMORY_USAGE)
                        || o.getResource().equals(ConfigurationConverter.UCPU_USAGE));
            }
        }

        //Check the VMs state
        Assert.assertEquals(map.getSleepingVMs().size(), 6);
        Assert.assertEquals(map.getReadyVMs().size(), 3);
        Assert.assertEquals(map.getRunningVMs().size(), 11);

        //Check the VM placement
        for (String id : new String[]{"VM2", "VM4", "VM8", "VM14", "VM16"}) {
            VM vm = conv.getRegistryVMs().resolve(id);
            Assert.assertTrue(map.getRunningVMs().contains(vm));
            Assert.assertEquals(map.getVMLocation(vm), conv.getRegistryNodes().resolve("N1"), id + " should be on N1. Instead:" + conv.getRegistryNodes().resolve(map.getVMLocation(vm)));
        }

        for (String id : new String[]{"VM1", "VM7", "VM11", "VM13", "VM17", "VM19"}) {
            VM vm = conv.getRegistryVMs().resolve(id);
            Assert.assertTrue(map.getRunningVMs().contains(vm));
            Assert.assertEquals(map.getVMLocation(vm), conv.getRegistryNodes().resolve("N2"), id + " should be on N2. Instead:" + conv.getRegistryNodes().resolve(map.getVMLocation(vm)));
        }

        for (String id : new String[]{"VM6", "VM9", "VM18"}) {
            VM vm = conv.getRegistryVMs().resolve(id);
            Assert.assertTrue(map.getSleepingVMs().contains(vm));
            Assert.assertEquals(map.getVMLocation(vm), conv.getRegistryNodes().resolve("N5"), id + " should be on N5. Instead:" + conv.getRegistryNodes().resolve(map.getVMLocation(vm)));
        }

        for (String id : new String[]{"VM3", "VM12"}) {
            VM vm = conv.getRegistryVMs().resolve(id);
            Assert.assertTrue(map.getSleepingVMs().contains(vm));
            Assert.assertEquals(map.getVMLocation(vm), conv.getRegistryNodes().resolve("N7"), id + " should be on N7. Instead:" + conv.getRegistryNodes().resolve(map.getVMLocation(vm)));
        }

        VM vm = conv.getRegistryVMs().resolve("VM15");
        Assert.assertTrue(map.getSleepingVMs().contains(vm));
        Assert.assertEquals(map.getVMLocation(vm), conv.getRegistryNodes().resolve("N10"), "VM15 should be on N10. Instead:" + conv.getRegistryNodes().resolve(map.getVMLocation(vm)));

        //Check the entropy attribute
        for (VM v : map.getAllVMs()) {
            String id = mo.getAttributes().getString(v, ConfigurationConverter.ENTROPY_ID);
            Assert.assertEquals(conv.getRegistryVMs().resolve(id), v);
            Assert.assertEquals(conv.getRegistryVMs().resolve(v), id);
        }

        for (Node n : map.getAllNodes()) {
            String id = mo.getAttributes().getString(n, ConfigurationConverter.ENTROPY_ID);
            Assert.assertEquals(conv.getRegistryNodes().resolve(id), n);
            Assert.assertEquals(conv.getRegistryNodes().resolve(n), id);
        }

    }
}
