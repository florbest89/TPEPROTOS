package proxy_server;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import parser.Common;
import parser.RequestObject;
import parser.RequestParser;
import parser.ResponseObject;
import parser.ResponseParser;

public class ProxySelectorProtocol implements TCPProtocol {

	private int bufSize; // Size of buffers
	private Map<String, String> usersServers = new HashMap<String, String>();
	
	//Proxy properties
	private Properties prop;
	
	
	private int port;
	private String defaultServer;
	private String admin ;
	
	// The proxy welcome message
	private String welcome_msg;
	// The proxy goodbye message
	private String goodbye_msg;

	// Proxy stats for monitoring
	private ProxyStats stats;

	// Parsers
	private RequestParser reqParser = new RequestParser();
	private ResponseParser respParser = new ResponseParser();

	// Transformation settings
	private boolean l33t;
	private boolean rotation;

	public ProxySelectorProtocol(int bufSize) throws FileNotFoundException, IOException  {
		this.bufSize = bufSize;
		l33t = false;
		rotation = false;
		stats = new ProxyStats();
		prop = new Properties();
		
		initialize();
	}
	
	private void initialize() throws FileNotFoundException, IOException{
		
		prop.load(new FileInputStream("src/resources/proxy.properties"));
		
		welcome_msg = prop.getProperty("welcome_msg");
		goodbye_msg = prop.getProperty("goodbye_msg");
		port = Integer.valueOf(prop.getProperty("pop3-port"));
		defaultServer = prop.getProperty("default-server");
		admin = prop.getProperty("admin");		
		
	}

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
		clntChan.configureBlocking(false); // Must be nonblocking to register
		// Register the selector with new channel for read and attach byte
		// buffer

		ProxyAtt attachment = new ProxyAtt(bufSize, clntChan);

		attachment.getClntWr().put(welcome_msg.getBytes());
		stats.addAccess();
		stats.addOkCode();

		clntChan.register(key.selector(), SelectionKey.OP_WRITE, attachment);
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {

		// Client socket channel has pending data
		SocketChannel channel = (SocketChannel) key.channel();

		ProxyAtt attachment = (ProxyAtt) key.attachment();
		SessionCalls calls = attachment.getCalls();
		ByteBuffer buf;
		ByteBuffer writer;

		long bytesRead;

		// See if this channel belongs to the client
		if (attachment.isClient(channel)) {

			stats.addAccess();

			buf = attachment.getClntRd();
			writer = attachment.getClntWr();

			bytesRead = channel.read(buf);

			if (bytesRead == -1) {
				channel.close();
			} else {
				
				RequestObject request = reqParser.parse(buf);
				System.out.println(request.getCommand());

				switch (request.getType()) {
				case AUTH:
					auth(attachment);
					break;
				case USER:
					logUser(request.getParams(), attachment, key);
					break;
				case TOP:
					retr(request.getParams(), attachment);
					break;
				case RETR:
					retr(request.getParams(), attachment);
					break;
				case CAPA:
					capaReq(request.getParams(), attachment);
					break;
				case L33T:
					l33t(request.getParams(), attachment);
					break;
				case ROTATION:
					rotation(request.getParams(), attachment);
					break;
				case SETSERVER:
					setServer(request.getParams(), attachment);
					break;
				case HISTOGRAM:
					histogram(request.getParams(), attachment);
					break;
				case STATS:
					stats(request.getParams(), attachment);
					break;
				case QUIT:
					quit(request.getParams(), attachment);
					break;
				case ETC:
					etc(request.getParams(), attachment);
					break;

				}
			}
		} else {

			buf = attachment.getServerRd();
			writer = attachment.getServerWr();

			bytesRead = channel.read(buf);

			// The other end closed
			if (bytesRead == -1) {
				channel.close();
			} else {

				ResponseObject respOb = respParser.parse(buf);

				System.out.println(respOb.getStatusCode() + " "
						+ respOb.getBody());

				if (calls.isWelcome()) {
					calls.setWelcome(false);
				} else {

					if (respOb.getStatusCode().toUpperCase().contains("+OK")) {
						stats.addOkCode();
					} else {
						stats.addErrCode();
					}

					if (calls.isQuiting()) {
						clntQuits(attachment);
						buf.clear();
					}

					if (calls.isEmail()) {
						//processEmail(attachment);
						retrMsg(attachment);
						key.interestOps(SelectionKey.OP_READ);
						return;
					}

					buf.flip();
					ByteBuffer clnt_wr = attachment.getClntWr();
					clnt_wr.put(buf);

					// Reading the status code for the PASS command
					if (calls.isPass()) {
						if (respOb.getStatusCode().equals("+OK")) {
							attachment.setLogState(true);
						}
						calls.setPass(false);
					} else {
						// Reading the status code for the CAPA command
						if (calls.isCapa()) {
							capaResp(attachment);
							calls.setCapa(false);
						}

					}
				}

			}

		}

		buf.clear();

		if (writer.position() != 0) {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} else {
			key.interestOps(SelectionKey.OP_READ);
		}

	}

