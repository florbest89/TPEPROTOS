package parser;

public enum RequestType {
	USER("USER") , PASS("PASS"), HISTOGRAM("HISTOGRAM") , L33T("L33T") , ROTATION("ROTATION") , CAPA("CAPA") , SETSERVER("SETSERVER"), TOP("TOP"),  QUIT("QUIT"); 

	private String type;
	
	private RequestType(String type){
		this.type = type;
	}
	
	public String getType(){
		return type;
	}
	
	
	
}
