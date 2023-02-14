package clientForNonBlockingServer.client;

import java.util.Arrays;


/**
 * This class contains static methods and attributes useful for message creation and processing.
 * @author xsala
 *
 */
public abstract class MessageUtilities {
	
    public static final String DEFAULT_SECRET_KEY_ALGORITHM = "AES";

    public static final String DEFAULT_PAIR_KEY_ALGORITHM = "RSA";

    public static final String DEFAULT_MAC_SK_ALGORITHM = "RawBytes";
    
    public static final int PRIVATE_KEY_SIZE = 256;
    
    public static final int SHARED_SECRET_KEY_COMPLEMENT = 98123;
    
    public static final String SERVER_NAME = "server";
    
    public static final String CLIENT_UNNAMED = "noName";
	
	//Use nextMessageId++
    private static long nextMessageId = 0;
    
    

	
    public static short byteArrayToShort(byte[] b) {
        return  (short) (((b[0] & 0xFF) << 8 ) |
                ((b[1] & 0xFF) << 0 ));
    }

    public static byte[] shortToByteArray(short s) {
        return new byte[] {
                (byte)(s >>> 8),
                (byte)s};
    }

    public static byte[] longToByteArray(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
    
    public static byte[] intToByteArray (int i) {
        return new byte[] {
                (byte)(i >>> 24),
                (byte)(i >>> 16),
                (byte)(i >>> 8),
                (byte) i };
    }
    
	public static int byteArrayToInt(byte[] bytes) {
	     return ((bytes[0] & 0xFF) << 24) | 
	             ((bytes[1] & 0xFF) << 16) | 
	             ((bytes[2] & 0xFF) << 8 ) | 
	             ((bytes[3] & 0xFF) << 0 );
	}

    public static long byteArrayToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static byte[] byteArraysToByteArray(byte[][] arrays){

        int newLength = 0;
        for(byte[] array : arrays) {
            newLength += array.length;
        }

        byte[] finalArray = null;

        int position = 0;

        for (byte[] array: arrays) {
            if (finalArray == null) {
                finalArray = Arrays.copyOf(array, newLength);
                position = array.length;
            } else {
                System.arraycopy(array, 0, finalArray, position, array.length);
                position += array.length;
            }
        }

        return finalArray;
    }

    public static long newMessageId(){
        return nextMessageId++;
    }
    
    
}
