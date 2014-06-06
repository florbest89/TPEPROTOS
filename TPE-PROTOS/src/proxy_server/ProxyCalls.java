package proxy_server;

public class ProxyCalls {
	
	private boolean welcome;
	private boolean pass;
	private boolean capa;
	
	public ProxyCalls(){}
	
	public void setWelcome(boolean welcome){
		this.welcome = welcome;
	}
	
	public void setPass(boolean pass){
		this.pass = pass;
	}
	
	public void setCapa(boolean capa){
		this.capa = capa;
	}
	
	public boolean getWelcome(){
		return welcome;
	}
	
	public boolean getPass(){
		return pass;
	}
	
	public boolean getCapa(){
		return capa;
	}
	
	

}
