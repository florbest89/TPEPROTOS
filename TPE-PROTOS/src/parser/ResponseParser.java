package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ResponseParser {
	
	public ResponseObject parse(ByteBuffer buf){
		
		String response = new String(Common.transferData(buf),Charset.forName("UTF-8"));
		String[] params;
		
		if(!response.contains("CAPA")){
			int index = response.indexOf('\n');		
			response = response.substring(0, index);			
			params = response.split(" ");
		} else {
			params = response.split("\n");
		}		
		
		String statusCode = params[0];
		
		ResponseObject respOb = new ResponseObject(statusCode);
		
		String body = "";
		for(int i = 1 ; i < params.length ; i++){
			body = body + params[i] + " ";
		}
		respOb.setBody(body);
		return respOb;
		
		
	}
	
	

}
