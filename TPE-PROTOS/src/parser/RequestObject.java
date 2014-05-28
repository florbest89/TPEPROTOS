package parser;

import java.util.ArrayList;
import java.util.List;

public class RequestObject {
	
	private RequestType type;
	private List<String> params;
	
	public RequestObject(){
		params = new ArrayList<String>();
	}
	
	public void addParamS(String param){
		params.add(param);
	}
	
	public void setType(RequestType type){
		this.type = type;
	}
	
	public RequestType getType(){
		return type;
	}
	
	public List<String> getParams(){
		return params;
	}

}
