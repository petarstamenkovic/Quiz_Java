package quiz_server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private int question_number;
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
            
            String regexPatternAnswerA = "\\t[a]";
            Pattern patternA = Pattern.compile(regexPatternAnswerA);
            
            String regexPatternAnswerB = "\\t[b]";
            Pattern patternB = Pattern.compile(regexPatternAnswerB);
            
            String regexPatternAnswerC = "\\t[c]";
            Pattern patternC = Pattern.compile(regexPatternAnswerC);
            
            String regexPatternAnswerD = "\\t[d]";
            Pattern patternD = Pattern.compile(regexPatternAnswerD);
            
            String answerAText = "";
            String answerBText = "";
            String answerCText = "";
            String answerDText;
            String currentQuestionText = null;
            BufferedReader in = new BufferedReader(new FileReader(fp));
            
            String line;
           
            while((line = in.readLine()) != null)
            {   
                
                Matcher matcherQ = patternQ.matcher(line);
                Matcher matcherA = patternA.matcher(line);
                Matcher matcherB = patternB.matcher(line);
                Matcher matcherC = patternC.matcher(line);
                Matcher matcherD = patternD.matcher(line);

                
                if(matcherQ.find()) // Found a question
                {
                    System.out.println("Found a question");
                    currentQuestionText = line;
                }
                
                if(matcherA.find()) 
                {
                    System.out.println("Found a wrong answer a)");
                    answerAText = line;
                    //Answer answerA = new Answer(line,false);   
                }
                if(matcherB.find()) 
                {   
                    System.out.println("Found a wrong answer b)");
                    answerBText = line;
                    //Answer answerB = new Answer(line,false);
                }
                if(matcherC.find())
                {
                    System.out.println("Found a wrong answer c)");
                    answerCText = line;
                    //Answer answerC = new Answer(line,false);
                }
                if(matcherD.find())
                {
                    System.out.println("Found a right answer d)");
                    Answer answerA = new Answer(answerAText,false);
                    Answer answerB = new Answer(answerBText,false);
                    Answer answerC = new Answer(answerCText,false);
                    Answer answerD = new Answer(line,true);
                    Question currentQuestion = new Question(currentQuestionText,answerA,answerB,answerC,answerD);
                    this.questions.add(currentQuestion);
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
                            //String login_regex = "^(?=[a-zA-Z])(?!.*[^a-zA-Z0-9])(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%^&*()-_=+\\\\|\\[{\\]};:'\",<.>/?]).{6,}:(admin|contestant)$";
                            //Pattern patternLogin = Pattern.compile(login_regex);
                            //Matcher matcherLogin = patternLogin.matcher(login_info);
                            //if(matcherLogin.find())
                            //{
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
                                        break;
                                    }
                                }
                                if(match == 0)
                                {
                                    System.out.println("No match found in db, login failed");
                                    this.pw.println("Failed login");
                                    this.state = "LOGIN";
                                }
                            //}
                            //else
                            //{
                            //    System.out.println("Wrong login format!");
                            //}
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
                        
                    // State that loads the question set and prepares for a start of a quiz    
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
                            this.state = "IN_GAME";
                        }
                        if(start_flag.startsWith("AddPlayer"))
                        {
                            //// Check the validity of a input format - use regex /////
                            String [] new_player_token = start_flag.split(",");
                            String new_player_info = new_player_token[1];
                            File fp = new File("./users.txt");
                            if(fp.exists())
                            {
                                BufferedReader in = new BufferedReader(new FileReader(fp));
                                String line;
                                boolean userExists = false;
                                while((line = in.readLine()) != null)
                                {   
                                    if(line.equals(new_player_info))
                                    {
                                        userExists = true;
                                        break;
                                    }
                                }
                                // User doesnt already exist
                                if(userExists == false)
                                {
                                    FileWriter fw = new FileWriter(fp,true);
                                    fw.write("\n"+new_player_info);
                                    fw.flush();
                                }
                            }
                            else
                            {
                                System.out.println("File not found");
                            }     
                        }
                        // Remove a player
                        /*
                        if(start_flag.startsWith("RemovePlayer"))
                        {
                            //// Check the validity of a input format - use regex /////
                            String [] new_player_token = start_flag.split(",");
                            String new_player_info = new_player_token[1];
                            File fp = new File("./users.txt");
                            if(fp.exists())
                            {
                                BufferedReader in = new BufferedReader(new FileReader(fp));
                                String line;                       
                                while((line = in.readLine()) != null)
                                {   
                                    if(line.equals(new_player_info))
                                    {
                                      
                                    }
                                }

                            }
                            else
                            {
                                System.out.println("File not found");
                            }     
                        }
                        */
                        
                        break;
                                 
                    
                    case "IN_GAME" : 
                        String new_question = br.readLine();
                        if(new_question.startsWith("NewQuestion"))
                        {
                            String current_question = questions.get(question_number).getText();
                            ArrayList<Answer> answersToShuffle = new ArrayList<>();
                            Answer tempA = questions.get(question_number).getAnswerA();
                            answersToShuffle.add(tempA);
                            Answer tempB = questions.get(question_number).getAnswerB();
                            answersToShuffle.add(tempB);
                            Answer tempC = questions.get(question_number).getAnswerC();
                            answersToShuffle.add(tempC);
                            Answer tempD = questions.get(question_number).getAnswerD();
                            answersToShuffle.add(tempD);
                            
                            Collections.shuffle(answersToShuffle);  // This is an ArrayList of shuffled answers
                            
                            String answerA = answersToShuffle.get(0).getAnswerText();
                            String answerB = answersToShuffle.get(1).getAnswerText();
                            String answerC = answersToShuffle.get(2).getAnswerText();
                            String answerD = answersToShuffle.get(3).getAnswerText();
                            
                            this.pw.println("NewQuestion: " + current_question + answerA + ":" + answerB + ":" + answerC + ":" + answerD);
                            question_number++;
                            
                            // At the end of a set, go back to a state that reads a new set of questions
                            if(question_number == 10)
                            {
                                this.state = "START_QUIZ";
                            }
                        }
                        
                        
                        
                }
                
            } catch (IOException ex) {
                Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
}