	// Aca hay que hacer que el proxy maneje y devuelva el response del server
	@Override
	public void handleWrite(SelectionKey key) throws IOException {
		/*
		 * Channel is available for writing, and key is valid (i.e., client
		 * channel not closed).
		 */
		// Retrieve data read earlier

		ProxyAtt attachment = (ProxyAtt) key.attachment();
		long bytesTransferred;

		ByteBuffer buf;
		SocketChannel channel = (SocketChannel) key.channel();

		if (attachment.isClient((channel))) {

			buf = attachment.getClntWr();

		} else {

			buf = attachment.getServerWr();

		}

		buf.flip(); // Prepare buffer for writing
		bytesTransferred = channel.write(buf);

		stats.addBytesTransf(bytesTransferred);

		if (!buf.hasRemaining()) { // Buffer completely written?
			// Nothing left, so no longer interested in writes
			key.interestOps(SelectionKey.OP_READ);
		}

		buf.clear(); // Clear buffer

		if (attachment.getCalls().alreadyQuited()) {
			key.channel().close();
			key.interestOps(SelectionKey.OP_READ);
		} else {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
	}

	private void auth(ProxyAtt attachment) {

		ByteBuffer clnt_wr = attachment.getClntWr();
		String response = "-ERR[INVALID] Command AUTH is not supported.\r\n";

		stats.addInvalid();
		clnt_wr.put(response.getBytes());
	}

	private void logUser(List<String> params, ProxyAtt attachment,
			SelectionKey key) throws UnknownHostException, IOException {

		if (params.size() > 1) {

			if (attachment.usrProvided()) {

				ByteBuffer clnt_rd = attachment.getClntRd();

				clnt_rd.flip();
				attachment.getServerWr().put(clnt_rd);
				clnt_rd.clear();

			} else {

				// El param[0] es el comando USER
				String username = params.get(1);
				attachment.setUsrProv(true);
				String serverAddr = usersServers.get(username);

				if (serverAddr == null) {
					serverAddr = defaultServer;
				}

				System.out.println("el username es " + username
						+ " y es administrador? " + username.equals(admin));

				if (username.equals(admin)) {
					attachment.setAdmin(true);
				}

				ByteBuffer server = attachment.getServerWr();
				ByteBuffer clnt = attachment.getClntRd();
				// Pongo lo que escribio el cliente en lo que le pido al
				// servidor

				clnt.flip();

				server.put(clnt);

				clnt.clear();

				// Create a new SocketChannel for the origin server
				SocketChannel channel = SocketChannel.open();
				// Initiate connection to server and repeatedly poll until
				// complete
				if (!channel.connect(new InetSocketAddress(serverAddr, port))) {
					while (!channel.finishConnect()) {
						System.out.print("."); // Do something else
					}
				}
				channel.configureBlocking(false);

				// I register a new key with the same ProxyAtt but with
				// non-client
				// state
				channel.register(key.selector(), SelectionKey.OP_READ,
						attachment);
				// The proxy will be expecting the welcome message
				attachment.getCalls().setWelcome(true);

			}
		} else {
			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] Need to provide a user.\r\n";
			clnt_wr.put(response.getBytes());
			stats.addUsrNeeded();
		}

	}


