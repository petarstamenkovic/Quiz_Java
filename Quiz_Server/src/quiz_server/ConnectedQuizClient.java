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

public class ConnectedQuizClient implements Runnable , Comparable<ConnectedQuizClient>{
    
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
    private int questions_answered;
    private int right_answeres;
    private String previousSet;
    private String activeSet;
    private boolean endOfSet;
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
            this.questions_answered = 0;
            this.right_answeres = 0;
            this.previousSet = "";
            this.activeSet = "";
            this.endOfSet = false;
        } catch (IOException ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override 
    public String toString()
    {
        return "Name: " + this.username + ": " + this.right_answeres + " / " + this.questions_answered; 
    }
    
    @Override
    public int compareTo(ConnectedQuizClient clnt2)
    {
        if (this.right_answeres > clnt2.right_answeres) 
                return -1;
        else 
                return 1;
    
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
            String regexPatternQuestion = "^\\d+[.] [a-zA-Z0-9ŠĆČŽĐćčšđž.? :,\\\"]+[:?]$";
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
                            // Is this regex okay? Lowercase letter issue
                            String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!?=*-_])[A-Za-z0-9!_()?$!]{6,}(:admin|:contestant)?$";
                            Pattern patternLogin = Pattern.compile(login_regex);
                            Matcher matcherLogin = patternLogin.matcher(login_info);
                            if(matcherLogin.find())
                            {
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
                                        this.username = u.getUsername();
                                        this.role = u.getRole();
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
                            }
                            else
                            {
                                System.out.println("Wrong login format!");
                                this.pw.println("Fail");
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
                        
                        //// LOG OUT HAS TO BE IN ALL STATES STARTING FROM THIS ONE ////
                        break;
                        
                    // State that loads the question set and prepares for a start of a quiz - in this state admins can add/remove players 
                    case "START_QUIZ" :
                        System.out.println("Im in state START_QUIZ,just before reading a new set!");
                        String start_flag = br.readLine(); 
                        System.out.println(start_flag);
                        if(start_flag.startsWith("RequestSet"))
                        {
                            String [] tokens = start_flag.split(":");
                            String actSet = tokens[1];
                            if(!actSet.equals("idle"))
                            {
                                if(!this.previousSet.equals(actSet))
                                {
                                    this.questions.clear();
                                    readSet(actSet);
                                    this.previousSet = actSet;
                                    this.question_number = 0;
                                    this.pw.println("RequestSet");
                                    this.state = "IN_GAME";
                                }
                                else
                                {
                                    System.out.println("Same set - Not possible");
                                    this.state = "START_QUIZ";
                                    this.pw.println("SameSetError");
                                }
                            }
                            else
                            {
                                this.pw.println("NoSet");
                            }
                            
                        }
                        if(start_flag.startsWith("Leaderboard"))
                        {
                            Collections.sort(allClients);
                            int size = this.allClients.size();
                            System.out.println(allClients);
                            this.pw.println("NewLeaderboard-"+size+"-"+allClients);
                        } 
                        if(start_flag.startsWith("Start:"))
                        {
                            
                            String [] active_set_fetch = start_flag.split(":");
                            String active_set = active_set_fetch[1];
                            System.out.println(active_set);
                            if(!this.previousSet.equals(active_set))
                            {
                                readSet(active_set);
                                this.previousSet = active_set;
                                this.question_number = 0;
                                System.out.println(questions);
                                for(ConnectedQuizClient clnt : this.allClients)
                                {
                                    clnt.pw.println("SetLoaded:"+active_set);
                                }
                                this.state = "IN_GAME";
                            }
                            else
                            {
                                System.out.println("Same set - Not possible");
                                this.state = "START_QUIZ";
                                this.pw.println("SameSetError");
                            }
                            System.out.println("Question number: " + this.question_number);
                        }
                        if(start_flag.startsWith("AddPlayer"))
                        {
                            String [] new_player_token = start_flag.split(",");
                            String new_player_info = new_player_token[1];
                            String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!?=*-_])[A-Za-z0-9!_()?$!]{6,}(:admin|:contestant)?$";
                            Pattern patternLogin = Pattern.compile(login_regex);
                            Matcher matcherLogin = patternLogin.matcher(new_player_info);
                            if(matcherLogin.find())
                            {
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
                                        this.pw.println("AddOk");
                                        FileWriter fw = new FileWriter(fp,true);
                                        fw.write("\n");
                                        fw.write(new_player_info);
                                        fw.flush();
                                    }
                                    else
                                    {
                                        System.out.println("User already exists!");
                                    }
                                }
                                else
                                {
                                    System.out.println("File not found");
                                   
                                }
                            }
                            else
                            {
                                    System.out.println("Wrong player info format!");
                                     this.pw.println("Fail");
                            }     
                        }
                        // Remove a player
                        
                        if(start_flag.startsWith("RemovePlayer"))
                        {
                
                            String [] new_player_token = start_flag.split(",");
                            String new_player_info = new_player_token[1];
                            String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!?=*-_])[A-Za-z0-9!_()?$!]{6,}(:admin|:contestant)?$";
                            Pattern patternLogin = Pattern.compile(login_regex);
                            Matcher matcherLogin = patternLogin.matcher(new_player_info);
                            if(matcherLogin.find())
                            {
                                
                                File fp = new File("./users.txt");
                                if(fp.exists())
                                {
                                    String remainingUsers = "";
                                    BufferedReader in = new BufferedReader(new FileReader(fp));
                                    String line;                       
                                    while((line = in.readLine()) != null)
                                    {   
                                        if(!line.equals(new_player_info))
                                        {
                                            remainingUsers+=line + "\n";
                                        }
                                    }
                                    
                                    // This cleares the file
                                    PrintWriter pw = new PrintWriter(fp);
                                    pw.flush();
                                    
                                    this.pw.println("RemoveOk");
                                    
                                    // This writes the remaining users in the blank file 
                                    FileWriter fw = new FileWriter(fp,true);
                                    fw.write(remainingUsers);
                                    fw.flush();    
                                }
                                else
                                {
                                    System.out.println("File not found");
                                }     
                            }
                        }
                    break;
                                 
                    
                    case "IN_GAME" : 
                        //System.out.println("Im in IN_GAME state");
                        //System.out.println("Question number: " + this.question_number);
                        String new_question = br.readLine();
                        if(new_question.startsWith("Start:"))   // This is added 
                        {                     
                            String [] active_set_fetch = new_question.split(":");
                            String active_set = active_set_fetch[1];
                            System.out.println(active_set);
                            if(!this.previousSet.equals(active_set))
                            {
                                System.out.println("IN_GAME_STATE, New set requested : " + active_set);
                                this.questions.clear();
                                readSet(active_set);
                                this.previousSet = active_set;
                                this.question_number = 0;
                                System.out.println(questions);
                                for(ConnectedQuizClient clnt : this.allClients)
                                {
                                    clnt.pw.println("SetLoaded:"+active_set);
                                }
                                this.state = "IN_GAME";
                            }
                            else
                            {
                                System.out.println("Same set - Not possible");
                                this.state = "START_QUIZ";
                                this.pw.println("SameSetError");
                            }
                            System.out.println("Question number: " + this.question_number);
                        }
                         
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
                            this.questions_answered++;
                            
                            // At the end of a set, go back to a state that reads a new set of questions
                            if(question_number == 11)
                            {
                                this.endOfSet = true;
                            }
                            
                        }
                        if(this.endOfSet == true)
                        {
                            this.pw.println("EndOfSet");
                            System.out.println("End of a question set");
                            this.state = "START_QUIZ";
                            this.question_number = 0;
                            this.questions.clear();
                            this.endOfSet = false;
                        }
                        if(new_question.startsWith("NewAnswer"))
                        {
                            String [] answer_token = new_question.split(":");
                            String question_numRcvd_Sp = answer_token[1].trim();
                            int question_numRcvd = Integer.parseInt(question_numRcvd_Sp);
                            //System.out.println(question_numRcvd);
                            String answer_text = answer_token[2];
                            String real_answer = this.questions.get(question_numRcvd-1).getAnswerD().getAnswerText();
                            if(real_answer.substring(real_answer.indexOf(')')+2).equals(answer_text))
                            {
                                this.right_answeres++;
                                System.out.println("Correct answer");
                            }
                            else
                            {
                                System.out.println("Wrong answer");
                            }
                        }
                        
                        if(new_question.startsWith("SwapQ"))
                        {
                            String swap_question_text = this.questions.get(10).getText();
                            String swap_A = this.questions.get(10).getAnswerA().getAnswerText();
                            String swap_B = this.questions.get(10).getAnswerB().getAnswerText();
                            String swap_C = this.questions.get(10).getAnswerC().getAnswerText();
                            String swap_D = this.questions.get(10).getAnswerD().getAnswerText();
                            this.pw.println("SwapQ:"+swap_question_text+swap_A+":"+swap_B+":"+swap_C+":"+swap_D);
                            this.questions_answered++;
                        }
                        if(new_question.startsWith("5050"))
                        {
                            String[]token_50 = new_question.split(":");
                            String questionNum = token_50[1].trim();
                            int questionNumInt = Integer.parseInt(questionNum);
                            System.out.println(questionNumInt);
                            Question currentQ = this.questions.get(questionNumInt-1);
                            String falseAnswers = "5050";
                            int cnt = 0;
                            if(currentQ.getAnswerA().isCorrect() == false)
                            {
                                falseAnswers = falseAnswers+":"+currentQ.getAnswerA().getAnswerText();
                                cnt++;
                            }
                            if(currentQ.getAnswerB().isCorrect() == false)
                            {
                                falseAnswers = falseAnswers+":"+currentQ.getAnswerB().getAnswerText();
                                cnt++;
                            }
                            if(cnt == 2)
                            {
                                this.pw.println(falseAnswers);
                                cnt = 0;
                            }
                            if(currentQ.getAnswerC().isCorrect() == false)
                            {
                                falseAnswers = falseAnswers+":"+currentQ.getAnswerC().getAnswerText();
                                cnt++;
                            }
                            if(cnt == 2)
                            {
                                this.pw.println(falseAnswers);
                                cnt = 0;
                            }                        
                        }
                        if(new_question.startsWith("FriendHelp"))
                        {
                            String [] friend_token = new_question.split(":");
                            String whoAsks = friend_token[1];
                            String whoHelps = friend_token[2];
                            String questionToAsk = friend_token[3];
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                if(clnt.username.equals(whoHelps) && !clnt.username.equals(whoAsks))
                                {
                                    clnt.pw.println("Friend:"+whoAsks+":"+questionToAsk);
                                    break;
                                }
                            }
                        }
                        
                        if(new_question.startsWith("Response"))
                        {
                            String[]responseToken = new_question.split(":");
                            String whoAsked = responseToken[1].trim();
                            String responseText = responseToken[2];
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                if(clnt.username.equals(whoAsked))
                                {
                                    clnt.pw.println("Response:"+responseText);
                                    break;
                                }
                            }
                        }
                        
                        if(new_question.startsWith("LogOut"))
                        {
                            String [] token = new_question.split(":");
                            String whoLogsOut = token[1].trim();
                            File fp = new File("./playerStats.txt");
                            if(fp.exists())
                            {
                                String logOutInfo;
                                logOutInfo = this.username + ":" + this.right_answeres + ":" + this.questions_answered + "\n";                                                     
                                FileWriter fw = new FileWriter(fp,true);
                                fw.write(logOutInfo);
                                fw.flush();
                                this.pw.println("LogOutOK");
                            }
                            else 
                            {
                                System.out.println("File not found!");
                            }
                            for(ConnectedQuizClient clnt : this.allClients)
                            {
                                if(!clnt.username.equals(whoLogsOut))
                                {
                                    clnt.pw.println("PlayerOut:" + whoLogsOut);
                                }
                            }
                            
                        }
                        
                        if(new_question.startsWith("Leaderboard"))
                        {
                            Collections.sort(allClients);
                            int size = this.allClients.size();
                            System.out.println(allClients);
                            this.pw.println("NewLeaderboard-"+size+"-"+allClients);
                        }               
                }
                
            } catch (IOException ex) {
                Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
}
