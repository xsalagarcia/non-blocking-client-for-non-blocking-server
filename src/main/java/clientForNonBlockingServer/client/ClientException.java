package clientForNonBlockingServer.client;

public class ClientException extends Exception {
    
	Type type;

    public ClientException (Type type) {
        super (type.message);
        this.type = type;
    }
    
    
    public enum Type {
        MAC_NOT_EQUAL ("[ERROR] MAC doesn't match."),
        PROBLEM_DESSERIALIZING_ALL_USERS("[ERROR] Problem desserializing users."),
        UNKNOWN_MESSAGE_TYPE ("[ERROR] Message type not recognized."),
        CONNECTION_PROBLEMS ("[ERROR] Connection problems."),
        PROBLEM_WITH_SECRET_KEY("[ERROR] Problem with secretKey."),
        PROBLEM_DECRYPTING ("[ERROR] Problem decrypting."), 
        PROBLEM_ENCRYPTING ("[ERROR] Problem encrypting.");

        public final String message;

        Type(String message) {
            this.message = message;
        }

    }
    
}
