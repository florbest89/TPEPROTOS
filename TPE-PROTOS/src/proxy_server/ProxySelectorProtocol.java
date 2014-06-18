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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import parser.Common;
import parser.RequestObject;
import parser.RequestParser;
import parser.ResponseObject;
import parser.ResponseParser;

public class ProxySelectorProtocol implements TCPProtocol {

	private int bufSize; // Size of buffers
	private Map<String, String> usersServers = new HashMap<String, String>();
	
	//Logs
	private Logger clnt_log = (Logger) LoggerFactory.getLogger("client.log");
	private Logger srv_log = (Logger) LoggerFactory.getLogger("server.log");
	
	// Proxy properties
	private Properties prop;

	private int port;
	private String defaultServer;
	private String admin;

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

	public ProxySelectorProtocol(int bufSize) throws FileNotFoundException,
			IOException {
		this.bufSize = bufSize;
		l33t = false;
		rotation = false;
		stats = new ProxyStats();
		prop = new Properties();

		initialize();
	}

	private void initialize() throws FileNotFoundException, IOException {

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
		
		Date date = new Date();

		long bytesRead;

		// See if this channel belongs to the client
		if (attachment.isClient(channel)) {

			//See if server channel is closed. If it is, close this channel
			if (attachment.serverClosed()) {
				attachment.closeServer();
				channel.close();
				key.interestOps(SelectionKey.OP_READ);
				return;
			}
			
			stats.addAccess();

			buf = attachment.getClntRd();
			writer = attachment.getClntWr();

			bytesRead = channel.read(buf);

			if (bytesRead == -1) {
				clnt_log.info(date.toString() + " | [" + attachment.getUser() + "] : closed conection.\n");
				channel.close();
			} else {

				
				clnt_log.info(date.toString() + " | [" + attachment.getUser() + "] : " + Common.transferData(buf) + "\n" );
				
				RequestObject request = reqParser.parse(buf);

				switch (request.getType()) {
				case AUTH:
					auth(attachment);
					break;
				case USER:
					logUser(request.getParams(), attachment, key);
					break;
				case TOP:
					retr(attachment);
					break;
				case RETR:
					retr(attachment);
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
				srv_log.info(date.toString() + " | ["+attachment.getUser() + "] server closed connection.\n ");
			} else {

				 
				if (calls.isWelcome()) {
					calls.setWelcome(false);
				} else {

					if (calls.isEmail()) {
						processMail(attachment);
						key.interestOps(SelectionKey.OP_READ);
						return;
					}

					if (calls.isQuiting()) {
						clntQuits(attachment);
						buf.clear();
					}

					ResponseObject respOb = respParser.parse(buf);

					srv_log.info(date.toString() + " | [" + attachment.getUser() + "] server response: " + respOb.getStatusCode() + " " + respOb.getBody() + "\n");
					
					if (respOb.getStatusCode().toUpperCase().contains("+OK")) {
						stats.addOkCode();
						if (calls.isPass()) {
							attachment.setLogState(true);
							calls.setPass(false);
						} else {
							if (calls.isCapa()) {
								capaResp(attachment);
								calls.setCapa(false);
							} else {
								if (calls.isWtngRetr()) {
									waitingRetr(attachment);
									// I need to retrieve the status code first
									key.interestOps(SelectionKey.OP_WRITE);
									return;
								}
							}
						}
					} else {
						if (respOb.getStatusCode().toUpperCase()
								.contains("-ERR")) {
							stats.addErrCode();
							attachment.resetSessionCalls();
						}
					}

					buf.flip();
					ByteBuffer clnt_wr = attachment.getClntWr();
					clnt_wr.put(buf);

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
			if (attachment.getCalls().isRetrMail()
					&& attachment.isClient(channel)) {
				retrieveMsg(attachment);
				key.interestOps(SelectionKey.OP_WRITE);

			} else {
				key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			}
		}
	}

	//We need to tell the client we DON'T support the AUTH command
	private void auth(ProxyAtt attachment) {

		ByteBuffer clnt_wr = attachment.getClntWr();
		String response = "-ERR[INVALID] Command AUTH is not supported.\r\n";

		stats.addInvalid();
		clnt_wr.put(response.getBytes());
	}

	//Called when the client sends the USER command
	private void logUser(List<String> params, ProxyAtt attachment,
			SelectionKey key) throws UnknownHostException, IOException {

		if (params.size() > 1) {

			if (attachment.isLogged()) {

				ByteBuffer clnt_rd = attachment.getClntRd();

				clnt_rd.flip();
				attachment.getServerWr().put(clnt_rd);
				clnt_rd.clear();

			} else {

				// El param[0] es el comando USER
				String username = params.get(1);
				attachment.setUsr(username);
				String serverAddr = usersServers.get(username);

				if (serverAddr == null) {
					serverAddr = defaultServer;
				}

				if (username.equals(admin)) {
					attachment.setAdmin(true);
				}

				ByteBuffer server = attachment.getServerWr();
				ByteBuffer clnt = attachment.getClntRd();

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

				attachment.getCalls().setWelcome(true);
				attachment.setSrvChannel(channel);
				
				// I register a new key with the same ProxyAtt
				channel.register(key.selector(), SelectionKey.OP_READ,
						attachment);
				// The proxy will be expecting the welcome message

			}
		} else {
			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] Need to provide a user.\r\n";
			clnt_wr.put(response.getBytes());
			stats.addUsrNeeded();
		}

	}

	//Called before giving the response of the origin server to the client.
	//If the client is the administrator, we add the capabilities of MONITOR, SETTINGS and SETSERVER
	private void capaResp(ProxyAtt attachment) {

		ByteBuffer serv_rd = attachment.getServerRd();

		if (attachment.isAdmin()) {

			String response = Common.transferData(serv_rd);

			String adminOptions = "MONITOR\nSETTINGS\nSETSERVER\n.\r\n";

			response = response.replace(".", adminOptions);
			
			srv_log.info(new Date().toString() + " | [" + attachment.getUser() + "] server response: " + response + "\n");

			serv_rd.clear();
			serv_rd.put(response.getBytes());

		}

	}

	// CAPA request from client, differs from capa response
	private void capaReq(List<String> params, ProxyAtt attachment) {

		String response = "+OK\nCAPA\n";

		if (!attachment.usrProvided()) {
			response = response + "USER\nQUIT\n.\r\n";
			
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + response +  "\n");

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

	//Called when the l33t command is requested by the client. Only the administrator is enabled
	//to use it
	private void l33t(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		// If the request doesn't have 2 parameters OR the 2nd parameter isn't
		// ON or OFF the request is invalid
		if (params.size() != 2
				|| (!params.get(1).equalsIgnoreCase("ON") && !params.get(1)
						.equalsIgnoreCase("OFF"))) {
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

		srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode + "\n");
		ByteBuffer buf = attachment.getClntWr();
		buf.put(statusCode.getBytes());

	}
	
	private void setl33t(boolean l33t) {
		this.l33t = l33t;
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
			
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode +  "\n");

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

	//The proxy only forwards commands that aren't of use to it
	private void etc(List<String> params, ProxyAtt attachment) {


		ByteBuffer fwd = attachment.getServerWr();
		ByteBuffer clnt_rd = attachment.getClntRd();

		if (attachment.usrProvided()) {
			String cmd = params.get(0);

			// The proxy will be expecting the response from the pass command
			if (cmd.equalsIgnoreCase("pass")) {
				attachment.getCalls().setPass(true);
			}

			clnt_rd.flip();
			fwd.put(clnt_rd);
			clnt_rd.clear();

		} else {

			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] Need to provide a user.\r\n";
			clnt_wr.put(response.getBytes());
			stats.addUsrNeeded();
			
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + response + "\n");

		}

	}


	private void rotation(List<String> params, ProxyAtt attachment) {

		String statusCode = "";

		// If the request doesn't have 2 parameters OR the 2nd parameter isn't
		// ON or OFF the request is invalid
		if (params.size() != 2
				|| (!params.get(1).equalsIgnoreCase("ON") && !params.get(1)
						.equalsIgnoreCase("OFF"))) {
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
		
		srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode + "\n");

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

		srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode);
		
		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.put(statusCode.getBytes());

	}

