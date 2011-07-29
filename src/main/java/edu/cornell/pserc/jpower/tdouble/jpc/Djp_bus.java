/*
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

package edu.cornell.pserc.jpower.tdouble.jpc;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.util.tdouble.Djp_util;

/**
 *
 * @author Richard Lincoln
 *
 */
public class Djp_bus {

	public static final int BUS_I		= 0;
	public static final int BUS_TYPE	= 1;
	public static final int PD			= 2;
	public static final int QD			= 3;
	public static final int GS			= 4;
	public static final int BS			= 5;
	public static final int BUS_AREA	= 6;
	public static final int VM			= 7;
	public static final int VA			= 8;
	public static final int BASE_KV	= 9;
	public static final int ZONE		= 10;
	public static final int VMAX		= 11;
	public static final int VMIN		= 12;

	public static final int LAM_P		= 13;
	public static final int LAM_Q		= 14;
	public static final int MU_VMAX	= 15;
	public static final int MU_VMIN	= 16;

	/** bus number (1 to 29997) */
	public IntMatrix1D bus_i;

	/** bus type (1 - PQ bus, 2 - PV bus, 3 - reference bus, 4 - isolated bus) */
	public IntMatrix1D bus_type;

	/** Pd, real power demand (MW) */
	public DoubleMatrix1D Pd;

	/** Qd, reactive power demand (MVAr) */
	public DoubleMatrix1D Qd;

	/** Gs, shunt conductance (MW at V = 1.0 p.u.) */
	public DoubleMatrix1D Gs;

	/** Bs, shunt susceptance (MVAr at V = 1.0 p.u.) */
	public DoubleMatrix1D Bs;

	/** area number, 1-100 */
	public IntMatrix1D bus_area;

	/** Vm, voltage magnitude (p.u.) */
	public DoubleMatrix1D Vm;

	/** Va, voltage angle (degrees) */
	public DoubleMatrix1D Va;

	/** baseKV, base voltage (kV) */
	public DoubleMatrix1D base_kV;

	/** zone, loss zone (1-999) */
	public IntMatrix1D zone;

	/** maxVm, maximum voltage magnitude (p.u.)	  (not in PTI format) */
	public DoubleMatrix1D Vmax;

	/** minVm, minimum voltage magnitude (p.u.)	  (not in PTI format) */
	public DoubleMatrix1D Vmin;

	/*
	 * included in opf solution, not necessarily in input
	 * assume objective function has units, u
	 *
	 */

	/** Lagrange multiplier on real power mismatch (u/MW) */
	public DoubleMatrix1D lam_P;

	/** Lagrange multiplier on reactive power mismatch (u/MVAr) */
	public DoubleMatrix1D lam_Q;

	/** Kuhn-Tucker multiplier on upper voltage limit (u/p.u.) */
	public DoubleMatrix1D mu_Vmax;

	/** Kuhn-Tucker multiplier on lower voltage limit (u/p.u.) */
	public DoubleMatrix1D mu_Vmin;

	/**
	 *
	 * @return the number of buses.
	 */
	public int size() {
		return (int) this.bus_i.size();
	}

	/**
	 *
	 * @return a full copy of the bus data.
	 */
	public Djp_bus copy() {
		return copy(null);
	}

	/**
	 *
	 * @param indexes
	 * @return a copy of the bus data for the given indexes.
	 */
	public Djp_bus copy(int[] indexes) {
		Djp_bus other = new Djp_bus();

		other.bus_i = this.bus_i.viewSelection(indexes).copy();
		other.bus_type = this.bus_type.viewSelection(indexes).copy();
		other.Pd = this.Pd.viewSelection(indexes).copy();
		other.Qd = this.Qd.viewSelection(indexes).copy();
		other.Gs = this.Gs.viewSelection(indexes).copy();
		other.Bs = this.Bs.viewSelection(indexes).copy();
		other.bus_area = this.bus_area.viewSelection(indexes).copy();
		other.Vm = this.Vm.viewSelection(indexes).copy();
		other.Va = this.Va.viewSelection(indexes).copy();
		other.base_kV = this.base_kV.viewSelection(indexes).copy();
		other.zone = this.zone.viewSelection(indexes).copy();
		other.Vmax = this.Vmax.viewSelection(indexes).copy();
		other.Vmin = this.Vmin.viewSelection(indexes).copy();

		if (this.lam_P != null)
			other.lam_P = this.lam_P.viewSelection(indexes).copy();
		if (this.lam_Q != null)
			other.lam_Q = this.lam_Q.viewSelection(indexes).copy();
		if (this.mu_Vmax != null)
			other.mu_Vmax = this.mu_Vmax.viewSelection(indexes).copy();
		if (this.mu_Vmin != null)
			other.mu_Vmin = this.mu_Vmin.viewSelection(indexes).copy();

		return other;
	}

