package clientForNonBlockingServer.client;


import java.nio.ByteBuffer;


public class MessageReader {

	Socket socket = null;
	
	/**Five bytes. First byte messageType, next four bytes length*/
	private ByteBuffer byteBufferTypeAndLength = ByteBuffer.allocate(5);

	/**For the message bundle reception*/
	private ByteBuffer byteBufferBundle = null;

	/**MessageType that has to bee read*/
	private MessageType messageType = null;

	/**The length of the message*/
	private int length = 0;
	
	/**Content type. From MessageType values > 1*/
	private byte[] bundle = null;
	
	
	
	public MessageReader(Socket socket) {
		this.socket = socket;
	}
	
	public void readMessage() throws ClientException {
		//The first five bytes haven't been read.
		if (messageType == null) {
			readTypeAndLength();
		}

		if (messageType != null && bundle == null) {
			if (byteBufferBundle == null) {
				byteBufferBundle = ByteBuffer.allocate(length);
			}

			readBundle();
		}

		if (bundle != null) {
			rebuildAndProcess();
		}
	}
	
	
	private void readTypeAndLength() throws ClientException {

		try {
			socket.socketChannel.read(this.byteBufferTypeAndLength); //throws exception with connection problems.
		} catch (Exception e) {
			throw new ClientException(ClientException.Type.CONNECTION_PROBLEMS);
		}
		
		//the first five bytes have been read.
		if (byteBufferTypeAndLength.position() >= byteBufferTypeAndLength.limit()) {

			byteBufferTypeAndLength.flip(); //ready to read

			byte b =  byteBufferTypeAndLength.get();
			
			messageType = MessageType.getTypeByByte(b);

			length = byteBufferTypeAndLength.getInt();

			byteBufferTypeAndLength.clear(); //clean and ready 

			if (messageType == null) throw new ClientException(ClientException.Type.UNKNOWN_MESSAGE_TYPE);

		}
	}
	
	
	private void readBundle() throws ClientException  {

		try {
			socket.socketChannel.read(this.byteBufferBundle);
		} catch (Exception e) {
			throw new ClientException(ClientException.Type.CONNECTION_PROBLEMS);
		}
		//destination is fully read
		if (byteBufferBundle.position() >= byteBufferBundle.limit()) {

			byteBufferBundle.flip(); //ready to read
			bundle = byteBufferBundle.array();

			length = 0;
			byteBufferBundle = null;
		}
	}
	
	
	private void rebuildAndProcess () throws ClientException {
		Message message = null;
		
		
		if (messageType != MessageType.WELCOME) {
			bundle = socket.decryptWithSecretKey(bundle);
		}
		
		message = new Message(messageType, bundle);

		
		messageType = null;
		length = 0;
		bundle = null;
		
		MessageProcessor.processIncomingMessage(message, socket);
		
		
		
	}
}
