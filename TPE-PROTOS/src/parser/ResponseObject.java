package parser;


public class ResponseObject {
	
	private String statusCode;
	private String body;
	
	public ResponseObject(String statusCode){
		this.statusCode = statusCode;	
	}
	
	public void setBody(String body){
		this.body = body;
	}
	
	public String getStatusCode(){
		return statusCode;
	}
	
	public String getBody(){
		return body;
	}
	

}
