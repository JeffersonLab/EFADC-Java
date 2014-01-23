package org.jlab.EFADC.matrix;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class MatrixFactory {

	public static CoincidenceMatrix newCoincidenceMatrix() {

		// 27 dtectors into 54 registers is hard coded into the cmp register mapping
		CoincidenceMatrix m = new CoincidenceMatrix(27, 54);

		m.initMatrix();

		return m;
	}

	public static RegisterMatrix newRegisterMatrix() {

		// 27 detectors into 7 registers
		RegisterMatrix m = new RegisterMatrix(27, 7);

		m.initMatrix();

		return m;
	}
}
