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
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.SparseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.SparseRCDoubleMatrix2D;
import cern.colt.util.tdouble.Djp_util;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.cornell.pserc.jpower.tdouble.Djp_jpoption;

/**
 * Solves the power flow using a full Newton's method.
 *
 * @author Ray Zimmerman
 * @author Richard Lincoln
 *
 */
public class Djp_newtonpf {

	private static final DoubleFunctions dfunc = DoubleFunctions.functions;
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
	 * output options (see JPOPTION for details). Uses default options if
	 * this parameter is not given. Returns the final complex voltages, a
	 * flag which indicates whether it converged or not, and the number of
	 * iterations performed.
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
	public static Object[] jp_newtonpf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus,
			DComplexMatrix1D V0, int ref, int[] pv, int[] pq, Map<String, Double> jpopt) {
		int i, max_it, verbose, npv, npq, j1, j2, j3, j4, j5, j6;
		int[] pvpq;
		double tol, normF;
		boolean converged;
		DoubleMatrix1D F, dx;
		DoubleMatrix2D J11, J12, J21, J22, J1, J2, J;
		DComplexMatrix1D mis, V, Va, Vm, dxz;
		DComplexMatrix2D dSbus_dVm, dSbus_dVa;
		DComplexMatrix2D[] dSbus_dV;
		SparseRCDoubleMatrix2D JJ;

		/* options */
		tol	= jpopt.get("PF_TOL");
		max_it	= jpopt.get("PF_MAX_IT").intValue();
		verbose	= jpopt.get("VERBOSE").intValue();

		/* initialize */
		pvpq = Djp_util.icat(pv, pq);
		converged = false;
		i = 0;
		V = V0;
		Va = V.copy().assign(cfunc.arg);
		Vm = V.copy().assign(cfunc.abs);

		/* set up indexing for updating V */
		npv = pv.length;
		npq = pq.length;
		j1 = 0;		j2 = npv;			// j1:j2 - V angle of pv buses
		j3 = j2;	j4 = j2 + npq;		// j3:j4 - V angle of pq buses
		j5 = j4;	j6 = j4 + npq;		// j5:j6 - V mag of pq buses

		/* evaluate F(x0) */
		mis = Ybus.zMult(V, null).assign(cfunc.conj);
		mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus);
		F = DoubleFactory1D.sparse.make(new DoubleMatrix1D[] {
				mis.viewSelection(pvpq).getRealPart(),
				mis.viewSelection(pq).getImaginaryPart() });

		/* check tolerance */
		normF = DenseDoubleAlgebra.DEFAULT.norm(F, Norm.Infinity);
		if (verbose > 0)
			System.out.print("(Newton)\n");
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

		/* do Newton iterations */
		while ((!converged) & (i < max_it)) {
			/* update iteration counter */
			i += 1;

			/* evaluate Jacobian */
			dSbus_dV = Djp_dSbus_dV.jp_dSbus_dV(Ybus, V);
			dSbus_dVm = dSbus_dV[0];
			dSbus_dVa = dSbus_dV[1];

			J11 = dSbus_dVa.getRealPart().viewSelection(pvpq, pvpq).copy();
			J12 = dSbus_dVm.getRealPart().viewSelection(pvpq, pq).copy();
			J21 = dSbus_dVa.getImaginaryPart().viewSelection(pq, pvpq).copy();
			J22 = dSbus_dVm.getImaginaryPart().viewSelection(pq, pq).copy();
			J1 = DoubleFactory2D.sparse.appendColumns(J11, J12);
			J2 = DoubleFactory2D.sparse.appendColumns(J21, J22);
			J = DoubleFactory2D.sparse.appendRows(J1, J2);
			JJ = new SparseRCDoubleMatrix2D(J.rows(), J.columns());
			JJ.assign(J);

			dx = SparseDoubleAlgebra.DEFAULT.solve(JJ, F).assign(dfunc.neg);
			dxz = Djp_util.complex(dx, null);

			/* update voltage */
			if (npv > 0)
				Va.viewSelection(pv).assign(dxz.viewSelection(Djp_util.irange(j1, j2)), cfunc.plus);
			if (npq > 0) {
				Va.viewSelection(pq).assign(dxz.viewSelection(Djp_util.irange(j3, j4)), cfunc.plus);
				Vm.viewSelection(pq).assign(dxz.viewSelection(Djp_util.irange(j5, j6)), cfunc.plus);
			}

			V = Djp_util.polar(Vm.getRealPart(), Va.getRealPart());
			/* update Vm and Va again in case we wrapped around with a negative Vm */
			Va = V.copy().assign(cfunc.arg);
			Vm = V.copy().assign(cfunc.abs);

			/* evalute F(x) */
			mis = Ybus.zMult(V, null).assign(cfunc.conj);
			mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus);
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
					System.out.printf("\nNewton's method power flow converged in %d iterations.\n", i);
			}
		}
		if (verbose > 0)
			if (!converged)
				System.out.printf("\nNewton''s method power did not converge in %d iterations.\n", i);

		return new Object[] {V, converged, i};
	}

	public static Object[] jp_newtonpf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus,
			DComplexMatrix1D V0, int ref, int[] pv, int[] pq) {
		return jp_newtonpf(Ybus, Sbus, V0, ref, pv, pq, Djp_jpoption.jp_jpoption());
	}

}
