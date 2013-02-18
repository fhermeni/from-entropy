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
import btrplace.model.SatConstraint;
import btrplace.model.constraint.Overbook;
import btrplace.model.constraint.Preserve;
import btrplace.model.view.ShareableResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

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
            Node n = cfg.getOnlines().get(rnd.nextInt(cfg.getOnlines().size()));
            if (i % 3 == 0) {
                cfg.setSleepOn(vm, n);
            } else if (i % 5 == 0) {
                cfg.addWaiting(vm);
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N" + (i%2 + 1)));
            }
        }
         */

        //Check the nodes resources
        for (UUID n : map.getAllNodes()) {
            int nb = rcNbCpus.get(n);
            int mem = rcMem.get(n);
            int ucpu = rcUcpu.get(n);
            Assert.assertEquals(ucpu, nb + 1);
            Assert.assertEquals(mem, nb + 2);
        }

        //Check the VMs initial resource usage
        for (UUID vm : map.getAllVMs()) {
            int nb = rcNbCpus.get(vm);
            int mem = rcMem.get(vm);
            int ucpu = rcUcpu.get(vm);
            Assert.assertEquals(ucpu, nb + 1);
            Assert.assertEquals(mem, nb + 2);
        }

        //Check the VMs next resource demand
        for (SatConstraint cstr : cstrs) {
            if (cstr instanceof Preserve) {
                Preserve p = (Preserve) cstr;
                UUID vm = p.getInvolvedVMs().iterator().next();
                Assert.assertEquals(p.getAmount(), rcNbCpus.get(vm) + 3);
                Assert.assertEquals(mo.getAttributes().getLong(vm, ConfigurationConverter.UCPU_MAX).intValue(), rcNbCpus.get(vm) + 3);
            } else if (cstr instanceof Overbook) {
                Overbook o = (Overbook) cstr;
                Assert.assertEquals(o.getInvolvedNodes(), map.getAllNodes());
                Assert.assertEquals(o.getRatio(), 1.0);
                Assert.assertTrue(o.getResource().equals(ConfigurationConverter.MEMORY_USAGE)
                        || o.getResource().equals(ConfigurationConverter.UCPU_USAGE));
            }
        }

        //Check the VMs state
        Assert.assertEquals(map.getSleepingVMs().size(), 6);
        Assert.assertEquals(map.getReadyVMs().size(), 3);
        Assert.assertEquals(map.getRunningVMs().size(), 11);
    }
}
