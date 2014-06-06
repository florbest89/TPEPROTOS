package proxy_server;

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

import parser.Common;
import parser.RequestObject;
import parser.RequestParser;
import parser.ResponseObject;
import parser.ResponseParser;

public class ProxySelectorProtocol implements TCPProtocol {

	private int bufSize; // Size of I/O buffer
	private Map<String, String> usersServers = new HashMap<String, String>();
	private RequestParser reqParser = new RequestParser();
	private ResponseParser respParser = new ResponseParser();
	private String defaultServer = "localhost";
	// admin = admin@protos.com
	private String admin = "florcha@domain2.com";
	// The proxy welcome message
	private String welcome_msg = "+OK PDC02-Proxy ready.\n";
	private int port = 110;
	// Flags
	private ProxyCalls flags;
	// Transformation settings
	private boolean l33t;
	private boolean rotation;

	public ProxySelectorProtocol(int bufSize) {
		this.bufSize = bufSize;
		l33t = false;
		rotation = false;
		flags = new ProxyCalls();

	}

	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
		clntChan.configureBlocking(false); // Must be nonblocking to register
		// Register the selector with new channel for read and attach byte
		// buffer

		ProxyAtt attachment = new ProxyAtt(bufSize, clntChan);

		attachment.getClntWr().put(welcome_msg.getBytes());

