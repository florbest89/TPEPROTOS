package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RequestParser {

	public RequestObject parse(ByteBuffer buffer) {

		String request = Common.transferData(buffer);

		if(request.contains("\r")){
			request = request.substring(0, request.indexOf('\r'));
		} else {
			request = request.substring(0, request.indexOf('\n'));
		}
		
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
					if (cmd.equalsIgnoreCase("RETR")) {
						reqOb.setType(RequestType.RETR);
					} else {
						if (cmd.equalsIgnoreCase("HISTOGRAM")) {
							reqOb.setType(RequestType.HISTOGRAM);
						} else {
							if (cmd.equalsIgnoreCase("STATS")) {
								reqOb.setType(RequestType.STATS);
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
											if(cmd.equalsIgnoreCase("QUIT")){
												reqOb.setType(RequestType.QUIT);
											}else {
												if(cmd.equalsIgnoreCase("AUTH")){
													reqOb.setType(RequestType.AUTH);
													
												} else{													
													reqOb.setType(RequestType.ETC);												
												}
											}
										}
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
