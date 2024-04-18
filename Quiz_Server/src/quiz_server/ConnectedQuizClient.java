package quiz_server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectedQuizClient implements Runnable{
    
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private ArrayList<ConnectedQuizClient> allClients;
    private String state;
    private String username;
    private String password;
    private String role;
    
    private ArrayList<User> possibleUsers; 
    // Constructor 
    public ConnectedQuizClient(Socket socket,ArrayList<ConnectedQuizClient> allClients)
    {
        try {
            this.socket = socket;
            this.allClients = allClients;           
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
            this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()),true);
            this.username = "";
            this.password = "";
            this.role = "";
            this.state = "IDLE";
            this.possibleUsers = new ArrayList<>();
        } catch (IOException ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void readUsers() throws FileNotFoundException, IOException
    {
        File fp = new File("./users.txt");
        if(fp.exists())
        {
            BufferedReader in = new BufferedReader(new FileReader(fp));
            String line;
            
            while((line = in.readLine()) != null)
            {
                String [] token = line.split(":");
                User user = new User(token[0],token[1],token[2]);
                possibleUsers.add(user);
            }
        }
        else 
        {
            System.out.println("File not found!");
        }
    }
    
    // Read from user files here
    @Override
    public void run()
    {
        try {
            readUsers();
        } catch (IOException ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true)
        {
            try {
 
                switch(this.state)
                {
                    case "IDLE" :
                        try {
                            String login_info = br.readLine();
                            String [] login_token = login_info.split(":");
                            this.username = login_token[0];
                            this.password = login_token[1];
                            this.role = login_token[2];
                            int match = 0;
                            for(User u : this.possibleUsers)
                            {
                                if(u.getUsername().equals(this.username) && u.getPassword().equals(this.password) && u.getRole().equals(this.role))
                                {
                                    match = 1;
                                    System.out.println("You found the match");
                                    String message = "Login ok:" + this.username + ":" + this.role;
                                    this.pw.println(message);
                                    this.state = "LOGGED_IN";
                                    allClients.add(this);       // Here I add people in allCLintes, why does it double them?
                                    break;
                                }
                            }
                            if(match == 0)
                            {
                                this.pw.println("Login failed");
                                this.state = "IDLE";
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        
                    case "LOGGED_IN" :
                        String start_indicator = br.readLine();
                        int num_users = this.allClients.size();
                        String all_users = "Users:"+num_users+":";
                        
                        if(start_indicator.equals("Update"))
                        {
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                all_users+=clnt.username+",";
                            }
                            
                            this.pw.println(all_users);
                            //this.state = "START_QUIZ";
                        }
                        
                        
                        break;
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                        
                }
                
            } catch (IOException ex) {
                Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
}
