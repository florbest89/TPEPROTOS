package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Common {

	public static String transferData(ByteBuffer source) {

		int size = source.position();
		char debug;

		byte[] dest = new byte[size];

		if (size == 1) {
			return "";
		}

		for (int i = 0; i < size; i++) {
			debug = (char)source.get(i);
			dest[i] = source.get(i);
		}

		return new String(dest, Charset.forName("UTF-8"));

	}
	
	

}
