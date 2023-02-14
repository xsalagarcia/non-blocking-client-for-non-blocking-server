package clientForNonBlockingServer.client;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.IconifyAction;

import javafx.application.Platform;


/**
 * Only static methods. Given a message, processes it.
 * @author xsala
 *
 */
public abstract class MessageProcessor {
	
	
	/**
	 * Entry function to process a message.
	 * @param message
	 * @param socket
	 * @throws ClientException
	 */
	public static void processIncomingMessage(Message message, Socket socket) throws ClientException {
		
		switch(message.messageType) {
		case WELCOME:
			try {
				processWelcome(message, socket);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case CHECK_WELCOME:
			processCheckWelcome(message, socket);
			break;
		case ACK:
			processACKMessage(message, socket);
			break;
		case NACK:
			processNACKMessage(message, socket);
		case ALL_USERS:
			MessageProcessor.processAllUsers(message, socket);
			break;
		case ENCRYPTED_TEXT:
			MessageProcessor.processTextMessage(message, socket);
		default:
		}
	}
	
	
	/**
	 * Creates PrivateKey, randomInt for MAC generation.
	 * Creates a response with PrivateKey, randomInt and SHARED_SECRET_KEY_COMPLEMENT.
	 * Sets socket.macForWelcome. Later with check welcome will be necessary.
	 * @param message
	 * @param socket
	 * @throws Exception if couldn't process welcome message.
	 */
	private static void processWelcome(Message message, Socket socket) throws ClientException  {
		
		long serverSocketId = MessageUtilities.byteArrayToLong(Arrays.copyOfRange(message.body, 0, 8));
        
		
		byte[] encodedPublicKey =  Arrays.copyOfRange(message.body, 8, message.body.length);
		
		PublicKey pk = getPulbicKeyFromBytes (encodedPublicKey);
		       
        byte[] encodedSecretKey = socket.setSecretKeyCiphers();
        
        int randomInt = new Random().nextInt();
        
        byte[] keyForMac = MessageUtilities.byteArraysToByteArray(new byte[][] {
        	MessageUtilities.longToByteArray(serverSocketId),
        	MessageUtilities.intToByteArray(MessageUtilities.SHARED_SECRET_KEY_COMPLEMENT),
        	MessageUtilities.intToByteArray(randomInt)
        });
        
        socket.setMacGenerator(keyForMac);
        
        byte[] bodyMessage = MessageUtilities.byteArraysToByteArray(new byte[][] {
        	MessageUtilities.intToByteArray(randomInt),
        	encodedSecretKey
        });
        
        Message responseMessage = new Message(MessageType.SECRET, "noName",
        		message.origin, MessageUtilities.newMessageId(), bodyMessage);
               
        socket.messageWriter.enqueueMessage(
        		responseMessage.getMessageInByteBuffer(socket, bytes -> {
        			try {
	        			Cipher cipher = Cipher.getInstance(MessageUtilities.DEFAULT_PAIR_KEY_ALGORITHM);
	        	        cipher.init(Cipher.ENCRYPT_MODE, pk);
	        	        return cipher.doFinal(bytes);
        	        } catch (Exception e){
        	        	e.printStackTrace();
        	        }
        	        return null;
        		}));
        
        
        
        socket.macForWelcome = socket.generateMac(new byte[][] {
        		encodedPublicKey,
        		encodedSecretKey,
        		keyForMac});
        
	}
	
	
	private static void processCheckWelcome(Message message, Socket socket) {
		if (Arrays.compare(message.body, socket.macForWelcome) != 0) {
			socket.messageToUI("Checking welcome failed");
			socket.stopMe();
		} else {
			socket.messageToUI("Waiting for logging...");
		}
	}
	
	private static void processACKMessage (Message message, Socket socket) {
		Long messageACKId = MessageUtilities.byteArrayToLong(message.body);
		Message acknowledgedMessage = socket.messagesPendingToCheck.get(messageACKId);
		if (messageACKId == socket.logInMessageId) {
			socket.messageToUI("Log in OK!");
			socket.userName = message.destination;
		} else if (acknowledgedMessage != null) {
			socket.messageToUI("Message to " + acknowledgedMessage.destination + " accepted.");
			socket.messagesPendingToCheck.remove(messageACKId);
		}
	}
	
	private static void processNACKMessage (Message message, Socket socket ) {
		Long messageNACKId = MessageUtilities.byteArrayToLong(Arrays.copyOfRange(message.body, 0, 8));
		socket.messageToUI(new String(Arrays.copyOfRange(message.body, 8, message.body.length),
			StandardCharsets.UTF_8));
		socket.messagesPendingToCheck.remove(messageNACKId);
	}
	
	private static void processAllUsers (Message message, Socket socket) throws ClientException  {

		try {

			ByteArrayInputStream bais = new ByteArrayInputStream(message.body);
			ObjectInputStream ois = new ObjectInputStream(bais);
			int size = ois.readInt();
			String[] allUsers = new String[size];
			for (int i = 0; i<size; i++)
				allUsers[i] = (String) ois.readObject();

			socket.allUsersToUI(allUsers);
			//Platform.runLater(() ->socket.uiController.updateAllUsers(allUsers));
		} catch (Exception e) {
			throw new ClientException(ClientException.Type.PROBLEM_DESSERIALIZING_ALL_USERS);
		}
	
	}
	
	private static void processTextMessage (Message message, Socket socket) throws ClientException {
		
		
		String toUI = String.format("[%s]: %s",
				message.origin, new String(message.body, StandardCharsets.UTF_8));
		
		socket.messageToUI(toUI);
	}
	
	private static PublicKey getPulbicKeyFromBytes (byte[] encodedPublicKey) throws ClientException {

		try {
			return KeyFactory.getInstance(MessageUtilities.DEFAULT_PAIR_KEY_ALGORITHM).
				generatePublic(new X509EncodedKeySpec(encodedPublicKey));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new ClientException (ClientException.Type.PROBLEM_DECRYPTING);
		}
	}


}
