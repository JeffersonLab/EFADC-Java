package org.jlab.EFADC;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class Tuple2 <T1, T2> {
	public T1 _1 = null;
	public T2 _2 = null;
	
	public Tuple2(T1 o1, T2 o2) {
		_1 = o1;
		_2 = o2;
	}
	
	public static <T1, T2> Tuple2<T1, T2> o(T1 o1, T2 o2) {
		return new Tuple2<T1, T2>(o1,  o2);
	}
}
