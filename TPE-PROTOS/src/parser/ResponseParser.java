package parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ResponseParser {
	
	public ResponseObject parse(ByteBuffer buf){
		
		String response = new String(buf.array(),Charset.forName("UTF-8"));
		
		int aux = response.indexOf('\0');		
		response = response.substring(0, aux - 1);
		String[] params = response.split(".");
		
		String statusCode = params[0];
		System.out.println("El status code es " + statusCode);
		
		ResponseObject respOb = new ResponseObject(statusCode);
		
		String body = "";
		for(int i = 1 ; i < params.length ; i++){
			body = body + params[i] + " ";
		}
		respOb.setBody(body);
		return respOb;
		
		
	}
	
	

}
