package org.jlab.EFADC.matrix;

import java.util.Enumeration;
import java.util.Vector;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public abstract class AbstractMatrix<T extends Number> {

	protected int m_Size;
	protected int m_Registers;
	protected Vector<T> m_Matrix;

	AbstractMatrix(int size, int registers) {
		m_Size = size;
		m_Registers = registers;

		m_Matrix = new Vector<>(size);

		initMatrix();
	}

	public int getBitSize() {
		return m_Size;
	}

	public int getRegisterCount() {
		return m_Registers;
	}

	public T getEntry(int idx) {
		return m_Matrix.get(idx);
	}

	/*
	@Deprecated
	public void setEntry(int idx, T value) {
		m_Matrix.set(idx, value);
	}
	*/

	public Enumeration<T> elements() {
		return m_Matrix.elements();
	}

	abstract protected void initMatrix();
}
