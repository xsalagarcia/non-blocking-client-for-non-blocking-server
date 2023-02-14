package clientForNonBlockingServer.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.function.Consumer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import clientForNonBlockingServer.MainScreenController;


/**
 * Represents the client with his socket.
 * Interacts with user interface with messageToUI(), tryLogIn(), newUser(), sendMessageToUser(), getAllUsers().
 * Has a messageReader and messageWriter for sending and receiving messages.
 * When SocketChannel is connected, it's turned into non-blocking mode.
 * Runnable class that runs a loop trying to send the content of outputQueue through a {@code SocketChannel} 
 * in non-blocking mode and tries to read incoming data from {@code SocketChannel} putting it to inputQueue.
 * @author xsala
 *
 */
public class Socket implements Runnable {

	
	public SocketChannel socketChannel = null;
	int port = 0;
	
	MessageReader messageReader = new MessageReader (this);
	MessageWriter messageWriter = new MessageWriter (this);
	
	public Cipher secretKeyCipherDecryptor = null;
	public Cipher secretKeyCipherEncryptor = null;
	
	private Mac mac = null;

	public byte[] macForWelcome = null;
	
	public volatile boolean hasToStop = false;
	
	public String userName = null;
	
	public MainScreenController uiController = null;
	
	private Consumer<String> messageToUIFunction = null;
	
	private Consumer<String[]> allUsersToUIFunction = null;
	
	HashMap<Long, Message> messagesPendingToCheck = new HashMap<Long, Message>();
	
