package proxy_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.RequestObject;
import parser.RequestParser;
import parser.RequestType;
import parser.ResponseParser;

public class ProxySelectorProtocol implements TCPProtocol {
	
    private int bufSize; // Size of I/O buffer
    private Map<String,String> usersServers = new HashMap<String,String>();
    private RequestParser reqParser = new RequestParser();
    private ResponseParser respParser = new ResponseParser();
    private String defaultServer = "localhost";
    private String admin = "admin@protos.com";
    private int port = 110;
    private boolean l33t;
    private boolean rotation;

    public ProxySelectorProtocol(int bufSize) {
        this.bufSize = bufSize;
        l33t = false;
        rotation = false;
    }

    public void handleAccept(SelectionKey key) throws IOException {
        SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
        clntChan.configureBlocking(false); // Must be nonblocking to register
        // Register the selector with new channel for read and attach byte
        // buffer
        clntChan.register(key.selector(), SelectionKey.OP_READ, new ProxyAtt(ByteBuffer.allocate(bufSize)));
    }

    
    //Aca hay que hacer que el parser lea el request del cliente
    public void handleRead(SelectionKey key) throws IOException {
        // Client socket channel has pending data
        SocketChannel clntChan = (SocketChannel) key.channel();
        
        ProxyAtt attachment = (ProxyAtt) key.attachment();
        
        ByteBuffer buf = attachment.getBuffer();
        long bytesRead = clntChan.read(buf);
        if (bytesRead == -1) { // Did the other end close?
            clntChan.close();
        } else if (bytesRead > 0) {
        	
        	RequestObject request = reqParser.parse(buf);
        	RequestType type = request.getType();
        	switch(type){
        		case USER: logUser(request.getParams(), attachment);
        		case PASS: passwd(request.getParams(),attachment);
        		case CAPA: capa(attachment);
        		case L33T: l33t(attachment, request.getParams());
        		case ROTATION: rotation(attachment,request.getParams());
        	}
            // Indicate via key that reading/writing are both of interest now.
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }


	//Aca hay que hacer que el proxy maneje y devuelva el response del server
    public void handleWrite(SelectionKey key) throws IOException {
        /*
         * Channel is available for writing, and key is valid (i.e., client
         * channel not closed).
         */
        // Retrieve data read earlier
    	
    	ProxyAtt attachment = (ProxyAtt) key.attachment();
    	
        ByteBuffer buf = attachment.getBuffer();
        //buf.flip(); // Prepare buffer for writing
        SocketChannel clntChan = (SocketChannel) key.channel();
        clntChan.write(buf);
        if (!buf.hasRemaining()) { // Buffer completely written?
            // Nothing left, so no longer interested in writes
            key.interestOps(SelectionKey.OP_READ);
        }
        buf.compact(); // Make room for more data to be read in
    }
    
    private void logUser(List<String> params, ProxyAtt attachment) throws UnknownHostException, IOException{
    	
    	String username = params.get(0);
    	String serverAddr = usersServers.get(username);
    	Socket socket;
    	
    	if(serverAddr != null){
    		socket = new Socket(InetAddress.getByName(serverAddr),port);    		
    	} else{
    		socket = new Socket(InetAddress.getByName(defaultServer),port);
    	}
    	
    	InputStream in = socket.getInputStream();
    	OutputStream out = socket.getOutputStream();
    	
    	String request = "USER " + username;
    	byte[] data = request.getBytes();
    	
    	out.write(data);
    	
    	//FALTA CONTINUAR
    	
    }
    
    
    private void passwd(List<String> params, ProxyAtt attachment) {
    	// TODO Auto-generated method stub
    	
    }
    
    private void capa(ProxyAtt attachment){
    	
    	String response = "CAPA \n";
    	
    	if(!attachment.isLogged()) {
    		response = response + "CAPA \n USER \n PASS \n QUIT \n";
    	} else {
    		if (attachment.isLogged() && attachment.isAdmin()){
    			response = response + "CAPA \n TOP \n HISTOGRAM \n L33T \n ROTATION \n QUIT \n ";
    		} else {
    			response = response + "CAPA \n TOP \n QUIT \n";
    		}
    	}
    	
    	ByteBuffer buf = attachment.getBuffer();
    	buf.flip();
    	//Put in the buffer the response from the CAPA command
    	buf.put(response.getBytes());
    	
    }
    
    //agregar el log
    private void l33t(ProxyAtt attachment, List<String> params){
    	
    	String statusCode;
    	
    	if(!attachment.isAdmin()){
    		statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings \n";
    	} else {
    		if(params.get(0).equals("ON")){
    			statusCode = "+OK l33t transformation on \n";
    			setl33t(true);    			
    		} else {
    			if(params.get(0).equals("OFF")){
    				statusCode = "+OK l33t transformation off \n";
    				setl33t(false);
    			} else { 
    				statusCode = "-ERR[INVALID] Invalid parameters \n";
    			}
    		}
    		
    	}
    	
    	ByteBuffer buf = attachment.getBuffer();
    	buf.flip();
    	buf.put(statusCode.getBytes());
    	
    	
    }
    
    private void setl33t(boolean l33t){
    	this.l33t = l33t;
    }
    
    //agregar logs
    private void rotation(ProxyAtt attachment,List<String> params){
    	String statusCode;
    	
    	if(!attachment.isAdmin()){
    		statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings \n";
    	} else {
    		if(params.get(0).equals("ON")){
    			statusCode = "+OK rotation transformation on \n";
    			setRotation(true);    			
    		} else {
    			if(params.get(0).equals("OFF")){
    				statusCode = "+OK rotation transformation off \n";
    				setRotation(false);
    			} else { 
    				statusCode = "-ERR[INVALID] Invalid parameters \n";
    			}
    		}
    		
    	}
    	
    	ByteBuffer buf = attachment.getBuffer();
    	buf.flip();
    	buf.put(statusCode.getBytes());
    	
    }
    
    private void setRotation(boolean rotation){
    	this.rotation = rotation;
    }
}