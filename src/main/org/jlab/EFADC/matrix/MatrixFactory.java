package org.jlab.EFADC.matrix;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class MatrixFactory {

	public static CoincidenceMatrix newCoincidenceMatrix(int matrixSize, int registers) {

		// 27 dtectors into 54 registers is hard coded into the cmp register mapping
		CoincidenceMatrix m = new CoincidenceMatrix(matrixSize, registers);

		m.initMatrix();

		return m;
	}

	public static RegisterMatrix newRegisterMatrix(int matrixSize, int registers) {

		// 27 detectors into 7 registers
		RegisterMatrix m = new RegisterMatrix(matrixSize, registers);

		m.initMatrix();

		return m;
	}
}
