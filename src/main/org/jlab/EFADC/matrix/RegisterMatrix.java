package org.jlab.EFADC.matrix;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class RegisterMatrix extends AbstractMatrix<Integer> implements Matrix {

	RegisterMatrix(int size, int reg) {
		super(size, reg);
	}

	protected void initMatrix() {
		m_Matrix.clear();

		for (int i = 0; i < m_Size; i++)
			m_Matrix.add(0);
	}

	public void set(int idx, Integer val) {
		m_Matrix.set(idx, val);
	}


}
