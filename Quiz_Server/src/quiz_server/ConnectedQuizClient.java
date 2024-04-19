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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ArrayList<Question> questions;
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
            this.state = "CONNECT";
            this.possibleUsers = new ArrayList<>();
            this.questions = new ArrayList<>();
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
    
    public void readSet(String active_set) throws FileNotFoundException, IOException
    {
        String filename = null;
        switch(active_set)
        {
            case "Set 1" : 
                filename = "./set1.txt";
                break;
            
            case "Set 2" : 
                filename = "./set2.txt";
                break;
            
            case "Set 3" :
                filename = "./set3.txt";
                break;
                
            case "Set 4" : 
                filename = "./set4.txt";
                break;
        }
        
        File fp = new File(filename);
        if(fp.exists())
        {
            String regexPatternQuestion = "\\d+[.] [a-zA-zćčšđž :]+";
            Pattern patternQ = Pattern.compile(regexPatternQuestion);
            String regexPatternAnswerWrong = "\\t[a-c]";
            Pattern patternAW = Pattern.compile(regexPatternAnswerWrong);
            String regexPatternAnswerRight = "\\t[d]";
            Pattern patternAR = Pattern.compile(regexPatternAnswerRight);
            
            HashMap<Boolean,String> answers = new HashMap<>();
            HashMap<Boolean,String> temp_answers;
            BufferedReader in = new BufferedReader(new FileReader(fp));
            String currentQuestionText = null;
            String line;
            while((line = in.readLine()) != null)
            {   
                Matcher matcherQ = patternQ.matcher(line);
                Matcher matcherAW = patternAW.matcher(line);
                Matcher matcherAR = patternAR.matcher(line);
                if(matcherQ.find()) // Found a question
                {
                    //answers.clear();
                    System.out.println("Found a question");
                    currentQuestionText = line;
                }
                else if(matcherAW.find())   // Found a wrong answer ; a,b,c
                {
                    System.out.println("Found a wrong answer");
                    answers.put(false,line);
                }
                else if(matcherAR.find())   // Found a right answer ; d
                {
                    System.out.println("Found a right answer");
                    answers.put(true,line);
                    temp_answers = new HashMap(answers);
                    Question currentQuestion = new Question(currentQuestionText,temp_answers);
                    this.questions.add(currentQuestion);
                    answers.clear();
                }
            }     
        }
        else 
        {
            System.out.println("File not found");
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
                    case "CONNECT" :
                        String connect_flag = br.readLine();
                        if(connect_flag.startsWith("Connect"))
                        {
                            this.pw.println("Success");
                            this.state = "LOGIN";
                        }
                        break;
                        
                        
                    case "LOGIN" :
                        try {
                            String login_info = br.readLine();
                            System.out.println(login_info);
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
                                    this.state = "CHECK_OK";
                                    //allClients.add(this);       // Here I add people in allCLintes, why does it double them?
                                    break;
                                }
                            }
                            if(match == 0)
                            {
                                this.pw.println("Failed login");
                                System.out.println("Failed login");
                                this.state = "LOGIN";
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        
                    case "CHECK_OK" :
                        String enter_update = br.readLine();
                        int num_users = this.allClients.size();
                        String all_users = "Users:"+num_users+":";
                        
                        if(enter_update.equals("Enter_update"))
                        {
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                all_users+=clnt.username+",";
                            }
                            
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                clnt.pw.println(all_users);
                            }
                            this.state = "START_QUIZ";
                        }
                        break;
                        
                    case "START_QUIZ" :
                        String start_flag = br.readLine(); 
                        System.out.println(start_flag);
                        if(start_flag.startsWith("Start:"))
                        {
                            String [] active_set_fetch = start_flag.split(":");
                            String active_set = active_set_fetch[1];
                            System.out.println(active_set);
                            readSet(active_set);
                            System.out.println(questions);
                        }
                        break;
                                 
                }
                
            } catch (IOException ex) {
                Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
}
