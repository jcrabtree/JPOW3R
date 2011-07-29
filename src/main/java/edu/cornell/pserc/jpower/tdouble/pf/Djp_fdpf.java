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
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.SparseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.SparseDoubleLUDecomposition;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import cern.colt.util.tdouble.Djp_util;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.cornell.pserc.jpower.tdouble.Djp_jpoption;

/**
 * Solves the power flow using a fast decoupled method.
 *
 * @author Ray Zimmerman
 * @author Richard Lincoln
 *
 */
public class Djp_fdpf {

	private static final DoubleFunctions dfunc = DoubleFunctions.functions;
	private static final DComplexFunctions cfunc = DComplexFunctions.functions;

	/**
	 * Solves for bus voltages given the full system admittance matrix (for
	 * all buses), the complex bus power injection vector (for all buses),
	 * the initial vector of complex bus voltages, the FDPF matrices B prime
	 * and B double prime, and column vectors with the lists of bus indices
	 * for the swing bus, PV buses, and PQ buses, respectively. The bus voltage
	 * vector contains the set point for generator (including ref bus)
	 * buses, and the reference angle of the swing bus, as well as an initial
	 * guess for remaining magnitudes and angles. MPOPT is a MATPOWER options
	 * vector which can be used to set the termination tolerance, maximum
	 * number of iterations, and output options (see MPOPTION for details).
	 * Uses default options if this parameter is not given. Returns the
	 * final complex voltages, a flag which indicates whether it converged
	 * or not, and the number of iterations performed.
	 *
	 * @param Ybus
	 * @param Sbus
	 * @param V0
	 * @param Bp
	 * @param Bpp
	 * @param ref
	 * @param pv
	 * @param pq
	 * @param jpopt
	 * @return
	 */
	@SuppressWarnings("static-access")
	public static Object[] jp_fdpf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus, DComplexMatrix1D V0,
			DoubleMatrix2D Bp, DoubleMatrix2D Bpp, int ref, int[] pv, int[] pq, Map<String, Double> jpopt) {
		double tol, normP, normQ;
		int i, max_it, alg, verbose;
		int[] pvpq;
		boolean converged;
		DComplexMatrix1D V, Va, Vm, mis;
		DoubleMatrix1D P, Q;
		SparseCCDoubleMatrix2D CCBp, CCBpp;
		SparseDoubleLUDecomposition luP, luQ;
		DComplexMatrix1D dVa, dVm;

		/* options */
		tol	= jpopt.get("PF_TOL");
		max_it	= jpopt.get("PF_MAX_IT").intValue();
		verbose	= jpopt.get("VERBOSE").intValue();

		/* initialize */
		pvpq = Djp_util.icat(pv, pq);
		converged = false;
		i = 0;
		V = V0.copy();
		Va = V.copy().assign(cfunc.arg);
		Vm = V.copy().assign(cfunc.abs);

		/* evaluate initial mismatch */
		mis = Ybus.zMult(V, null).assign(cfunc.conj);
		mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus).assign(Vm, cfunc.div);
		P = mis.viewSelection(pvpq).getRealPart();
		Q = mis.viewSelection(pq).getImaginaryPart();

		/* check tolerance */
		normP = DenseDoubleAlgebra.DEFAULT.norm(P, Norm.Infinity);
		normQ = DenseDoubleAlgebra.DEFAULT.norm(Q, Norm.Infinity);

		if (verbose > 0) {
			alg = jpopt.get("PF_ALG").intValue();
			System.out.printf("(fast-decoupled, %s)\n", (alg == 2) ? "XB" : "BX");
		}
		if (verbose > 1) {
			System.out.printf("\niteration     max mismatch (p.u.)  ");
			System.out.printf("\ntype   #        P            Q     ");
			System.out.printf("\n---- ----  -----------  -----------");
			System.out.printf("\n  -  %3d   %10.3e   %10.3e", i, normP, normQ);
		}
		if ((normP < tol) & (normQ < tol)) {
			converged = true;
			if (verbose > 1)
				System.out.printf("\nConverged!\n");
		}

		/* reduce B matrices */
		// column-compressed format for factorisation
		CCBp = new SparseCCDoubleMatrix2D(pvpq.length, pvpq.length);
		CCBp.assign(Bp.viewSelection(pvpq, pvpq));
		CCBpp = new SparseCCDoubleMatrix2D(pq.length, pq.length);
		CCBpp.assign(Bpp.viewSelection(pq, pq));

		/* factor B matrices */
		luP = SparseDoubleAlgebra.DEFAULT.lu(CCBp, 0);
		luQ = SparseDoubleAlgebra.DEFAULT.lu(CCBpp, 0);

		/* do P and Q iterations */
		while ((!converged) & (i < max_it)) {
			/* update iteration counter */
			i += 1;

			/* -----  do P iteration, update Va  ----- */
			luP.solve(P);
			dVa = Djp_util.complex(P.assign(dfunc.neg), null);

			/* update voltage */
			Va.viewSelection(pvpq).assign(dVa, cfunc.plus);
			V = Djp_util.polar(Vm.getRealPart(), Va.getRealPart());

			/* evalute mismatch */
			mis = Ybus.zMult(V, null).assign(cfunc.conj);
			mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus).assign(Vm, cfunc.div);
			P = mis.viewSelection(pvpq).getRealPart();
			Q = mis.viewSelection(pq).getImaginaryPart();

			/* check tolerance */
			normP = DenseDoubleAlgebra.DEFAULT.norm(P, Norm.Infinity);
			normQ = DenseDoubleAlgebra.DEFAULT.norm(Q, Norm.Infinity);
			if (verbose > 1)
				System.out.printf("\n  P  %3d   %10.3e   %10.3e", i, normP, normQ);
			if ((normP < tol) & (normQ < tol)) {
				converged = true;
				if (verbose > 0)
					System.out.printf("\nFast-decoupled power flow converged in %d P-iterations and %d Q-iterations.\n", i, i-1);
				break;
			}

			/* -----  do Q iteration, update Vm  ----- */
			luQ.solve(Q);
			dVm = Djp_util.complex(Q.assign(dfunc.neg), null);

			/* update voltage */
			Vm.viewSelection(pq).assign(dVm, cfunc.plus);
			V = Djp_util.polar(Vm.getRealPart(), Va.getRealPart());

			/* evalute mismatch */
			mis = Ybus.zMult(V, null).assign(cfunc.conj);
			mis.assign(V, cfunc.mult).assign(Sbus, cfunc.minus).assign(Vm, cfunc.div);
			P = mis.viewSelection(pvpq).getRealPart();
			Q = mis.viewSelection(pq).getImaginaryPart();

			/* check tolerance */
			normP = DenseDoubleAlgebra.DEFAULT.norm(P, Norm.Infinity);
			normQ = DenseDoubleAlgebra.DEFAULT.norm(Q, Norm.Infinity);
			if (verbose > 1)
				System.out.printf("\n  Q  %3d   %10.3e   %10.3e", i, normP, normQ);
			if (normP < tol && normQ < tol) {
				converged = true;
				if (verbose > 0)
					System.out.printf("\nFast-decoupled power flow converged in %d P-iterations and %d Q-iterations.\n", i, i);
				break;
			}
		}

		if (verbose > 0)
			if (!converged)
				System.out.printf("\nFast-decoupled power flow did not converge in %d iterations.\n", i);

		return new Object[] {V, converged, i};
	}

	public static Object[] jp_fdpf(DComplexMatrix2D Ybus, DComplexMatrix1D Sbus, DComplexMatrix1D V0,
			DoubleMatrix2D Bp, DoubleMatrix2D Bpp, int ref, int[] pv, int[] pq) {
		return jp_fdpf(Ybus, Sbus, V0, Bp, Bpp, ref, pv, pq, Djp_jpoption.jp_jpoption());
	}

}
