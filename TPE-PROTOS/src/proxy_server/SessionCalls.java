package proxy_server;

public class SessionCalls {
	
	private boolean welcome;
	private boolean pass;
	private boolean capa;
	private boolean email;
	private boolean quit;
	private boolean alreadyquit;
	
	public boolean alreadyQuited() {
		return alreadyquit;
	}

	public void setAlreadyQuit(boolean alreadyquit) {
		this.alreadyquit = alreadyquit;
	}

	public SessionCalls(){}
	
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
	
	public void setQuit(boolean quit){
		this.quit = quit;
	}
	
	public boolean isQuiting(){
		return quit;
	}
	
	

}
