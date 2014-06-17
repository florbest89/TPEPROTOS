package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ResponseParser {
	
	public ResponseObject parse(ByteBuffer buf){
		
		String response = Common.transferData(buf);
		String[] params;
		String sep = " ";
		
		if(!response.contains("CAPA")){
			//If the response is not of CAPA type, I read from the buffer until I find a \n
			int index = response.indexOf("\r\n");		
			response = response.substring(0, index);			
			params = response.split(" ");
		} else {
			params = response.split("\n");
			sep = "\n";
		}	
		
		String statusCode = params[0];
		
		ResponseObject respOb = new ResponseObject(statusCode);
		
		String body = "";
		for(int i = 1 ; i < params.length ; i++){
			body = body + params[i] + sep;
		}
		respOb.setBody(body);
		return respOb;
		
		
	}
	
	

}
