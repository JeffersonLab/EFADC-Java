package org.jlab.EFADC.matrix;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Enumeration;
import java.util.logging.Logger;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

/**
 * org.jlab.EFADC.matrix
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/14/13
 */
public class MatrixRegisterEncoder {

	/**
	 * Encodes a pair of coincident matrices ready to send directly to an EFADC/CMP system
	 *
	 * @param mOR OR table entry
	 * @param mAND AND table entry
	 * @param fieldWidthBits Field size of each entry in the encoded matrix (bits)
	 * @param offset Bitwise offset of each entry in each encoded field
	 * @param order
	 * @return Encoded matrix
	 */
	public static ChannelBuffer encode(CoincidenceMatrix mOR, CoincidenceMatrix mAND, int fieldWidthBits, int offset, int order) {

		assert fieldWidthBits != 0;

		int nDetectors = mOR.getBitSize();
		int nRegisters = mOR.getRegisterCount();

		//nRegisters /= 2;	// convert to bytes, each register is 16 bits (2 bytes)

		// TODO: Calculate correct size
		// Create buffer large enough to hold the entire encoded matrix
		ChannelBuffer buffer = buffer(nRegisters * 4);

		for (int i = 0; i < nDetectors; i++) {

			// the 0xfffffff masks off the high 4 bits since they are unused
			int orVal =  mOR.getEntry(i).intValue() & 0xfffffff;
			int andVal = mAND.getEntry(i).intValue() & 0xfffffff;

			buffer.writeInt(orVal);
			buffer.writeInt(andVal);

			//Logger.getGlobal().info(String.format("[%d] %08x    %08x", i, orVal, andVal));
		}

		return buffer;

	}

	/**
	 * Encodes a pair of coincident matrices ready to send directly to an EFADC/CMP system
	 *
	 * @param mOR OR table entry
	 * @param mAND AND table entry
	 * @param fieldWidthBits Field size of each entry in the encoded matrix (bits)
	 * @return Encoded matrix
	 */
	public static ChannelBuffer encode(CoincidenceMatrix mOR, CoincidenceMatrix mAND, int fieldWidthBits) {
		return encode(mOR, mAND, fieldWidthBits, 0, 0);
	}


	/**
	 *
	 * @param m
	 * @param fieldWidth Number of bits in each register field
	 * @param bitsPerEntry Number of bits each entry uses per field
	 * @return Encoded matrix
	 */
	public static ChannelBuffer encode(RegisterMatrix m, int fieldWidth, int bitsPerEntry) {

		int nEntries = m.getBitSize();

		int nRegisters = (int)Math.round((nEntries * bitsPerEntry / 16) + 0.5);

		ChannelBuffer buffer = buffer(nRegisters * 2);

		Enumeration<Integer> entries = m.elements();

		for (int i = 0; i < nRegisters; i++) {
			int field = 0;

			for (int j = 0; j < 3; j++) {
				if (!entries.hasMoreElements() && buffer.readerIndex() == buffer.capacity()) {

					Logger.getGlobal().severe("Invalid register matrix size" + entries);
					return null;
				}

				int fieldVal = entries.nextElement();
				int shiftVal = j * bitsPerEntry;

				field |= (fieldVal << shiftVal);
			}

			buffer.writeShort(field & 0xffff);

			Logger.getGlobal().info(String.format("[%d] %04x", i, field & 0xffff));
		}

		return buffer;
	}
}