		clntChan.register(key.selector(), SelectionKey.OP_WRITE, attachment);
	}

	// Aca hay que hacer que el parser lea el request del cliente
	public void handleRead(SelectionKey key) throws IOException {

		// Client socket channel has pending data
		SocketChannel channel = (SocketChannel) key.channel();

		ProxyAtt attachment = (ProxyAtt) key.attachment();
		ByteBuffer buf;

		long bytesRead;

		// See if this channel belongs to the client
		if (attachment.isClient(channel)) {

			buf = attachment.getClntRd();
			bytesRead = channel.read(buf);

			/*
			 * System.out.println("Estoy leyendo del cliente: " + new
			 * String(Common.transferData(buf)));
			 */

			if (bytesRead == -1) {
				channel.close();
			} else {

				RequestObject request = reqParser.parse(buf);

				switch (request.getType()) {
				case USER:
					logUser(request.getParams(), attachment, key);
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
				case ETC:
					etc(request.getParams(), attachment);
					break;

				}
			}
		} else {
			
			buf = attachment.getServerRd();
			bytesRead = channel.read(buf);

			// The other end closed
			if (bytesRead == -1) {
				channel.close();
			} else {
				ResponseObject respOb = respParser.parse(buf);

				if (flags.getWelcome()) {
					
					flags.setWelcome(false);
					
				} else {

					buf.flip();
					ByteBuffer clnt_wr = attachment.getClntWr();
					clnt_wr.put(buf);
					
					
					// Reading the status code for the PASS command
					if (flags.getPass()) {
						if (respOb.getStatusCode().equals("+OK")) {
							attachment.setLogState(true);
						}
						flags.setPass(false);
					} else {
						// Reading the status code for the CAPA command
						if (flags.getCapa()) {
							capaResp(attachment);
							flags.setCapa(false);
						}
					}
				}

			}

		}

		buf.clear();
		key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

	}

	// Aca hay que hacer que el proxy maneje y devuelva el response del server
	public void handleWrite(SelectionKey key) throws IOException {
		/*
		 * Channel is available for writing, and key is valid (i.e., client
		 * channel not closed).
		 */
		// Retrieve data read earlier

		ProxyAtt attachment = (ProxyAtt) key.attachment();

		ByteBuffer buf;
		SocketChannel channel = (SocketChannel) key.channel();

		if (attachment.isClient((channel))) {

			buf = attachment.getClntWr();

		} else {

			buf = attachment.getServerWr();

		}

		buf.flip(); // Prepare buffer for writing
		channel.write(buf);

		if (!buf.hasRemaining()) { // Buffer completely written?
			// Nothing left, so no longer interested in writes
			key.interestOps(SelectionKey.OP_READ);
		}

		buf.clear(); // Clear buffer
		key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}

	private void logUser(List<String> params, ProxyAtt attachment,
			SelectionKey key) throws UnknownHostException, IOException {

		if (params.size() > 1) {

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
			// Pongo lo que escribio el cliente en lo que le pido al servidor

			clnt.flip();

			server.put(clnt);

			clnt.clear();

			// Create a new SocketChannel for the origin server
			SocketChannel channel = SocketChannel.open();
			// Initiate connection to server and repeatedly poll until complete
			if (!channel.connect(new InetSocketAddress(serverAddr, port))) {
				while (!channel.finishConnect()) {
					System.out.print("."); // Do something else
				}
			}
			channel.configureBlocking(false);

			// I register a new key with the same ProxyAtt but with non-client
			// state
			channel.register(key.selector(), SelectionKey.OP_READ, attachment);
			//The proxy will be expecting the welcome message
			flags.setWelcome(true);

		} else {
			ByteBuffer clnt_wr = attachment.getClntWr();
			String response = "-ERR[USRNEEDED] need to provide a user.\n";

			clnt_wr.put(response.getBytes());
		}

	}

	private void capaResp(ProxyAtt attachment) {

		ByteBuffer clnt_wr = attachment.getClntWr();

		if (attachment.isAdmin()) {

			System.out.println("user es administrador");

			
			 String response = new String(Common.transferData(clnt_wr),
			 Charset.forName("UTF-8"));
			 
			 System.out.println(response);
			 
			String adminOptions = "HISTOGRAM\nL33T\nROTATION\nSETSERVER\n.\n";

			response = response.replace(".", adminOptions);

			clnt_wr.clear();
			clnt_wr.put(response.getBytes());

		}

	}

	// CAPA request from client, differs from capa response
	private void capaReq(List<String> params, ProxyAtt attachment) {

		System.out.println("Estoy en el metodo CAPA");
		String response = "+OK\nCAPA \n";

		if (!attachment.usrProvided()) {
			response = response + "USER\nPASS\nQUIT\n";
			System.out.println(response.getBytes().length);

			ByteBuffer buf = attachment.getClntWr();

			// Put in the buffer the response from the CAPA command
			buf.put(response.getBytes());

		} else {

			ByteBuffer serverbuf = attachment.getServerWr();
			ByteBuffer clntRd = attachment.getClntRd();

			clntRd.flip();
			serverbuf.put(clntRd);
			clntRd.clear();

			//The proxy will be expecting the response from the capa command
			flags.setCapa(true);

		}
	}

	// agregar el log
	private void l33t(List<String> params, ProxyAtt attachment) {

		String statusCode;

		if (!attachment.isAdmin()) {
			statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings. \n";
		} else {
			if (params.get(1).equalsIgnoreCase("ON")) {
				statusCode = "+OK l33t transformation on. \n";
				setl33t(true);
			} else {
				if (params.get(1).equalsIgnoreCase("OFF")) {
					statusCode = "+OK l33t transformation off. \n";
					setl33t(false);
				} else {
					statusCode = "-ERR[INVALID] Invalid parameters. \n";
				}
			}

		}

		ByteBuffer buf = attachment.getClntWr();
		buf.put(statusCode.getBytes());

	}

	private void etc(List<String> params, ProxyAtt attachment) {

		ByteBuffer fwd = attachment.getServerWr();
		ByteBuffer clnt_rd = attachment.getClntRd();

		//The proxy will be expecting the response from the pass command
		if (params.get(0).equalsIgnoreCase("pass")) {
			flags.setPass(true);
		}

		System.out.println("Comando "
				+ new String(Common.transferData(clnt_rd), Charset
						.forName("UTF-8")));

		clnt_rd.flip();
		fwd.put(clnt_rd);
		clnt_rd.clear();

	}

	private void setl33t(boolean l33t) {
		this.l33t = l33t;
	}

	// agregar logs
	private void rotation(List<String> params, ProxyAtt attachment) {

		String statusCode;

		if (!(attachment.isAdmin() && attachment.isLogged())) {
			statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings. \n";
		} else {
			if (params.get(1).equalsIgnoreCase("ON")) {
				statusCode = "+OK rotation transformation on. \n";
				setRotation(true);
			} else {
				if (params.get(1).equalsIgnoreCase("OFF")) {
					statusCode = "+OK rotation transformation off. \n";
					setRotation(false);
				} else {
					statusCode = "-ERR[INVALID] Invalid parameters. \n";
				}
			}

		}

		ByteBuffer buf = attachment.getClntWr();
		buf.flip();
		buf.put(statusCode.getBytes());

	}

	private void setRotation(boolean rotation) {
		this.rotation = rotation;
	}

	// SETSERVER username originserver
	private void setServer(List<String> params, ProxyAtt attachment) {

		String statusCode;

		if (!attachment.isAdmin()) {

		}

	}
}