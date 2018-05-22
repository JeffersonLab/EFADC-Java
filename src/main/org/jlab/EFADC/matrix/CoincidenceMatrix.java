package org.jlab.EFADC.matrix;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class CoincidenceMatrix extends AbstractMatrix<BigInteger> implements Matrix {

	CoincidenceMatrix(int size, int reg) {
		super(size, reg);
	}


	void initMatrix() {

		m_Matrix.clear();

		for (int i = 0; i < m_Size; i++) {
			m_Matrix.add(BigInteger.ZERO);
		}
	}


	/**
	 * This assumes that detector A is represented by bit A (no offset in bitmask)
	 * @param a Detector A
	 * @param b Detector B
	 * @param enable Set or clear coincidence
	 * @param symmetric Do b -> a symmetric coincidence as well
	 */
	public void setCoincident(int a, int b, boolean enable, boolean symmetric) {

		if (a >= m_Matrix.size()) {
			Logger.getLogger("global").warning(String.format("Matrix module A %d not applicable, max size: %d (index origin 0)", a, m_Matrix.size() - 1));
			return;
		} else if (b >= m_Matrix.size()) {
			Logger.getLogger("global").warning(String.format("Matrix module B %d not applicable, max size: %d (index origin 0)", b, m_Matrix.size()- 1));
			return;
		}

		BigInteger det = m_Matrix.get(a);

		//int bit = (1 << b) - 1;

		m_Matrix.set(a, (enable ? det.setBit(b) : det.clearBit(b)));

		//Logger.getLogger("global").info(String.format("det %d bit(%d) %d = %s", a, bit, b, enable ? "true" : "false"));

		// Should we do the opposite as well?
		if (symmetric) {

			det = m_Matrix.get(b);

			//bit = 1 << a;

			m_Matrix.set(b, (enable ? det.setBit(a) : det.clearBit(a)));
		}
	}

}
