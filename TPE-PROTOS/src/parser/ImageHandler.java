package parser;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;


public class ImageHandler {
	
	private boolean finished;
	private String type;
	private String encoding; 			
	private String imageString;	  		
	private BufferedImage imageBuf;	  	
	private byte[] imageBytes;
	private Base64 base64Coder = new Base64(78);
	
	

	public ImageHandler(String imageType) {
		// TODO Auto-generated constructor stub
		type = imageType;
		imageString = "";
				
	}



	public boolean isFinished() {
		return finished;
	}



	public void setFinished(boolean finished) {
		this.finished = finished;
	}



	



	public String getEncoding() {
		return encoding;
	}



	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}



	public String getImageString() {
		return imageString;
	}



	public void setImageString(String imageString) {
		this.imageString = imageString;
	}



	public BufferedImage getImageBuf() {
		return imageBuf;
	}



	public void setImageBuf(BufferedImage imageBuf) {
		this.imageBuf = imageBuf;
	}



	public byte[] getImageBytes() {
		return imageBytes;
	}



	public void setImageBytes(byte[] imageBytes) {
		this.imageBytes = imageBytes;
	}



	public  void rotate180() {
			int width = this.getImageBuf().getWidth(); //the Width of the original image
			int height = this.getImageBuf().getHeight();//the Height of the original image

			BufferedImage returnImage = new BufferedImage( width, height, this.getImageBuf().getType()  );
		

			for( int x = 1; x < width; x++ ) {
				for( int y = 1; y < height; y++ ) {
					returnImage.setRGB(width - x, height -y - 1, this.getImageBuf().getRGB(x,y));
					//returnImage.setRGB(width - x – 1, height – y – 1, inputImage.getRGB( x, y  )  );
				}
			}
		
			this.setImageBuf(returnImage);


	}
	
	
public void processImage() throws IOException {
		
		
		
		this.setImageBytes(this.base64Coder.decode(this.getImageString()));
		InputStream in = new ByteArrayInputStream(this.getImageBytes());
		this.setImageBuf(ImageIO.read(in));
		
		
		//Roto
		this.rotate180();
		
		
		//Vuelvo a tranformalo en String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(this.getImageBuf(), this.getType(), baos);
		baos.flush();
		
		this.setImageString(this.base64Coder.encodeToString(baos.toByteArray()));
		baos.close();	
	}
	
	
	
	
	
public void processImageDebug() throws IOException {
		
		
		//termine de leer
		System.out.println("Imagen antes de Encoding");
		System.out.println(imageString);
		this.setImageBytes(Base64.decodeBase64(this.getImageString()));
		InputStream in = new ByteArrayInputStream(this.getImageBytes());
		imageBuf = ImageIO.read(in);
		
		//Roto
		this.rotate180();
		
		
		//Vuelvo a tranformalo en String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		//VERIFICO IMAGEN FINAL//
		////////////////////////////////////////////////
		File outputfile = new File("Rotation."+ this.getType());
	    ImageIO.write(this.getImageBuf(),this.getType(), outputfile);
	    ////////////////////////////////////////////////
	    
		ImageIO.write(this.getImageBuf(), this.getType(), baos);
		baos.flush();
		this.setImageString(Base64.encodeBase64String(baos.toByteArray()));
		baos.close();
		//imageString = new String(imageBytes);
		System.out.println("Imagen luego de Parseo");
		System.out.println(this.getImageString());
		
		//VERIFICO ENCODING FINAL//
		//////////////////////////////////////////////////////
		this.setImageBytes( Base64.decodeBase64(this.getImageString()));
		InputStream in2 = new ByteArrayInputStream(this.getImageBytes());
		this.setImageBuf( ImageIO.read(in2));
		
		File outputfile2 = new File("Final."+type);
	    ImageIO.write(this.getImageBuf(),this.getType(), outputfile2);
	    ///////////////////////////////////////////////////////
			    
		
	}




	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}



	public void addStringData(String str) {
		// TODO Auto-generated method stub
		this.setImageString(this.getImageString() + str);
		
	}

}
