package proxy_server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import parser.MailParser;

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
	//Indicates the user of this channel
	private String user;
	//Client SocketChannel
	private SocketChannel clntChannel;
	//Session flags
	private SessionCalls calls;
	//Mail parser of client
	private MailParser mailParse;
	
	public ProxyAtt(int bufSize,SocketChannel clntChannel){
		clnt_rd = ByteBuffer.allocate(bufSize);
		clnt_wr = ByteBuffer.allocate(bufSize);
		originsr_rd = ByteBuffer.allocate(bufSize);
		originsr_wr = ByteBuffer.allocate(bufSize);
		logged = false;
		admin = false;
		calls = new SessionCalls();
		mailParse = new MailParser();
		this.clntChannel = clntChannel;
		
		
	}
	
	
	public boolean isLogged(){
		return logged;
	}
	
	public boolean isAdmin(){
		return admin && logged;
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
	
	public void setUsr(String usr_prov){
		this.user = usr_prov;
	}
	
	public boolean usrProvided(){
		return user != null;
	}
	
	public SessionCalls getCalls(){
		return calls;
	}
	
	public MailParser getMailParser(){
		return mailParse;
	}
	
	public void setTransformations(boolean l33t, boolean rotation){
		mailParse.setTransformations(l33t, rotation);
	}
	
	
	public boolean initializeParser(){
		return mailParse.initializeMailFile(user);
	}


	public void resetSessionCalls() {
		calls.resetCalls();		
	}
	
	public boolean readMail(ByteBuffer readBuffer) throws IOException
	{
		return mailParse.readMail(readBuffer);
	}
	
	

}
