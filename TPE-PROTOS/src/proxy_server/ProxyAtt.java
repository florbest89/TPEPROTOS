package proxy_server;

import java.net.Socket;
import java.nio.ByteBuffer;

public class ProxyAtt {
	
	private ByteBuffer buffer;
	private Socket socket;
	private boolean logged;
	private boolean admin;
	
	public ProxyAtt(ByteBuffer buffer){
		this.buffer = buffer;
		logged = false;
		admin = false;
	}
	
	public void setSocket(Socket socket){
		this.socket  = socket;
	}
	
	public boolean isLogged(){
		return logged;
	}
	
	public boolean isAdmin(){
		return admin;
	}
	
	public ByteBuffer getBuffer(){
		return buffer;
	}
	
	public void setLogState(boolean state){
		logged = state;
	}
	
	public void setAdmin(boolean admin){
		this.admin = admin;
	}
	

}
