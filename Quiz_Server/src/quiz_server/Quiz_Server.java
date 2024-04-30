package quiz_server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Quiz_Server {

    private ServerSocket ssocket;
    private int port;
    private ArrayList<ConnectedQuizClient> clients;
    private static final String AES = "AES";
    private SecretKey symmetricKey;
    private byte[] initializationVector;
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";
    
    public ServerSocket getSsocket() {
        return ssocket;
    }

    public void setSsocket(ServerSocket ssocket) {
        this.ssocket = ssocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    // Method that awaits new clients, creates a new thread for them and adds them to a list.
    public void acceptClients() throws IOException, Exception
    {
        Socket client = null;
        Thread thr;
        while(true)
        {
            System.out.println("Waiting for new clients...");
            client = this.ssocket.accept();
            
            System.out.println("Key is : " + symmetricKey);
            byte[] initializationVector = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
            
            if(client != null)
            {
                ConnectedQuizClient clnt = new ConnectedQuizClient(client,clients,symmetricKey,initializationVector);
                clients.add(clnt);
                thr = new Thread(clnt);
                thr.start();
            }
            else
            {
                break;
            }
        }
    }

    // Method that creates a same key every time we start the server - fixed seed
    public static SecretKey createAESKey() throws Exception {
        SecureRandom securerandom = new SecureRandom();
        String seed = "RSZEOS2024";
        securerandom.setSeed(seed.getBytes());
        KeyGenerator keygenerator = KeyGenerator.getInstance(AES);
        keygenerator.init(128, securerandom);
        SecretKey key = keygenerator.generateKey(); 
        return key;
    }
    
    // This function is not neccesary since we use a fixed initialization vector
    public static byte[] createInitializationVector()
    {
        byte[] initializationVector = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);
        return initializationVector;
    }

    // Method that does the encryption
    public static byte[] do_AESEncryption(String plainText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        return cipher.doFinal(plainText.getBytes());
    }

    // Method that does the decryption
    public static String do_AESDecryption(byte[] cipherText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM); 
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] result = cipher.doFinal(cipherText);
        return new String(result);
    }
    
    // Constructor
    public Quiz_Server(int port) throws IOException, Exception
    {
        this.clients = new ArrayList<>();
        this.port = port;
        this.ssocket = new ServerSocket(port);  
        this.symmetricKey = createAESKey();
    }
 
    // Method that loads the initital admin - encryption required
    public void LoadInitialAdmin() throws FileNotFoundException, IOException, Exception
    {
        byte[] init_vec = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        String startAdmin = "Petar:123ABC@d:admin";
        byte[] encAdmin = do_AESEncryption(startAdmin,this.symmetricKey,init_vec);
        FileOutputStream fos = new FileOutputStream("./users.txt");
        fos.write(encAdmin);
        fos.flush();
    }
    
    // Main starts the server
    public static void main(String[] args) throws IOException, Exception 
    {
        Quiz_Server server = new Quiz_Server(6001);
        System.out.println("Server online, listening on port 6001...");
        server.LoadInitialAdmin();
        server.acceptClients();
    }
    
}
