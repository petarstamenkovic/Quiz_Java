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
import java.util.Arrays;
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
  
    // Block cipher(CBC mode) - tzv Blok sifra kod koje se originalna poruka sifruje po grupama (blokovima)
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private ArrayList<User> possibleUsers; 
    private ArrayList<Question> questions;
    private int question_number;
    private int questions_answered;
    private int right_answeres;
    private String previousSet;
    private String activeSet;
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
        
        
        /*
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
        */
        
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
        //System.out.println(this.key);
        //System.out.println(Arrays.toString(this.init_vector));
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
                            String [] loginToken = login_info.split(":");
                            
                            if(loginToken.length == 3)
                            {
                                String userPassText = loginToken[0]+":"+loginToken[1];
                                String roleLogin = loginToken[2];
                                System.out.println(login_info);
                                System.out.println(userPassText);
                                System.out.println(roleLogin);
                                // Is this regex okay? Lowercase letter issue
                                String login_regex = "^[a-zA-Z]{1,}[A-Za-z0-9]*:(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9:])[A-Za-z\\d\\W]{6,}$";
                                String role_regex = "^(admin|contestant)$";


                                Pattern patternLogin = Pattern.compile(login_regex);
                                Pattern patternRole = Pattern.compile(role_regex);

                                Matcher matcherRole = patternRole.matcher(roleLogin);
                                Matcher matcherLogin = patternLogin.matcher(userPassText);
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
                                            this.state = "CHECK_OK";
                                            break;
                                        }
                                    }
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
                        
                        if(enter_update.startsWith("LogOut"))
                        {
                            String [] token = enter_update.split(":");
                            String whoLogsOut = token[1].trim();
                            /*
                            for(int i = 0 ; i < this.allClients.size() ; i++)
                            {
                                if(this.allClients.get(i).username.equals(whoLogsOut))
                                {
                                    this.allClients.remove(i);
                                    break;
                                }
                            }
                            */
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
                                        if(new_player_info.equals(addToken[i]))
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
                         
                        // Remove a player
                        if(start_flag.startsWith("RemovePlayer"))
                        {
                            String [] new_player_token = start_flag.split(",");
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
                        
                        if(start_flag.startsWith("LogOut"))
                        {
                            String [] token = start_flag.split(":");
                            String whoLogsOut = token[1].trim();
                            /*
                            for(int i = 0 ; i < this.allClients.size() ; i++)
                            {
                                if(this.allClients.get(i).username.equals(whoLogsOut))
                                {
                                    this.allClients.remove(i);
                                    break;
                                }
                            }
                            */
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
                                this.pw.println("CorrectAnswer");
                            }
                            else
                            {
                                System.out.println("Wrong answer");
                                this.pw.println("WrongAnswer");
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
                            /*
                            for(int i = 0 ; i < this.allClients.size() ; i++)
                            {
                                if(this.allClients.get(i).username.equals(whoLogsOut))
                                {
                                    this.allClients.remove(i);
                                    break;
                                }
                            }
                            */
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
