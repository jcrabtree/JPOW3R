/*
 * Copyright (C) 1996-2010 by Power System Engineering Research Center (PSERC)
 * Copyright (C) 2010 Richard Lincoln
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package edu.cornell.pserc.jpower.tdouble;

import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.impl.SparseRCIntMatrix2D;
import cern.jet.math.tint.IntFunctions;
import edu.cornell.pserc.jpower.tdouble.jpc.DZjp_bus;
import edu.cornell.pserc.jpower.tdouble.jpc.DZjp_gen;
import edu.cornell.pserc.jpower.tdouble.jpc.DZjp_jpc;
import edu.cornell.pserc.jpower.tdouble.util.DZjp_util;

/**
 * Builds index lists for each type of bus (REF, PV, PQ).
 *
 * @author Ray Zimmerman (rz10@cornell.edu)
 * @author Richard Lincoln (r.w.lincoln@gmail.com)
 *
 */
public class DZjp_bustypes {

	private static final DZjp_util util = new DZjp_util();
	private static final IntFunctions ifunc = IntFunctions.intFunctions;

	private static final int REF = DZjp_jpc.REF;
	private static final int PV = DZjp_jpc.PV;
	private static final int PQ = DZjp_jpc.PQ;

	/**
	 * Generators with "out-of-service" status are treated as PQ buses with
	 * zero generation (regardless of Pg/Qg values in gen). Expects BUS and
	 * GEN have been converted to use internal consecutive bus numbering.
	 *
	 * @param bus
	 * @param gen
	 * @return
	 */
	@SuppressWarnings("static-access")
	public static IntMatrix1D[] jp_bustypes(DZjp_bus bus, DZjp_gen gen) {

		/* get generator status */
		int nb = bus.size();
		int ng = gen.size();

		/* gen connection matrix, element i, j is 1 if, generator j at bus i is ON */
		SparseRCIntMatrix2D Cg = new SparseRCIntMatrix2D(nb, ng,
				gen.gen_bus.toArray(), util.irange(ng),
				gen.gen_status.assign(ifunc.equals(1)).toArray(), false, false, false);

		/* number of generators at each bus that are ON */
		IntMatrix1D bus_gen_status = Cg.zMult(IntFactory1D.dense.make(ng, 1), null);

		/* form index lists for slack, PV, and PQ buses */
		IntMatrix1D ref = bus.bus_type.assign(ifunc.equals(REF));
		ref.assign(bus_gen_status, ifunc.and);		// reference bus index
		IntMatrix1D pv = bus.bus_type.assign(ifunc.equals(PV));
		pv.assign(bus_gen_status, ifunc.and);		// PV bus indices
		IntMatrix1D pq = bus.bus_type.assign(ifunc.equals(PQ));
		pv.assign(bus_gen_status.assign(ifunc.not), ifunc.or);

		/* pick a new reference bus if for some reason there is none (may have been shut down) */
		if (ref.size() == 0) {
			ref = IntFactory1D.dense.make(1, pv.get(0));		// use the first PV bus
			pv = pv.viewPart(1, (int) (pv.size() - 1)).copy();	// take it off PV list
		}

		return new IntMatrix1D[] {ref, pv, pq};
	}

}