	/**
	 * Updates the bus data for the given indices.
	 *
	 * @param other source bus data.
	 * @param indexes bus indices.
	 */
	public void update(Djp_bus other, int[] indexes) {

//		this.bus_i.viewSelection(indexes).assign(other.bus_i.viewSelection(indexes));
//		this.bus_type.viewSelection(indexes).assign(other.bus_type.viewSelection(indexes));
//		this.Pd.viewSelection(indexes).assign(other.Pd.viewSelection(indexes));
//		this.Qd.viewSelection(indexes).assign(other.Qd.viewSelection(indexes));
//		this.Gs.viewSelection(indexes).assign(other.Gs.viewSelection(indexes));
//		this.Bs.viewSelection(indexes).assign(other.Bs.viewSelection(indexes));
//		this.bus_area.viewSelection(indexes).assign(other.bus_area.viewSelection(indexes));
//		this.Vm.viewSelection(indexes).assign(other.Vm.viewSelection(indexes));
//		this.Va.viewSelection(indexes).assign(other.Va.viewSelection(indexes));
//		this.base_kV.viewSelection(indexes).assign(other.base_kV.viewSelection(indexes));
//		this.zone.viewSelection(indexes).assign(other.zone.viewSelection(indexes));
//		this.Vmax.viewSelection(indexes).assign(other.Vmax.viewSelection(indexes));
//		this.Vmin.viewSelection(indexes).assign(other.Vmin.viewSelection(indexes));
//
//		if (this.lam_P != null)
//			this.lam_P.viewSelection(indexes).assign(other.lam_P.viewSelection(indexes));
//		if (this.lam_Q != null)
//			this.lam_Q.viewSelection(indexes).assign(other.lam_Q.viewSelection(indexes));
//		if (this.mu_Vmax != null)
//			this.mu_Vmax.viewSelection(indexes).assign(other.mu_Vmax.viewSelection(indexes));
//		if (this.mu_Vmin != null)
//			this.mu_Vmin.viewSelection(indexes).assign(other.mu_Vmin.viewSelection(indexes));

		this.bus_i.viewSelection(indexes).assign(other.bus_i);
		this.bus_type.viewSelection(indexes).assign(other.bus_type);
		this.Pd.viewSelection(indexes).assign(other.Pd);
		this.Qd.viewSelection(indexes).assign(other.Qd);
		this.Gs.viewSelection(indexes).assign(other.Gs);
		this.Bs.viewSelection(indexes).assign(other.Bs);
		this.bus_area.viewSelection(indexes).assign(other.bus_area);
		this.Vm.viewSelection(indexes).assign(other.Vm);
		this.Va.viewSelection(indexes).assign(other.Va);
		this.base_kV.viewSelection(indexes).assign(other.base_kV);
		this.zone.viewSelection(indexes).assign(other.zone);
		this.Vmax.viewSelection(indexes).assign(other.Vmax);
		this.Vmin.viewSelection(indexes).assign(other.Vmin);

		if (this.lam_P != null)
			this.lam_P.viewSelection(indexes).assign(other.lam_P);
		if (this.lam_Q != null)
			this.lam_Q.viewSelection(indexes).assign(other.lam_Q);
		if (this.mu_Vmax != null)
			this.mu_Vmax.viewSelection(indexes).assign(other.mu_Vmax);
		if (this.mu_Vmin != null)
			this.mu_Vmin.viewSelection(indexes).assign(other.mu_Vmin);
	}