	private void capaResp(ProxyAtt attachment) {

		ByteBuffer clnt_wr = attachment.getClntWr();

		System.out.println("Soy administrador? : " + attachment.isAdmin());
		
		
		if (attachment.isAdmin()) {

			System.out.println("user es administrador");
			
			String response = Common.transferData(clnt_wr);

			System.out.println(response);

			String adminOptions = "MONITOR\nSETTINGS\nSETSERVER\n.\r\n";

			System.out.println(response);
			response = response.replace(".", adminOptions);
			System.out.println(response);

			clnt_wr.clear();
			clnt_wr.put(response.getBytes());

		}

	}

	// CAPA request from client, differs from capa response
	private void capaReq(List<String> params, ProxyAtt attachment) {

		System.out.println("Estoy en el metodo CAPA");
		String response = "+OK\nCAPA\n";

		if (!attachment.usrProvided()) {
			response = response + "USER\nQUIT\r\n";

			ByteBuffer buf = attachment.getClntWr();

			// Put in the buffer the response from the CAPA command
			buf.put(response.getBytes());

			stats.addOkCode();

		} else {

			ByteBuffer serverbuf = attachment.getServerWr();
			ByteBuffer clntRd = attachment.getClntRd();

			clntRd.flip();
			serverbuf.put(clntRd);
			clntRd.clear();

			// The proxy will be expecting the response from the capa command
			attachment.getCalls().setCapa(true);

		}
	}

	private void l33t(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		// If the request doesn't have 2 parameters OR the 2nd parameter isn't
		// ON or OFF the request is invalid
		if (params.size() != 2
				|| (!(params.get(1).equalsIgnoreCase("ON") && !(params.get(1)
						.equalsIgnoreCase("OFF"))))) {
			statusCode = "-ERR[INVALID] Invalid parameters. \r\n";
			stats.addInvalid();
		} else {

			if (!attachment.isAdmin()) {
				statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings. \r\n";
				stats.addNotAdmin();
			} else {
				stats.addOkCode();
				if (params.get(1).equalsIgnoreCase("ON")) {
					statusCode = "+OK l33t transformation on. \r\n";
					setl33t(true);
				} else {
					if (params.get(1).equalsIgnoreCase("OFF")) {
						statusCode = "+OK l33t transformation off. \r\n";
						setl33t(false);
					}

				}
			}
		}

		ByteBuffer buf = attachment.getClntWr();
		buf.put(statusCode.getBytes());

	}

	private void quit(List<String> params, ProxyAtt attachment) {

		String statusCode = "";
		ByteBuffer writer;
		SessionCalls calls = attachment.getCalls();

		if (!attachment.usrProvided()) {

			writer = attachment.getClntWr();

			if (params.size() != 1) {
				statusCode = "-ERR[INVALID] Invalid parameters. \r\n";
				stats.addInvalid();
			} else {
				statusCode = goodbye_msg;
				stats.addOkCode();
				calls.setAlreadyQuit(true);
			}
			
			writer.put(statusCode.getBytes());

		} else {
			writer = attachment.getServerWr();
			ByteBuffer reader = attachment.getClntRd();

			reader.flip();
			writer.put(reader);
			calls.setQuit(true);

		}

	}
	
	private void clntQuits(ProxyAtt attachment) {
		
		ByteBuffer clnt_wr = attachment.getClntWr();
		SessionCalls calls = attachment.getCalls();
		
		clnt_wr.put(goodbye_msg.getBytes());
		
		calls.setQuit(false);
		calls.setAlreadyQuit(true);
		
	}

