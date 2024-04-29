package quiz_server;

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
  
    // Block cipher(CBC mode) - tzv Blok sifra kod koje se originalna poruka sifruje po grupama (blokovima)
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
            
            SecretKey symmetricKey = createAESKey();
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


    // Funkcija koja kreira skriveni kljuc
    public static SecretKey createAESKey() throws Exception {
        SecureRandom securerandom = new SecureRandom();
        String seed = "RSZEOS2024";
        securerandom.setSeed(seed.getBytes());
        //prilikom pravljenja kljuca navodi se koji se algoritam koristi
        KeyGenerator keygenerator = KeyGenerator.getInstance(AES);
  
        //duzina kljuca se navodi prilikom pozivanja init funkcije
        //ovde koristimo duzinu 128 bita (za 256 bita je potrebno instalirati 
        //dodatne pakete)
        keygenerator.init(128, securerandom);
        
        SecretKey key = keygenerator.generateKey();
  
        return key;
    }
    
    public static byte[] createInitializationVector()
    {

        //velicina inicijalizacionog vektora
        byte[] initializationVector = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);
        return initializationVector;
    }
    
        //Funkcija koja prima otvoreni tekst, kljuc i inicijalizacioni vektor i 
    //generise sifrat (cipher text)
    public static byte[] do_AESEncryption(String plainText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        //klasa Cipher se koristi za enkripciju/dekripciju, prilikom kreiranja navodi se koji algoritam se koristi
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        
        //IvParameterSpec se kreira koristeci inicijalizacioni vektor a potreban je za inicijalizaciju cipher objekta
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
  
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
  
        //metoda doFinal nakon sto se inicijalizuje metodom init, vrsi enkripciju otvorenog teksta
        return cipher.doFinal(plainText.getBytes());
    }

    //Funkcija koja prima sifrat (kriptovan tekst), kljuc i inicijalizacioni vektor i vraca dekriptovani tekst
    //generise sifrat (cipher text)
    public static String do_AESDecryption(byte[] cipherText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
  
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
  
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
  
        //ista metoda doFinal se koriti i za dekripciju
        byte[] result = cipher.doFinal(cipherText);
  
        return new String(result);
    }
    
    // Constructor for a server - just supply a port to listen to
    public Quiz_Server(int port) throws IOException
    {
        this.clients = new ArrayList<>();
        this.port = port;
        this.ssocket = new ServerSocket(port);    
    }
 
    // Main starts the server
    public static void main(String[] args) throws IOException, Exception 
    {
        Quiz_Server server = new Quiz_Server(6001);
        
        System.out.println("Server online, listening on port 6001...");
        server.acceptClients();
    }
    
}
