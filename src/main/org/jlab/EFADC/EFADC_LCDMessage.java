//
//  EFADC_LCDMessage.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.io.UnsupportedEncodingException;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

public class EFADC_LCDMessage {

	static final byte LCD_CHAR =					(byte)0x03;
	static final byte LCD_CMD =						(byte)0x02;
	static final byte LCD_NoOp =					(byte)0x0;
	static final byte LCD_RH =						(byte)0x01;   /// Set DDRAM address to 00H and return cursor to home (first char)
	static final byte LCD_SetDdrAdr =				(byte)0x80;  /// Set DDRAM address
	static final byte LCD_2ndLineStartAdr =			(byte)0x40; // address of first char on second line
	static final byte LCD_SetDdrTo2ndLineStartAdr =	(byte)0xC0;
	
	public static ChannelBuffer encode(int line, String message) {

		ChannelBuffer buffer = buffer(71);

		byte[] ascii;

		try {
			ascii = message.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		buffer.writeByte((byte)0x5a);
		buffer.writeByte((byte)0x5a);
		buffer.writeByte((byte)0x01);
		buffer.writeByte((byte)0x00);
		buffer.writeByte((byte)0x02);
		buffer.writeByte(LCD_CMD);
		buffer.writeByte((line == 1 ? LCD_RH : LCD_SetDdrTo2ndLineStartAdr));

		int idx = 7;
		int i = 0;
		for (i = 0; i < ascii.length; i++) {
			buffer.writeByte(LCD_CHAR);
			buffer.writeByte(ascii[i]);
			idx += 2;
		}

		//Fill the rest of the array with NoOps
		for (i = idx; i < 69; i++) {
			buffer.writeByte(LCD_NoOp);
		}
		
		return buffer;
	}
}