	// TOP msg n
	// msg: number of message to retrieve
	// n: number of lines from body to retrieve

	// RETR msg
	// retrieve the msg message
	private void retr(ProxyAtt attachment) {

		if (attachment.usrProvided()) {

			attachment.getCalls().setWtngRetr(true);
			attachment.setTransformations(l33t, rotation);
			attachment.initializeParser();

			ByteBuffer srv_wr = attachment.getServerWr();
			ByteBuffer clnt_rd = attachment.getClntRd();

			clnt_rd.flip();
			srv_wr.put(clnt_rd);
		} else {
			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] Need to provide a user.\r\n";
			
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + response +  "\n");
			
			clnt_wr.put(response.getBytes());
			stats.addUsrNeeded();
		}

	}
	
	private void waitingRetr(ProxyAtt attachment) {

		// Next I'm expecting the mail
		attachment.getCalls().setEmail(true);

		ByteBuffer serv_rd = attachment.getServerRd();
		ByteBuffer clnt_wr = attachment.getClntWr();

		String response = Common.transferData(serv_rd);

		int index = response.indexOf('\r');

		String substring = response.substring(0, index);
		substring = substring + "\r\n";

		clnt_wr.put(response.getBytes());

		serv_rd.clear();
		serv_rd.put((response.substring(index + 2, response.length()))
				.getBytes());

	}

	private void processMail(ProxyAtt attachment) {

		ByteBuffer srv_rd = attachment.getServerRd();
		try {

			if (attachment.getMailParser().processMail(srv_rd)) {
				attachment.getCalls().setEmail(false);
				attachment.getCalls().setRetrMail(true);
				attachment.getClntWr().putChar('.');
			}
		} catch (IOException e) {
			
			String response = "-ERR[FAILED] Failed to retrieve mail.\r\n";
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + response);
			stats.addFailed();
			
			attachment.getClntWr().put(response.getBytes());
			attachment.getCalls().setEmail(false);
		}

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
		
		srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode + "\n");

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
		
		srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + statusCode+ "\n");

		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.put(statusCode.getBytes());
	}

	private void retrieveMsg(ProxyAtt attachment) {

		ByteBuffer clnt_wr = attachment.getClntWr();
		clnt_wr.clear();

		try {
			if (attachment.readMail(clnt_wr)) {
				attachment.getCalls().setRetrMail(false);
			}

		} catch (IOException e) {
			String response = "-ERR[FAILED] Failed to retrieve mail.\r\n";
			srv_log.info((new Date()).toString() + " | [" + attachment.getUser() + "] server response: " + response);
			stats.addFailed();
			
			clnt_wr.put(response.getBytes());
			attachment.getCalls().setRetrMail(false);
			
		}

	}

}