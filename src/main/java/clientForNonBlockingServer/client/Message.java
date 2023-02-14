package clientForNonBlockingServer.client;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

public class Message {

	MessageType messageType;
	String origin;
	String destination;
	long messageId;
	byte[] body;
	
	/**
	 * Use this to recreate a received message without MAC check (messageType == WELCOME)
	 * @param messageType
	 * @param bundle
	 */
	public Message (MessageType messageType, byte[] bundle) {
        this.messageType = messageType;

        ByteBuffer bb = ByteBuffer.allocate(bundle.length);
        bb.put(bundle);
        bb.flip();

        messageId = bb.getLong();

        byte[] originInBytes = new byte[bb.getInt()];
        bb.get(originInBytes);

        byte[] destinationInBytes = new byte[bb.getInt()];
        bb.get(destinationInBytes);

        body = new byte[bb.getInt()];
        bb.get(body);

        this.origin = new String (originInBytes, StandardCharsets.UTF_8);
        this.destination = new String (destinationInBytes, StandardCharsets.UTF_8);
		
	}
	
	
	/**
     * Constructor from {@code MessageType} and [@code byte[]} for bundle, with MAC check.
     * @param messageType
     * @param bundle
     * @param socket
     * @throws Exception
     */
    public Message (MessageType messageType, byte[] bundle, Socket socket ) throws Exception {

        this.messageType = messageType;

        ByteBuffer bb = ByteBuffer.allocate(bundle.length);
        bb.put(bundle);
        bb.flip();

        messageId = bb.getLong();

        byte[] originInBytes = new byte[bb.getInt()];
        bb.get(originInBytes);

        byte[] destinationInBytes = new byte[bb.getInt()];
        bb.get(destinationInBytes);

        body = new byte[bb.getInt()];
        bb.get(body);


        byte[] srcMac = new byte[bb.getInt()];
        bb.get(srcMac);
        byte[] calculatedMac = socket.generateMac(new byte[][]{new byte[]{messageType.byteValue}, originInBytes, destinationInBytes, body});
        if (Arrays.compare(srcMac, calculatedMac) != 0)
            throw new ClientException (ClientException.Type.MAC_NOT_EQUAL);

        this.origin = new String (originInBytes, StandardCharsets.UTF_8);
        this.destination = new String (destinationInBytes, StandardCharsets.UTF_8);
    }
	
	
	
    /**
     * Use this to create a new message.
     * @param messageType
     * @param origin
     * @param destination
     * @param messageId
     * @param body
     */
    public Message(MessageType messageType, String origin, String destination, long messageId, byte[] body){
        this.messageType = messageType;
        this.origin = origin;
        this.destination = destination;
        this.messageId = messageId;
        this.body = body;
    }
    
        

    /**
     * Gets a ByteBuffer ready to be sent.
     * Generates and includes MAC and encrypts with lambda parameter encryptMe.
     * @param socket
     * @param encryptMe
     * @return
     */
    public ByteBuffer getMessageInByteBuffer(Socket socket, Function<byte[], byte[]> encryptMe) {
    	
    	byte[] bundle = generateBundleWithMac(socket);

        bundle = encryptMe.apply(bundle);


        ByteBuffer bb = ByteBuffer.allocate (5+bundle.length);
        bb.put(messageType.byteValue);
        bb.putInt(bundle.length);
        bb.put(bundle);

        return bb;
    }
    
    
    private byte[] generateBundleWithMac(Socket socket) {
    	byte[] originInBytes = origin.getBytes(StandardCharsets.UTF_8);

        byte[] destinationInBytes = destination.getBytes(StandardCharsets.UTF_8);

        byte[] mac = socket.generateMac(new byte[][]{new byte[]{messageType.byteValue}, originInBytes, destinationInBytes, body});


        ByteBuffer bb = ByteBuffer.allocate( 8 + 4 + originInBytes.length +
                4 + destinationInBytes.length + 4 + body.length + 4 + (mac==null? 0 : mac.length));

        bb.putLong(messageId);
        
        bb.putInt(originInBytes.length);
        bb.put(originInBytes);
        
        bb.putInt(destinationInBytes.length);
        bb.put(destinationInBytes);
        
        bb.putInt(body.length);
        bb.put(body);
        
        bb.putInt(mac.length);
        bb.put(mac);

        
        return bb.array();
    }
}
