package parser;

public enum RequestType {
	USER("USER") , HISTOGRAM("HISTOGRAM") , L33T("L33T") , ROTATION("ROTATION") , CAPA("CAPA") , SETSERVER("SETSERVER"), TOP("TOP"),  QUIT("QUIT"), ETC("ETC"); 

	private String type;
	
	private RequestType(String type){
		this.type = type;
	}
	
	public String getType(){
		return type;
	}
	
	
	
}
