package org.jlab.EFADC.matrix;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class MatrixFactory {

	public static CoincidenceMatrix newCoincidenceMatrix(int size, int regs) {

		// 27 detectors into 54 registers is hard coded into the cmp register mapping
		CoincidenceMatrix m = new CoincidenceMatrix(size, regs);

		m.initMatrix();

		return m;
	}

	public static RegisterMatrix newRegisterMatrix(int size, int regs) {

		// 27 detectors into 7 registers
		RegisterMatrix m = new RegisterMatrix(size, regs);

		m.initMatrix();

		return m;
	}
}
