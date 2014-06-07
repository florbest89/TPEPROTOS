package proxy_server;

public class ProxyCalls {
	
	private boolean welcome;
	private boolean pass;
	private boolean capa;
	private boolean email;
	
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
	
	public boolean isWelcome(){
		return welcome;
	}
	
	public boolean isPass(){
		return pass;
	}
	
	public boolean isCapa(){
		return capa;
	}

	public boolean isEmail() {
		return email;
	}

	public void setEmail(boolean email) {
		this.email = email;
	}
	
	

}
