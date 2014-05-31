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
import parser.ResponseObject;
import parser.ResponseParser;

public class ProxySelectorProtocol implements TCPProtocol {

	private int bufSize; // Size of I/O buffer
	private Map<String, String> usersServers = new HashMap<String, String>();
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
		clntChan.register(key.selector(), SelectionKey.OP_READ, new ProxyAtt(
				bufSize, clntChan));
	}

	// Aca hay que hacer que el parser lea el request del cliente
	public void handleRead(SelectionKey key) throws IOException {
		// Client socket channel has pending data
		SocketChannel channel = (SocketChannel) key.channel();

		ProxyAtt attachment = (ProxyAtt) key.attachment();
		ByteBuffer buf;

		if (attachment.isClient(channel)) {
			buf = attachment.getClntRd();
			long bytesRead = channel.read(buf);
			System.out.println("Estoy leyendo del cliente");
			if (bytesRead == -1) { // Did the other end close?
				channel.close();
			} else if (bytesRead > 0) {

				// Parse the client's request
				RequestObject request = reqParser.parse(buf);
				RequestType type = request.getType();
				switch (type) {
				case USER:
					logUser(request.getParams(), attachment, key);
					break;
				case CAPA:
					capaReq(attachment);
					break;
				case L33T:
					l33t(attachment, request.getParams());
					break;
				case ROTATION:
					rotation(attachment, request.getParams());
					break;
				case ETC:
					etc(attachment, request.getParams());
					break;

				}

				// Indicate via key that reading/writing are both of interest
				// now.
				// key.interestOps(SelectionKey.OP_READ |
				// SelectionKey.OP_WRITE);
				key.interestOps(SelectionKey.OP_WRITE);

			}

		} else {

			buf = attachment.getServerRd();
			long bytesRead = channel.read(buf);
			if (bytesRead == -1) { // Did the other end close?
				channel.close();
			} else if (bytesRead > 0) {
				System.out.println("Estoy leyendo del servidor");
				System.out.println(new String(buf.array()));
				ResponseObject respOb = respParser.parse(buf);
				ByteBuffer resp = attachment.getClntWr();
				resp.put(buf.array());
				key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			}

		}
		// Como ya lei lo que me mando el servidor
		buf.clear();
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
			System.out.println("Voy a responderle al cliente "
					+ new String(buf.array()));
		} else {
			buf = attachment.getServerWr();
			System.out.println("Voy a preguntarle al servidor "
					+ new String(buf.array()));
		}
		buf.flip(); // Prepare buffer for writing
		channel.write(buf);
		if (!buf.hasRemaining()) { // Buffer completely written?
			// Nothing left, so no longer interested in writes
			key.interestOps(SelectionKey.OP_READ);
		}
		buf.compact(); // Make room for more data to be read in
	}

	private void logUser(List<String> params, ProxyAtt attachment,
			SelectionKey key) throws UnknownHostException, IOException {

		// El param[0] es el comando USER
		String username = params.get(1);
		String serverAddr = usersServers.get(username);

		if (serverAddr == null) {
			serverAddr = defaultServer;
		}

		System.out.println("Estoy en el metodo USER y el user es: " + username);

		ByteBuffer buf = attachment.getServerWr();
		// Pongo lo que escribio el cliente en lo que le pido al servidor
		buf.put(attachment.getClntRd().array());

		// Create a new SocketChannel for the origin server
		SocketChannel channel = SocketChannel.open();
		// Initiate connection to server and repeatedly poll until complete
		if (!channel.connect(new InetSocketAddress(serverAddr, port))) {
			while (!channel.finishConnect()) {
				System.out.print("."); // Do something else
			}
		}
		channel.configureBlocking(false);

		// I register a new key with the same ProxyAtt but with non-client state
		channel.register(key.selector(), SelectionKey.OP_READ, attachment);

	}

	// CAPA request from client, differs from capa response
	private void capaReq(ProxyAtt attachment) {

		System.out.println("Estoy en el metodo CAPA");
		String response = "+OK\nCAPA \n";

		if (!attachment.isLogged()) {
			response = response + "USER\nPASS\nQUIT\n";
			System.out.println(response.getBytes().length);

			ByteBuffer buf = attachment.getClntWr();

			// Put in the buffer the response from the CAPA command
			buf.put(response.getBytes());
		} else {
			// I have to obtain the result from CAPA command from the origin
			// server
			attachment.setClient(false);
		}
	}

	// agregar el log
	private void l33t(ProxyAtt attachment, List<String> params) {

		String statusCode;

		if (!attachment.isAdmin()) {
			statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings. \n";
		} else {
			if (params.get(0).equals("ON")) {
				statusCode = "+OK l33t transformation on. \n";
				setl33t(true);
			} else {
				if (params.get(0).equals("OFF")) {
					statusCode = "+OK l33t transformation off. \n";
					setl33t(false);
				} else {
					statusCode = "-ERR[INVALID] Invalid parameters. \n";
				}
			}

		}

		ByteBuffer buf = attachment.getClntWr();
		buf.flip();
		buf.put(statusCode.getBytes());

	}

	private void etc(ProxyAtt attachment, List<String> params) {

		String cmd = "";

		for (String each : params) {
			cmd = cmd + each + " ";
		}

		ByteBuffer fwd = attachment.getServerWr();
		fwd.put(cmd.getBytes());

		attachment.setClient(false);

	}

	private void setl33t(boolean l33t) {
		this.l33t = l33t;
	}

	// agregar logs
	private void rotation(ProxyAtt attachment, List<String> params) {
		String statusCode;

		if (!attachment.isAdmin()) {
			statusCode = "-ERR[NOT ADMIN] Only the administrator can change settings. \n";
		} else {
			if (params.get(0).equals("ON")) {
				statusCode = "+OK rotation transformation on. \n";
				setRotation(true);
			} else {
				if (params.get(0).equals("OFF")) {
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
}