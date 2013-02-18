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

import btrplace.model.Model;
import btrplace.model.SatConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class Instance {

    private Model model;

    private List<SatConstraint> cstrs;

    public Instance(Model mo, List<SatConstraint> constraints) {
        model = mo;
        this.cstrs = new ArrayList<SatConstraint>(constraints);

    }

    public Instance(Model mo) {
        this(mo, Collections.<SatConstraint>emptyList());
    }

    public Model getModel() {
        return model;
    }

    public List<SatConstraint> getConstraints() {
        return cstrs;
    }
}