	long logInMessageId;
	
	

	
	
	
	/**
	 * 	Constructor, sets the port and the login message.
	 * @param port
	 * @param userName
	 */
	public Socket (int port, Consumer<String> messageToUIFunction, Consumer<String[]> allUsersToUIFunction) {
		
		this.port = port;
		this.messageToUIFunction = messageToUIFunction;
		this.allUsersToUIFunction = allUsersToUIFunction;
		

		
	}
	@Override
	public void run() {

		try {
			openSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
			
		while (socketChannel.isConnected() && !hasToStop) {
			executeCycle();
			
			try {
				Thread.sleep(100); //GIVE ME SOME AIR.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		messageToUI("Connection is over");
		
		
		
	}
	

	private void executeCycle() {
		
		try {
		
		messageReader.readMessage();
		messageWriter.write();
		
		} catch (ClientException e) {
			e.printStackTrace();
			stopMe();
		}
		
	}
	
	public void messageToUI (String message) {
		this.messageToUIFunction.accept(message);
		//Platform.runLater(()-> {uiController.sendMessage(message);});
	}
	
	public void allUsersToUI (String[] allUsers) {
		this.allUsersToUIFunction.accept(allUsers);
	}
	
	
	
	private void openSocket() throws IOException {
		socketChannel = SocketChannel.open();
		socketChannel.connect(new InetSocketAddress("localHost", port));
		messageToUI("socket connected.");
		socketChannel.configureBlocking(false);
		
	}
	


	
	public void stopMe() {
			this.messageToUI("Disconnected.");
		try {

			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		hasToStop = true;
		

	}
	
	/**
	 * Creates a SecretKey, sets Ciphers and returns encoded SecretKey.
	 * @return encoded generated SecretKey.
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 */
	public byte[] setSecretKeyCiphers () throws ClientException {
        SecretKey sk = null;
		try {
			KeyGenerator kg = KeyGenerator.getInstance(MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
	        kg.init(MessageUtilities.PRIVATE_KEY_SIZE, new SecureRandom());
	        sk = kg.generateKey();
	        
	        secretKeyCipherDecryptor = Cipher.getInstance(MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
	        secretKeyCipherDecryptor.init(Cipher.DECRYPT_MODE, sk);
	        
	        secretKeyCipherEncryptor = Cipher.getInstance(MessageUtilities.DEFAULT_SECRET_KEY_ALGORITHM);
	        secretKeyCipherEncryptor.init(Cipher.ENCRYPT_MODE, sk);
        } catch (Exception e) {
        	throw new ClientException (ClientException.Type.PROBLEM_WITH_SECRET_KEY);
        }
        
        return sk.getEncoded();
	}
	
	/**
	 * Creates and initiates mac object from a given key.
	 * @param keyForMac
	 */
	public void setMacGenerator(byte[] keyForMac) {
		try {
			mac = Mac.getInstance("HmacSHA256");
			//mac.init(new SecretKeySpec(keyForMac, "RawBytes"));
			mac.init(new SecretKeySpec(keyForMac, "HmacSHA256"));
		} catch (NoSuchAlgorithmException | InvalidKeyException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates MAC or byte[0]=0 if mac object is null.
	 * @param datas
	 * @return
	 */
	public byte[] generateMac(byte[][] datas){
		if (mac == null) return new byte[]{0}; //no MAC generator.

		for (byte[] data : datas){
			mac.update(data);
		}
		return mac.doFinal();
	}
	
	/**
	 * Decrypt function with secret key.
	 * @param bytes
	 * @return
	 * @throws ClientException
	 */
	public byte[] decryptWithSecretKey(byte[] bytes) throws ClientException {
		try {
			return secretKeyCipherDecryptor.doFinal(bytes);
		} catch (Exception e) {
			throw new ClientException (ClientException.Type.PROBLEM_DECRYPTING);
		}
	}

	/**
	 * Encrypt function with secret key. If exception occurs encrypting, will return null.
	 * @param bytes
	 * @return
	 */
	public byte[] encryptWithSecretKey(byte[] bytes)  {
		try {
			return secretKeyCipherEncryptor.doFinal(bytes);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;

	}
	
	public void tryLogIn(String userName, byte[] password)  {
		
		sendUserAndPasswordMessage(userName, password, MessageType.LOG_IN);

	}
	
	
	public void newUser (String userName, byte[] password) {
		
		sendUserAndPasswordMessage(userName, password, MessageType.NEW_USER);
		
	}
	
	public void sendMessageToUser (String destination, String text) {
		if (userName == null) {
			this.messageToUI("It's necessary to be logged");
			return;
		}
		
		Message message = new Message (MessageType.ENCRYPTED_TEXT,
				userName, destination, MessageUtilities.newMessageId(), text.getBytes(StandardCharsets.UTF_8));
		
		messageWriter.enqueueMessage(message.getMessageInByteBuffer(this, b -> encryptWithSecretKey(b)));
		
	}
	
	private void sendUserAndPasswordMessage(String userName, byte[] password, MessageType messageType) {
		byte[] userNameInBytes = userName.getBytes(StandardCharsets.UTF_8);
		
		byte[] body = MessageUtilities.byteArraysToByteArray(new byte[][] {
			MessageUtilities.intToByteArray(userName.length()),
			userNameInBytes,
			password
		});
				
		this.logInMessageId = MessageUtilities.newMessageId();
		
		Message logInMessage = new Message(messageType, MessageUtilities.CLIENT_UNNAMED,
				MessageUtilities.SERVER_NAME, logInMessageId, body);
		
		messageWriter.enqueueMessage(logInMessage.getMessageInByteBuffer(this, b->{return encryptWithSecretKey(b);} ));
		messageToUI("Waiting for server response");
	}
	
	
	public void getAllUsers () {
		
		if (userName == null ) {
			messageToUI("Try to log in first.");
			return;
		}
		Message message = new Message(MessageType.ALL_USERS, userName, MessageUtilities.SERVER_NAME, MessageUtilities.newMessageId(), new byte[] {0});
		messageWriter.enqueueMessage(message.getMessageInByteBuffer(this,  b -> encryptWithSecretKey(b)));
	}
	
	public void sendTextMessage(String destination, String text) {
		if (userName == null) {
			this.messageToUI("You have to be logged.");
			return;
		} else if (destination == null ){
			this.messageToUI("Try to update and select an user.");
			return;
		}
		
		Message message = new Message (MessageType.ENCRYPTED_TEXT, userName, destination,
				MessageUtilities.newMessageId(), text.getBytes(StandardCharsets.UTF_8));
		
		messageWriter.enqueueMessage(message.getMessageInByteBuffer(this, b -> encryptWithSecretKey(b)));
		
		messagesPendingToCheck.put(message.messageId, message);
		
	}
	

}
