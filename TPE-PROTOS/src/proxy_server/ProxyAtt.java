package proxy_server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProxyAtt {
	
	//Buffer to read from client
	private ByteBuffer clnt_rd;
	//Buffer to write for client
	private ByteBuffer clnt_wr;
	//Buffer to read from origin server
	private ByteBuffer originsr_rd;
	//Buffer to write for origin server
	private ByteBuffer originsr_wr;
	//Indicates wether the user is logged or not. 
	private boolean logged;
	//Indicates if the user is admin
	private boolean admin;
	//Client SocketChannel
	private SocketChannel clntChannel;
	
	public ProxyAtt(int bufSize,SocketChannel clntChannel){
		clnt_rd = ByteBuffer.allocate(bufSize);
		clnt_wr = ByteBuffer.allocate(bufSize);
		originsr_rd = ByteBuffer.allocate(bufSize);
		originsr_wr = ByteBuffer.allocate(bufSize);
		logged = false;
		admin = false;
		this.clntChannel = clntChannel;
	}
	
	
	public boolean isLogged(){
		return logged;
	}
	
	public boolean isAdmin(){
		return admin;
	}
		
	public boolean isClient(SocketChannel channel){
		return clntChannel.equals(channel);
	}
	
	public void setLogState(boolean state){
		logged = state;
	}
	
	public void setAdmin(boolean admin){
		this.admin = admin;
	}
	
	public ByteBuffer getClntRd(){
		return clnt_rd;
	}
	
	public ByteBuffer getClntWr(){
		return clnt_wr;
	}
	
	public ByteBuffer getServerRd(){
		return originsr_rd;
	}
	
	public ByteBuffer getServerWr(){
		return originsr_wr;
	}
	
	

}
