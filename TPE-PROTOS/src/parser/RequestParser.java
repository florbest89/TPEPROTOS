package parser;

import java.nio.ByteBuffer;

public class RequestParser {
	
	public RequestObject parse(ByteBuffer buffer){
		
		String request = new String(buffer.array());
		String[] params = request.split(" ");
		
		RequestObject reqOb = new RequestObject();
		String cmd = params[0];
		
		if(cmd.equals("USER")){
			reqOb.setType(RequestType.USER);
		}else{
			if(cmd.equals("PASS")){
				reqOb.setType(RequestType.PASS);
			} else {
				if(cmd.equals("CAPA")){
					reqOb.setType(RequestType.CAPA);
				} else {
					if(cmd.equals("TOP")){
						reqOb.setType(RequestType.TOP);
					} else {
						if(cmd.equals("HISTOGRAM")){
							reqOb.setType(RequestType.HISTOGRAM);
						} else {
							if(cmd.equals("L33T")){
								reqOb.setType(RequestType.L33T);
							} else {
								if(cmd.equals("ROTATION")){
									reqOb.setType(RequestType.ROTATION);
								} else {
									if(cmd.equals("SETSERVER")){
										reqOb.setType(RequestType.SETSERVER);
									} else {
										if(cmd.equals("QUIT")){
											reqOb.setType(RequestType.QUIT);
										} 
									}
								}
							}
						}
					}
				}
			}
		}
		
		for(int i=1; i < params.length ; i++ ){
			reqOb.addParamS(params[i]);
		}
		
		return reqOb;
		
	}

}
