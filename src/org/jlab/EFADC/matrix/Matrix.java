package org.jlab.EFADC.matrix;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public interface Matrix<T extends Number> {
	public T getEntry(int idx);

	// Deprecated, use SetCoincident instead
	@Deprecated
	//public void setEntry(int idx, T value);

	public int getBitSize();
}
