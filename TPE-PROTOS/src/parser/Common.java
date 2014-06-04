package parser;

import java.nio.ByteBuffer;

public class Common {
	
	public static byte[] transferData(ByteBuffer source){
		
		int size = source.position() + 1;
		
		byte[] dest = new byte[size];
		
		if(size == 1){
			return dest;
		}
				
		for(int i = 0 ; i < size ; i++){
			dest[i] = source.get(i);
		}
		
		return dest;
		
	}
	

}
