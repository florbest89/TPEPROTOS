package parser;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class MailParser {

	//Variables de Clase
	private static final String CONTENT_TYPE = "content-type";
	private static final String IMAGE_TYPE = "image/";
	private static final String TEXT_PLAIN_TYPE = "text/plain";
	private static final String TRANSFER_ENCODING = "content-transfer-encoding";
	
	//Variables de Instancia
	private boolean isPlain = false;
	private boolean isImage = false;
	private boolean pendingBoundary = false;
	private boolean contentReady = false;
	private boolean leetIsActivated = true;
	private boolean rotationIsActivated = true;
	
	private String actualBoundary= "";
	private String partialBoundary = "";	
	private String actualType = "";
	private ImageHandler attachedImage;
	
	
	public void setTransformations(boolean l33t, boolean rotation){
		this.leetIsActivated = l33t;
		this.rotationIsActivated = rotation;
	}


	public boolean processMail(ByteBuffer server_rd) throws IOException {
		// TODO Auto-generated method stub
		
		
		String bufStr = Common.transferData(server_rd);
		CharBuffer buf = CharBuffer.wrap(bufStr);
		System.out.println("Muestro CharBuffer");
		System.out.println(buf);
		
		boolean isEmpty = false;
		while (!isEmpty){
			
			char[] line = getLine(buf);
			if (line != null){
				//Analizo la linea
				
				
				String str = cleanLine(line);
				
				if (pendingBoundary)
				{
					//Junto las partes de la Boundary
					str = joinSplittedBoundary(partialBoundary, str);
					
				}
				
				if (isEmptyLine(str) && (isPlain || isImage))
				{
					//Ya tengo el tipo y lei la linea vacia, lo proximo que viene es el contenido
					contentReady = true;
					
				}else if (foundBoundary(str))
				{
					//Me fijo si recibi una linea completa
					if (boundaryIsComplete(str))
					{
						//Si llegue al final de la seccion, limpio los tipos actuales y reseto los flags
						String boundary = filterBoundary(str);
						if (actualBoundary != null && boundary.contains(actualBoundary))
						{
							//Reseteo los Tipos
							resetTypes();
							//Verifico si tengo que procesar una imagen
							if(attachedImage !=null && attachedImage.isFinished()){
								if (rotationIsActivated)
									attachedImage.processImage();
								returnProcessedImage();
								attachedImage = null;
							}
								
						}
						actualBoundary = boundary;								
					}else{
						//Si no esta completa, me guardo lo que tengo hasta el momento
						savePartialBoudary(str);
						
					}
					
				}
				if (!contentReady){						
				
					//Considero un posible header
					String[] headerLine = parseHeader(str);
					
					if (isHeader(headerLine))
					{
						//Obtengo Header y Value
						String headerName = getHeaderName(headerLine);
						String headerValue = getHeaderValue(headerLine);
						
						//Analizo Header
						if (isContentType(headerName))
						{
							//Obtengo el Tipo y Actualizo Flags
							actualType = getContentType(headerValue);
							checkTypes();									
							
						}else if (isTransferEncoding(headerName))
						{
							//Obtengo el Encoding
							if(attachedImage != null){
								attachedImage.setEncoding(headerValue);
							}
							
							
						}
					 
					}
				}else{
					//Contenido Listo
					if (isPlain && leetIsActivated)
						str = l33tTransformation(str);
					
					if (isImage)
						loadImage(str);
				}
										
				if (!pendingBoundary){
						if (!(contentReady && isImage))
							System.out.print(str);
							//writer.write(str);
				}
			}else{
				//Lei linea NULL
				isEmpty = true;
				buf.flip();
			}
		}
		
		String str = buf.toString();
		//FIN DEL MAIL
		if (isEndOfMail(str))
		{
			return true;					
		}	
		
		return false;
		
	}
	
	
	private void returnProcessedImage() {
		System.out.print("\r\n" + attachedImage.getImageString() + "\r\n");	
		//writer.write("\n" + attachedImage.getImageString() + "\n");		
	}

	private static boolean isEmptyLine(String str) {
		return str.equals("\n");
	}

	private void savePartialBoudary(String str) {
		partialBoundary = str;
		pendingBoundary = true;		
	}

	private static String getContentType(String headerValue) {
		return headerValue.split(";")[0];
		
	}

	private static boolean isTransferEncoding(String headerName) {
		
		return headerName.contains(TRANSFER_ENCODING);
	}

	private static boolean isContentType(String headerName) {
		
		return headerName.contains(CONTENT_TYPE);
	}

	private static String getHeaderValue(String[] headerLine) {
		
		return headerLine[1];
	}

	private static String getHeaderName(String[] headerLine) {
		
		return headerLine[0].toLowerCase();
	}

	private static boolean isHeader(String[] headerLine) {
		
		return headerLine.length >=2;
	}

	private static String[] parseHeader(String str) {
		
		return str.split(":");
	}

	private static boolean isEndOfMail(String str) {
		
		return str.contains("\r\n.\r\n");
	}

	private String joinSplittedBoundary(String part,
			String str) {
		str = part.concat(str);
		partialBoundary = "";
		pendingBoundary = false;
		return str;
	}

	private static String cleanLine(char[] line) {
		// TODO Auto-generated method stub
		return String.valueOf(line).split("\0")[0];
		
	}

	private static String filterBoundary(String str) {
		// TODO Auto-generated method stub		
		return str.replace("-", "");
	}

	private void loadImage(String str) {
		if (attachedImage != null){
			String inLineStr = makeInLine(str);
			attachedImage.addStringData(inLineStr);
		}
		
	}
	

	private static String makeInLine(String str) {
		return str.replace("\n", "");
	}

	private static String l33tTransformation(String str) {
		str = str.toLowerCase();
		str = str.replace("a", "4");
		str = str.replace("c", "<");
		str = str.replace("e", "3");
		str = str.replace("i", "1");
		str = str.replace("o", "0");
		return str;
	}

	private static boolean boundaryIsComplete(String str) {
		
		return str.endsWith("\n");
	}

	private static boolean foundBoundary(String str) {
		
		return str.startsWith("--");
	}

	private static char[] getLine(CharBuffer buffer) {
		
		char[] ans = new char[257];
		int i;
		boolean foundLine = false;
		
		//Si me llego un buffer vacio, devuelvo null
		if (!buffer.hasRemaining())
			return null;
		
		//Cargo mi respuesta hasta encontrar un /n
		for (i=0; !foundLine ; i++)
		{
			ans[i] = buffer.get();
			if ( (ans[i] == '\n') || (!buffer.hasRemaining()) )
				foundLine = true;
				ans[i+1]='\0';
			
		}		
			
		return ans;
		
	}
	
	/**
	 * 
	 */
	private void checkTypes(){
		
		
		if (actualType.contains(IMAGE_TYPE))
		{
			//Es IMAGE, obtengo el Tipo de Imagen
			attachedImage = new ImageHandler(actualType.split("/")[1]);
			
			isImage = true;			
		}else{
			isImage = false;
		}
		
		if (actualType.contains(TEXT_PLAIN_TYPE))
		{
			//Es PLAIN TEXT
			isPlain = true;
		}else{
			isPlain = false;
		}
	}
	
	private void resetTypes(){
		if(this.isImage){
			attachedImage.setFinished(true);
		}
		isImage = false;
		isPlain = false;
		contentReady = false;
		actualType = "";
		
		
				
	}
	
	
}
