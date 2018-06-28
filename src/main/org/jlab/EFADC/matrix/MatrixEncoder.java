package org.jlab.EFADC.matrix;

import org.jboss.netty.buffer.ChannelBuffer;

import java.math.BigInteger;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
@Deprecated
public class MatrixEncoder {

	private CoincidenceMatrix m_Matrix;

	//private int m_Offset;
	//private int m_Size;



	/**
	 * Constructor
	 * @param m Matrix to be encoded
	 */
	public MatrixEncoder(CoincidenceMatrix m) {
		m_Matrix = m;
		//m_Size = size;
		//m_Offset = offset;
	}


	/**
	 *
	 * @param size Field size of each entry in the encoded matrix
	 * @param offset Bitwise offset of each entry in each encoded field
	 * @param order
	 * @return
	 */
	public ChannelBuffer encode(int size, int offset, int order) {

		assert(size == 0);

		int nDetectors = m_Matrix.getBitSize();

		// Create buffer large enough to hold the entire encoded matrix
		ChannelBuffer buffer = buffer(nDetectors * size);

		for (int i = 0; i < nDetectors; i++) {

			BigInteger entry = m_Matrix.getEntry(i);

			// Be smart about this container size in the future
			int iVal = entry.intValue();

			buffer.writeInt(iVal);
		}

		return buffer;

	}

	public ChannelBuffer encode(int size) {
		return encode(size, 0, 0);
	}
}