	private void etc(List<String> params, ProxyAtt attachment) {

		ByteBuffer fwd = attachment.getServerWr();
		ByteBuffer clnt_rd = attachment.getClntRd();

		
		if(attachment.usrProvided()){
			String cmd = params.get(0);
			
			// The proxy will be expecting the response from the pass command
			if (cmd.equalsIgnoreCase("pass")) {
				attachment.getCalls().setPass(true);
			}
			
			System.out.println("Comando "
					+ Common.transferData(clnt_rd));
			
			clnt_rd.flip();
			fwd.put(clnt_rd);
			clnt_rd.clear();	
			
		} else {
			
			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] Need to provide a user.\r\n";
			clnt_wr.put(response.getBytes());
			stats.addUsrNeeded();
			
		}


	}

	private void setl33t(boolean l33t) {
		this.l33t = l33t;
	}

	// agregar logs
	private void rotation(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		// If the request doesn't have 2 parameters OR the 2nd parameter isn't
		// ON or OFF the request is invalid
		if (params.size() != 2
				|| (!(params.get(1).equalsIgnoreCase("ON") && !(params.get(1)
						.equalsIgnoreCase("OFF"))))) {
			statusCode = "-ERR[INVALID] Invalid parameters. \r\n";
			stats.addInvalid();
		} else {

			if (!attachment.isAdmin()) {
				statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings.\r\n";
				stats.addNotAdmin();
			} else {
				stats.addOkCode();
				if (params.get(1).equalsIgnoreCase("ON")) {
					statusCode = "+OK Rotation transformation on. \r\n";
					setRotation(true);
				} else {
					if (params.get(1).equalsIgnoreCase("OFF")) {
						statusCode = "+OK Rotation transformation off. \r\n";
						setRotation(false);
					}

				}
			}
		}

		ByteBuffer buf = attachment.getClntWr();
		buf.put(statusCode.getBytes());

	}

	private void setRotation(boolean rotation) {
		this.rotation = rotation;
	}

	// SETSERVER username originserver
	private void setServer(List<String> params, ProxyAtt attachment) {

		String statusCode;

		if (!attachment.isAdmin()) {
			statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings.\r\n";
			stats.addNotAdmin();
		} else {
			if (params.size() == 3) {
				usersServers.put(params.get(1), params.get(2));
				statusCode = "+OK Settings changed.\r\n";
				stats.addOkCode();
			} else {
				statusCode = "-ERR[INVALID] Invalid parameters\r\n";
				stats.addInvalid();
			}
		}

		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.put(statusCode.getBytes());

	}

	// TOP msg n
	// msg: number of message to retrieve
	// n: number of lines from body to retrieve

	// RETR msg
	// retrieve the msg message
	private void retr(List<String> params, ProxyAtt attachment) {
		
		attachment.getCalls().setEmail(true);
		
		ByteBuffer srv_wr = attachment.getServerWr();
		ByteBuffer clnt_rd = attachment.getClntRd();
		
		clnt_rd.flip();
		srv_wr.put(clnt_rd);

	}
	
	//ANALIZAR
	private void retrMsg(ProxyAtt attachment){
		
		ByteBuffer srv_rd = attachment.getServerRd();
		ByteBuffer clnt_wr = attachment.getClntWr();
		
		System.out.println(Common.transferData(srv_rd));
		
		String analise = Common.transferData(srv_rd);
		
		if(analise.endsWith(".")){
			attachment.getCalls().setEmail(false);
		}
		
		srv_rd.flip();
		clnt_wr.put(srv_rd);
		srv_rd.clear();
		
		
	}

	private void histogram(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		if (params.size() != 1) {
			statusCode = "-ERR[INVALID] Invalid parameters\r\n";
			stats.addInvalid();
		} else {
			if (!attachment.isAdmin()) {
				statusCode = "-ERR[NOT ADMIN] Only the administrator can see the histogram. \r\n";
				stats.addNotAdmin();
			} else {
				stats.addOkCode();
				statusCode = "+OK The histogram is: \n" + stats.getHistogram();
			}
		}

		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.put(statusCode.getBytes());

	}

	private void stats(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		if (params.size() != 1) {
			statusCode = "-ERR[INVALID] Invalid parameters\r\n";
			stats.addInvalid();
		} else {
			if (!attachment.isAdmin()) {
				statusCode = "-ERR[NOT ADMIN] Only the administrator can see the stats. \r\n";
				stats.addNotAdmin();
			} else {
				stats.addOkCode();
				statusCode = "+OK The stats are: \n" + stats.getStats();
			}
		}

		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.put(statusCode.getBytes());
	}

	private void processEmail(ProxyAtt attachment) {
		// TODO Auto-generated method stub
		/*
		 * Deberia tener un metodo que sea para parsear el mail y que reciba un
		 * bytebuffer (el server read) y l33t y rotation para ir haciendo los
		 * cambios necesarios =)
		 * 
		 * Primero igual, que analice si el statuscode es -ERR porque siendo ese
		 * el caso me ahorro mucho. Si ese es el caso, cambio el flag del mail a
		 * false.
		 */

	}

	
}