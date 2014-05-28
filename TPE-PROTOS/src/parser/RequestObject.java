package parser;

import java.util.ArrayList;
import java.util.List;

public class RequestObject {
	
	private RequestType type;
	private List<String> params;
	
	public RequestObject(RequestType type){
		this.type = type;
		params = new ArrayList<String>();
	}
	
	public void addParam(String param){
		params.add(param);
	}
	
	public RequestType getType(){
		return type;
	}
	
	public List<String> getParams(){
		return params;
	}

}
