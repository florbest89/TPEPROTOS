package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RequestParser {

	public RequestObject parse(ByteBuffer buffer) {

		String request = new String(buffer.array(),Charset.forName("UTF-8"));
		
		int aux = request.indexOf('\0');		
		request = request.substring(0, aux - 1);
		String[] params = request.split(" ");		

		RequestObject reqOb = new RequestObject();
		String cmd = params[0];
		

		if (cmd.equalsIgnoreCase("USER")) {
			reqOb.setType(RequestType.USER);
		} else {
			if (cmd.equalsIgnoreCase("CAPA")) {
				reqOb.setType(RequestType.CAPA);
			} else {
				if (cmd.equalsIgnoreCase("TOP")) {
					reqOb.setType(RequestType.TOP);
				} else {
					if (cmd.equalsIgnoreCase("HISTOGRAM")) {
						reqOb.setType(RequestType.HISTOGRAM);
					} else {
						if (cmd.equalsIgnoreCase("L33T")) {
							reqOb.setType(RequestType.L33T);
						} else {
							if (cmd.equalsIgnoreCase("ROTATION")) {
								reqOb.setType(RequestType.ROTATION);
							} else {
								if (cmd.equalsIgnoreCase("SETSERVER")) {
									reqOb.setType(RequestType.SETSERVER);
								} else {
									if (cmd.equalsIgnoreCase("QUIT")) {
										reqOb.setType(RequestType.QUIT);
									} else {
										reqOb.setType(RequestType.ETC);
									}
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < params.length; i++) {
			reqOb.addParams(params[i]);
		}

		return reqOb;

	}

}
