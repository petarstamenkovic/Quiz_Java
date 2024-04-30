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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class ConnectedQuizClient implements Runnable , Comparable<ConnectedQuizClient>{
    
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private ArrayList<ConnectedQuizClient> allClients;
    private String state;
    private String username;
    private String password;
    private String role;
    private static final String AES = "AES";
  
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private ArrayList<User> possibleUsers; 
    private ArrayList<Question> questions;
    private int question_number;
    private int questions_answered;
    private int right_answeres;
    private String previousSet;
    private boolean endOfSet;
    private SecretKey key;
    private byte[] init_vector;
    
    // Constructor 
    public ConnectedQuizClient(Socket socket,ArrayList<ConnectedQuizClient> allClients, SecretKey key,byte[] init_vector)
    {
        try {
            this.socket = socket;
            this.allClients = allClients;           
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
            this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()),true);
            this.key = key;
            this.init_vector = init_vector;
            this.username = "";
            this.password = "";
            this.role = "";
            this.state = "CONNECT";
            this.possibleUsers = new ArrayList<>();
            this.questions = new ArrayList<>();
            this.questions_answered = 0;
            this.right_answeres = 0;
            this.previousSet = "";
            this.endOfSet = false;
        } catch (IOException ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // Overriden method that prints out a client in usable form
    @Override 
    public String toString()
    {
        return "Name: " + this.username + ": " + this.right_answeres + " / " + this.questions_answered; 
    }
    
    // Overriden method that uses comparable interface to sort players
    @Override
    public int compareTo(ConnectedQuizClient clnt2)
    {
        if (this.right_answeres > clnt2.right_answeres) 
                return -1;
        else 
                return 1;
    
    }
    
    public static byte[] do_AESEncryption(String plainText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector); 
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        return cipher.doFinal(plainText.getBytes());
    }

    public static String do_AESDecryption(byte[] cipherText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM); 
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] result = cipher.doFinal(cipherText);
        return new String(result);
    }
    
    // Method that reads the users at the start of a program in a possibleUsers array list, loading initial admin
    public void readUsers() throws FileNotFoundException, IOException, Exception
    {
        Path path = Paths.get("./users.txt");
        byte[] encMessage = Files.readAllBytes(path);

        String decryptedText = do_AESDecryption(encMessage, this.key, this.init_vector);
        System.out.println(decryptedText);
        String [] users_token = decryptedText.split("\n");
        for(int i = 0 ; i < users_token.length ; i++)
        {
            String [] userToken = users_token[i].split(":");
            User user = new User(userToken[0],userToken[1],userToken[2]);
            possibleUsers.add(user);
        }   
    }
    
    // Method that reads the setN.txt files and loads the informaton in a question array list
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
                }
                if(matcherB.find()) 
                {   
                    System.out.println("Found a wrong answer b)");
                    answerBText = line;
                }
                if(matcherC.find())
                {
                    System.out.println("Found a wrong answer c)");
                    answerCText = line;
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
    
    // Overriden method that runs upon creating a new client thread
    @Override
    public void run()
    {
        try {
            readUsers();
        } catch (IOException ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true)
        {
            try {
                // FSM that leads the client in adequate states
                switch(this.state)
                {
                    // STATE - CLIENTS IS NOT CONNECTED YET
                    case "CONNECT" :
                        String connect_flag = br.readLine();
                        if(connect_flag.startsWith("Connect"))
                        {
                            this.pw.println("Success");
                            this.state = "LOGIN";
                        }
                        break;
                        
                    // STATE - CLIENT IS TRYING TO LOG IN - UPON FAILING COME BACK HERE    
                    case "LOGIN" :
                        try {
                            String login_info = br.readLine();
                            String [] loginToken = login_info.split(":");
                            
                            // If that requires a xxx:xxx format
                            if(loginToken.length == 3)
                            {
                                String userPassText = loginToken[0]+":"+loginToken[1];
                                String roleLogin = loginToken[2];
                                String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9:])[A-Za-z\\d\\W]{6,}$";
                                String role_regex = "^(admin|contestant)$";

                                Pattern patternLogin = Pattern.compile(login_regex);
                                Pattern patternRole = Pattern.compile(role_regex);

                                Matcher matcherRole = patternRole.matcher(roleLogin);
                                Matcher matcherLogin = patternLogin.matcher(userPassText);
                                
                                // All okay - continute
                                if(matcherLogin.find() && matcherRole.find())
                                {
                                    System.out.println(login_info);
                                    String [] login_token = login_info.split(":");
                                    this.username = login_token[0];
                                    this.password = login_token[1];
                                    this.role = login_token[2];
                                    System.out.println(this.possibleUsers);
                                    int match = 0;
                                    for(User u : this.possibleUsers)
                                    {
                                        if(u.getUsername().equals(this.username) && u.getPassword().equals(this.password) && u.getRole().equals(this.role))
                                        {
                                            match = 1;
                                            System.out.println("You found the match");
                                            String message = "Login ok:" + this.username + ":" + this.password + ":" + this.role;
                                            this.username = u.getUsername();
                                            this.role = u.getRole();
                                            this.password = u.getPassword();
                                            
                                            // Check for backup
                                            File fp = new File("./playerStats.txt");
                                            BufferedReader in = new BufferedReader(new FileReader(fp));
                                            String line;
                                            if (fp.exists())
                                            {
                                                while((line = in.readLine()) != null)
                                                {
                                                    String [] backUpToken = line.split(":");
                                                    String backUpUser = backUpToken[0];
                                                    int backUpPoints = Integer.parseInt(backUpToken[1]);
                                                    int backUpQuestions = Integer.parseInt(backUpToken[2]);
                                                    if(backUpUser.equals(this.username))
                                                    {
                                                        System.out.println("BackUp found!");
                                                        this.right_answeres = this.right_answeres + backUpPoints;
                                                        this.questions_answered = this.questions_answered + backUpQuestions;
                                                    }
                                                    else
                                                    {
                                                        System.out.println("Login OK! No back up found!");
                                                    }
                                                }
                                                
                                            }
                                            this.pw.println(message);
                                            this.state = "CHECK_OK";    // Proceed to a next state
                                            break;
                                        }
                                    }
                                    
                                    // No user found, stay in this state
                                    if(match == 0)
                                    {
                                        System.out.println("No match found in db, login failed");
                                        this.pw.println("Failed login");
                                        this.state = "LOGIN";
                                        break;
                                    }
                                }
                                else
                                {
                                    System.out.println("Wrong login format!");
                                    this.pw.println("FailFormat");
                                    this.state = "LOGIN";
                                    break;
                                }
                            }
                            else
                            {
                                System.out.println("Wrong input type!");
                                this.pw.println("TypeFailure");
                                this.state = "LOGIN";
                                break;
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        break;

                    // STATE - WAIT FOR ENTER PRESS TO ENTER A GAME ROOM - UPON PRESSING ENTER AN ARRAY LIST
                    case "CHECK_OK" :
                        String enter_update = br.readLine();
                        int num_users = this.allClients.size();
                        String all_users = "Users:"+num_users+":";
                        
                        // If block that updates everyones players lists
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
                        
                        // If block that logs a player out
                        if(enter_update.startsWith("LogOut"))
                        {
                            String [] token = enter_update.split(":");
                            String whoLogsOut = token[1].trim();
                            
                            Iterator<ConnectedQuizClient> it = this.allClients.iterator();
                            while (it.hasNext()) 
                            {
                                if (it.next().username.equals(whoLogsOut)) 
                                {
                                    it.remove();
                                }
                            }
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
                        break;
                        
                    // STATE - HERE YOU SET/REQUEST SET DEPENDING ON WHAT ROLE YOU HAVE    
                    case "START_QUIZ" :
                        String start_flag = br.readLine();
                        
                        // If block that fetches a set
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
                        
                        // If that requests a leaderboard
                        if(start_flag.startsWith("Leaderboard"))
                        {
                            Collections.sort(allClients);
                            int size = this.allClients.size();
                            System.out.println(allClients);
                            this.pw.println("NewLeaderboard-"+size+"-"+allClients);
                        } 
                        
                        // If that loads a set - admin
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
                        
                        // If that allows admin to add a player
                        if(start_flag.startsWith("AddPlayer"))
                        {
                            String [] new_player_token = start_flag.split(",");
                            String new_player_info = new_player_token[1];
                            String [] token = new_player_info.split(":");
                            if(token.length == 3)
                            {
                                String userPass = token[0] + ":" + token[1];
                                String rolePlayer = token[2];
                                String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d\\W]{6,}$";
                                String role_regex = "^(admin|contestant)$";

                                Pattern patternRole = Pattern.compile(role_regex);
                                Pattern patternLogin = Pattern.compile(login_regex);

                                Matcher matcherLogin = patternLogin.matcher(userPass);
                                Matcher matcherRole = patternRole.matcher(rolePlayer);
                                if(matcherLogin.find() && matcherRole.find())
                                {
                                    // Here we decrypt
                                    Path path = Paths.get("./users.txt");
                                    byte[] encMessage = Files.readAllBytes(path);
                                    String decryptedText = do_AESDecryption(encMessage, this.key, this.init_vector);               
                                    boolean addOK = true;
                                    String [] addToken = decryptedText.split("\n");
                                    for(int i = 0 ; i < addToken.length ; i++)
                                    {
                                        String [] tokenSamePlayer = addToken[i].split(":");
                                        String usernameCheck = tokenSamePlayer[0];
                                        if(token[0].equals(usernameCheck))
                                        {
                                            addOK = false;
                                            System.out.println("Found the same player, cant add him again");
                                            break;
                                        }
                                    }

                                    if(addOK == true)
                                    {
                                        decryptedText+="\n"+new_player_info;
                                        this.pw.println("AddOk");
                                        encMessage = do_AESEncryption(decryptedText,this.key,this.init_vector);
                                        Files.write(path,encMessage);
                                    }
                                    else
                                    {
                                        System.out.println("User already exists");
                                        this.pw.println("ExistingUser");
                                    }
                                    System.out.println("New Player added!");
                                    System.out.println(decryptedText);
                                }
                                else
                                {
                                    System.out.println("Invalid format!");
                                    this.pw.println("FailFormat");
                                }
                            }
                            else
                            {
                                System.out.println("Invalid format!");
                                this.pw.println("TypeFailure");
                            }
                        }     
                         
                        // If that allows admin to remove a player
                        if(start_flag.startsWith("RemovePlayer"))
                        {
                            String [] new_player_token = start_flag.split(",");
                            if(new_player_token.length == 2)
                            {    
                                String new_player_info = new_player_token[1];          
                                String usernameRegex = "^[a-zA-Z]{1,}[A-Za-z0-9]*$";                        
                                Pattern patternLogin = Pattern.compile(usernameRegex);      
                                Matcher matcherLogin = patternLogin.matcher(new_player_info);                            
                                if(matcherLogin.find())
                                {
                                    String newPlayerList = null;
                                    Path path = Paths.get("./users.txt");
                                    byte[] encMessage = Files.readAllBytes(path);
                                    String decryptedText = do_AESDecryption(encMessage, this.key, this.init_vector);   
                                    String [] userToken = decryptedText.split("\n");
                                    for(int i = 0 ; i < userToken.length ; i++)
                                    {
                                        String [] currentToken = userToken[i].split(":");
                                        String currentUsername = currentToken[0];
                                        if(!new_player_info.equals(currentUsername))
                                        {
                                            newPlayerList+="\n"+userToken[i];
                                        }
                                        else
                                        {
                                            System.out.println("Found the player to remove.");
                                            this.pw.println("RemoveOk");
                                        }
                                    }

                                    encMessage = do_AESEncryption(newPlayerList,this.key,this.init_vector);
                                    Files.write(path,encMessage);

                                }
                                else
                                {
                                    System.out.println("Invalide remove format!");
                                    this.pw.println("xRemove");
                                }
                            }
                            else
                            {
                                System.out.println("Cant leave this field blank!");
                                this.pw.println("404Remove");
                            }
                        }
                        
                        // If that logs out a player from this state
                        if(start_flag.startsWith("LogOut"))
                        {
                            String [] token = start_flag.split(":");
                            String whoLogsOut = token[1].trim();
       
                            Iterator<ConnectedQuizClient> it = this.allClients.iterator();
                            while (it.hasNext()) 
                            {
                                if (it.next().username.equals(whoLogsOut)) 
                                {
                                    it.remove();
                                }
                            }
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
                    break;
                                 
                    // STATE - GAME HAS STARTED
                    case "IN_GAME" : 

                        String new_question = br.readLine();
                        
                        // If that prepares a set 
                        if(new_question.startsWith("Start:")) 
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
                         
                        // If that covers a scenario upon receieving a new question
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
                        
                        // End of set scenario
                        if(this.endOfSet == true)
                        {
                            this.pw.println("EndOfSet");
                            System.out.println("End of a question set");
                            this.state = "START_QUIZ";
                            this.question_number = 0;
                            this.questions.clear();
                            this.endOfSet = false;
                        }
                        
                        // If that check the answer validity
                        if(new_question.startsWith("NewAnswer"))
                        {
                            String [] answer_token = new_question.split(":");
                            String question_numRcvd_Sp = answer_token[1].trim();
                            int question_numRcvd = Integer.parseInt(question_numRcvd_Sp);
                            String answer_text = answer_token[2];
                            String real_answer = this.questions.get(question_numRcvd-1).getAnswerD().getAnswerText();
                            if(real_answer.substring(real_answer.indexOf(')')+2).equals(answer_text))
                            {
                                this.right_answeres++;
                                System.out.println("Correct answer");
                                this.pw.println("CorrectAnswer");
                            }
                            else
                            {
                                System.out.println("Wrong answer");
                                this.pw.println("WrongAnswer");
                            }
                        }
                        
                        // If that activates swap question help
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
                        
                        // If that activates a 50/50 help
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
                        
                        // If that activates a friend help
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
                        
                        // If that covers a friend response scenario
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
                        
                        // If that logs the player out
                        if(new_question.startsWith("LogOut"))
                        {
                            String [] token = new_question.split(":");
                            String whoLogsOut = token[1].trim();

                            Iterator<ConnectedQuizClient> it = this.allClients.iterator();
                            while (it.hasNext()) 
                            {
                                if (it.next().username.equals(whoLogsOut)) 
                                {
                                    it.remove();
                                }
                            }
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
            } catch (Exception ex) {
                Logger.getLogger(ConnectedQuizClient.class.getName()).log(Level.SEVERE, null, ex);
            }

    }
    
}
}
