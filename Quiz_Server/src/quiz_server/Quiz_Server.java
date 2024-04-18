package quiz_server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Quiz_Server {

    private ServerSocket ssocket;
    private int port;
    private ArrayList<ConnectedQuizClient> clients;

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
    public void acceptClients() throws IOException
    {
        Socket client = null;
        Thread thr;
        while(true)
        {
            System.out.println("Waiting for new clients...");
            client = this.ssocket.accept();
            
            if(client != null)
            {
                ConnectedQuizClient clnt = new ConnectedQuizClient(client,clients);
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
    
    // Constructor for a server - just supply a port to listen to
    public Quiz_Server(int port) throws IOException
    {
        this.clients = new ArrayList<>();
        this.port = port;
        this.ssocket = new ServerSocket(port);    
    }
 
    // Main starts the server
    public static void main(String[] args) throws IOException 
    {
        Quiz_Server server = new Quiz_Server(6001);
        
        System.out.println("Server online, listening on port 6001...");
        server.acceptClients();
    }
    
}
