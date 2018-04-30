package org.jlab.EFADC.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.*;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/5/12
 * Time: 10:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class ErrorFormatter extends Formatter {

	DateFormat dateFormat;
	StringBuilder buf;

	public ErrorFormatter() {
		dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		buf = new StringBuilder(1000);
	}

	public String format(LogRecord rec) {
		
		buf.setLength(0);

		buf.append(dateFormat.format(new Date(rec.getMillis())));
		buf.append(" [");

		buf.append(rec.getLevel());

		//buf.append("] (" + Thread.currentThread().getName() + ") ");
		buf.append("] ");

		StringTokenizer tok = new StringTokenizer(rec.getSourceClassName(), ".");
		int numTokens = tok.countTokens();
		for (int i = 0; i < numTokens - 1; i++)
			tok.nextToken();

		String className = tok.nextToken();

		//Chop off inner-class names
		int idx = className.indexOf("$");
		if (idx > 0)
			className = className.substring(0, idx);

		buf.append(className + ":: ");
		buf.append(formatMessage(rec));
		buf.append('\n');

		return buf.toString();

		/*
		return String.format("%s [%s] (%s) %s:: %s",
			dateFormat.format(new Date(rec.getMillis())),
			rec.getLevel(),
			Thread.currentThread().getName(),
			rec.getSourceClassName(),
			formatMessage(rec));
		*/
	}

}
