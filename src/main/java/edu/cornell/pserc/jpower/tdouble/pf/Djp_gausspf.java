/*
 * Copyright (C) 1996-2010 Power System Engineering Research Center
 * Copyright (C) 2010-2011 Richard Lincoln
 *
 * JPOWER is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * JPOWER is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JPOWER. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package edu.cornell.pserc.jpower.tdouble.pf;

import java.util.Map;

import cern.colt.matrix.Norm;
import cern.colt.matrix.tdcomplex.DComplexMatrix1D;
import cern.colt.matrix.tdcomplex.DComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.util.tdouble.Djp_util;
import cern.jet.math.tdcomplex.DComplexFunctions;
import edu.cornell.pserc.jpower.tdouble.Djp_jpoption;

/**
 * Solves the power flow using a Gauss-Seidel method.
 *
 * @author Ray Zimmerman
 * @author Alberto Borghetti, University of Bologna, Italy
 * @author Richard Lincoln
 *
 */
public class Djp_gausspf {

	private static final DComplexFunctions cfunc = DComplexFunctions.functions;

	/**
	 * Solves for bus voltages given the full system admittance matrix (for
	 * all buses), the complex bus power injection vector (for all buses),
	 * the initial vector of complex bus voltages, and column vectors with
	 * the lists of bus indices for the swing bus, PV buses, and PQ buses,
	 * respectively. The bus voltage vector contains the set point for
	 * generator (including ref bus) buses, and the reference angle of the
	 * swing bus, as well as an initial guess for remaining magnitudes and
	 * angles. JPOPT is a JPOWER options vector which can be used to
	 * set the termination tolerance, maximum number of iterations, and
	 * output options (see MPOPTION for details). Uses default options
	 * if this parameter is not given. Returns the final complex voltages,
	 * a flag which indicates whether it converged or not, and the number
	 * of iterations performed.
	 *
	 * @param Ybus
	 * @param Sbus
	 * @param V0
	 * @param ref
	 * @param pv
	 * @param pq
	 * @param jpopt
	 * @return
	 */
	@SuppressWarnings("static-access")
	public static Object[] jp_gausspf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus,
			DComplexMatrix1D V0, int ref, int[] pv, int[] pq, Map<String, Double> jpopt) {
		double tol, normF;
		int i, max_it, verbose, npv;
		int[] pvpq;
		boolean converged;
		DComplexMatrix1D V, /*Va, */Vm, mis, dVpq, dVpv, absV;
		DoubleMatrix1D F;


		/* options */
		tol	= jpopt.get("PF_TOL");
		max_it	= jpopt.get("PF_MAX_IT").intValue();
		verbose	= jpopt.get("VERBOSE").intValue();

		/* initialize */
		pvpq = Djp_util.icat(pv, pq);
		converged = false;
		i = 0;
		V = V0;
		//Va = V.copy().assign(cfunc.arg);
		Vm = V.copy().assign(cfunc.abs);

		/* set up indexing for updating V */
		npv = pv.length;

		/* evaluate F(x0) */
		mis = Ybus.zMult(V, null).assign(cfunc.conj);
		mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus);
		F = DoubleFactory1D.sparse.make(new DoubleMatrix1D[] {
				mis.viewSelection(pvpq).getRealPart(),
				mis.viewSelection(pq).getImaginaryPart() });

		/* check tolerance */
		normF = DenseDoubleAlgebra.DEFAULT.norm(F, Norm.Infinity);
		if (verbose > 0)
			System.out.printf("(Gauss-Seidel)\n");
		if (verbose > 1) {
			System.out.printf("\n it    max P & Q mismatch (p.u.)");
			System.out.printf("\n----  ---------------------------");
			System.out.printf("\n%3d        %10.3e", i, normF);
		}
		if (normF < tol) {
			converged = true;
			if (verbose > 1)
				System.out.printf("\nConverged!\n");
		}

		/* do Gauss-Seidel iterations */
		while (!converged && i < max_it) {
			/* update iteration counter */
			i += 1;

			/* update voltage at PQ buses */
			dVpq = Sbus.viewSelection(pq).copy().assign(V.viewSelection(pq), cfunc.div).assign(cfunc.conj);
			dVpq.assign(Ybus.viewSelection(pq, null).zMult(V, null), cfunc.minus);
			for (int k : pq)
				V.set(k, cfunc.plus.apply(V.get(k), cfunc.div.apply(dVpq.get(k), Ybus.get(k, k))));

			/* update voltage at PV buses */
			if (npv > 0) {
				Sbus.viewSelection(pv).assignImaginary( Ybus.viewSelection(pv, null).zMult(V, null).assign(cfunc.conj).assign(V.viewSelection(pv), cfunc.mult).getImaginaryPart() );
				dVpv = Sbus.viewSelection(pv).copy().assign(V.viewSelection(pv), cfunc.div).assign(cfunc.conj);
				dVpv.assign(Ybus.viewSelection(pv, null).zMult(V, null), cfunc.minus);
				for (int k : pv)
					V.set(k, cfunc.plus.apply(V.get(k), cfunc.div.apply(dVpv.get(k), Ybus.get(k, k))));
				absV = V.viewSelection(pv).copy().assign(cfunc.abs);
				absV.assign(V.viewSelection(pv), cfunc.swapArgs(cfunc.div));
				V.viewSelection(pv).assign(Vm).assign(absV, cfunc.mult);
			}

			/* evalute F(x) */
			mis = Ybus.zMult(V, null).assign(cfunc.conj).assign(V, cfunc.mult).assign(Sbus, cfunc.minus);
			F = DoubleFactory1D.sparse.make(new DoubleMatrix1D[] {
					mis.viewSelection(pvpq).getRealPart(),
					mis.viewSelection(pq).getImaginaryPart() });

			/* check for convergence */
			normF = DenseDoubleAlgebra.DEFAULT.norm(F, Norm.Infinity);
			if (verbose > 1)
				System.out.printf("\n%3d        %10.3e", i, normF);
			if (normF < tol) {
				converged = true;
				if (verbose > 0)
					System.out.printf("\nGauss-Seidel power flow converged in %d iterations.\n", i);
			}
		}
		if (verbose > 0)
			if (!converged)
				System.out.printf("\nGauss-Seidel power did not converge in %d iterations.\n", i);

		return new Object[] {V, converged, i};
	}

	public static Object[] jp_gausspf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus,
			DComplexMatrix1D V0, int ref, int[] pv, int[] pq) {
		return jp_gausspf(Ybus, Sbus, V0, ref, pv, pq, Djp_jpoption.jp_jpoption());
	}

}