	/**
	 *
	 * @param bus
	 */
//	public void fromMatrix(DoubleMatrix2D bus) {
//
//		this.bus_i = Djp_util.intm(bus.viewColumn(BUS_I));
//		this.bus_type = Djp_util.intm(bus.viewColumn(BUS_TYPE));
//		this.Pd = bus.viewColumn(PD);
//		this.Qd = bus.viewColumn(QD);
//		this.Gs = bus.viewColumn(GS);
//		this.Bs = bus.viewColumn(BS);
//		this.bus_area = Djp_util.intm(bus.viewColumn(BUS_AREA));
//		this.Vm = bus.viewColumn(VM);
//		this.Va = bus.viewColumn(VA);
//		this.base_kV = bus.viewColumn(BASE_KV);
//		this.zone = Djp_util.intm(bus.viewColumn(ZONE));
//		this.Vmax = bus.viewColumn(VMAX);
//		this.Vmin = bus.viewColumn(VMIN);
//
//		if (bus.columns() > VMIN + 1) {
//			this.lam_P = bus.viewColumn(LAM_P);
//			this.lam_Q = bus.viewColumn(LAM_Q);
//			this.mu_Vmax = bus.viewColumn(MU_VMAX);
//			this.mu_Vmin = bus.viewColumn(MU_VMIN);
//		}
//	}

	public static Djp_bus fromMatrix(DoubleMatrix2D data) {
		Djp_bus bus = new Djp_bus();

		bus.bus_i = Djp_util.intm(data.viewColumn(BUS_I));
		bus.bus_type = Djp_util.intm(data.viewColumn(BUS_TYPE));
		bus.Pd = data.viewColumn(PD);
		bus.Qd = data.viewColumn(QD);
		bus.Gs = data.viewColumn(GS);
		bus.Bs = data.viewColumn(BS);
		bus.bus_area = Djp_util.intm(data.viewColumn(BUS_AREA));
		bus.Vm = data.viewColumn(VM);
		bus.Va = data.viewColumn(VA);
		bus.base_kV = data.viewColumn(BASE_KV);
		bus.zone = Djp_util.intm(data.viewColumn(ZONE));
		bus.Vmax = data.viewColumn(VMAX);
		bus.Vmin = data.viewColumn(VMIN);

		if (data.columns() > VMIN + 1) {
			bus.lam_P = data.viewColumn(LAM_P);
			bus.lam_Q = data.viewColumn(LAM_Q);
			bus.mu_Vmax = data.viewColumn(MU_VMAX);
			bus.mu_Vmin = data.viewColumn(MU_VMIN);
		}

		return bus;
	}

	public static Djp_bus fromMatrix(double[][] data) {
		return fromMatrix(DoubleFactory2D.dense.make(data));
	}

	public DoubleMatrix2D toMatrix() {
		boolean opf = (lam_P != null);
		return toMatrix(opf);
	}

	/**
	 *
	 * @param opf include opf data
	 * @return bus data matrix
	 */
	public DoubleMatrix2D toMatrix(boolean opf) {
		DoubleMatrix2D matrix;
		if (opf) {
			matrix = DoubleFactory2D.dense.make(size(), 17);
		} else {
			matrix = DoubleFactory2D.dense.make(size(), 13);
		}

		matrix.viewColumn(BUS_I).assign( Djp_util.dblm(this.bus_i) );
		matrix.viewColumn(BUS_TYPE).assign( Djp_util.dblm(this.bus_type) );
		matrix.viewColumn(PD).assign(this.Pd);
		matrix.viewColumn(QD).assign(this.Qd);
		matrix.viewColumn(GS).assign(this.Gs);
		matrix.viewColumn(BS).assign(this.Bs);
		matrix.viewColumn(BUS_AREA).assign( Djp_util.dblm(this.bus_area) );
		matrix.viewColumn(VM).assign(this.Vm);
		matrix.viewColumn(VA).assign(this.Va);
		matrix.viewColumn(BASE_KV).assign(this.base_kV);
		matrix.viewColumn(ZONE).assign( Djp_util.dblm(this.zone) );
		matrix.viewColumn(VMAX).assign(this.Vmax);
		matrix.viewColumn(VMIN).assign(this.Vmin);

		if (opf) {
			matrix.viewColumn(LAM_P).assign(this.lam_P);
			matrix.viewColumn(LAM_Q).assign(this.lam_Q);
			matrix.viewColumn(MU_VMAX).assign(this.mu_Vmax);
			matrix.viewColumn(MU_VMIN).assign(this.mu_Vmin);
		}

		return matrix;
	}

	public double[][] toArray() {
		boolean opf = (lam_P != null);
		return toArray(opf);
	}

	public double[][] toArray(boolean opf) {
		return toMatrix(opf).toArray();
	}

	@Override
	public String toString() {
		return toMatrix().toString();
	}

}
