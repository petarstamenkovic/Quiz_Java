
package quiz_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RecieveMessageFromServer implements Runnable {
    
    private Quiz_Client parent;
    private BufferedReader br;
    
    public RecieveMessageFromServer(Quiz_Client parent)
    {
        this.parent = parent;
        this.br = parent.getBr();
    }
    
    @Override
    public void run()
    {
        while(true)
        {
            try {
                String line;
                line = this.br.readLine();
                System.out.println(line);  
                
                if(line.startsWith("Login"))
                {
                    if(!"Login failed".equals(line))        // Login success block - Try to upadate combo box here
                    {
                        parent.getEnterButton().setEnabled(true);
                        String [] check_role = line.split(":");
                        parent.getAllPlayers().addItem(check_role[1]);  // This is potentially issue with double listing!
                        if(check_role[2].equals("admin"))
                        {
                            parent.getAllPlayers().setEnabled(true);
                            parent.getQuestionSet().setEnabled(true);
                            parent.getQuestionSet().addItem("Set 1");
                            parent.getQuestionSet().addItem("Set 2");
                            parent.getQuestionSet().addItem("Set 3");
                            parent.getQuestionSet().addItem("Set 4");
                            parent.getAnswerA().setEnabled(true);
                            parent.getAnswerB().setEnabled(true);
                            parent.getAnswerC().setEnabled(true);
                            parent.getAnswerD().setEnabled(true);
                            parent.getHelp5050().setEnabled(true);
                            parent.getHelpFriend().setEnabled(true);
                            parent.getHelpSwap().setEnabled(true);
                            parent.getLoginArea().setEnabled(false);
                            parent.getAddRemovePlayerArea().setEnabled(true);
                            parent.getAddPlayer().setEnabled(true);
                            parent.getRemovePlayer().setEnabled(true);
                        }
                        else if (check_role[2].equals("contestant"))
                        {
                            parent.getAllPlayers().setEnabled(true);
                            parent.getAnswerA().setEnabled(true);
                            parent.getAnswerB().setEnabled(true);
                            parent.getAnswerC().setEnabled(true);
                            parent.getAnswerD().setEnabled(true);
                            parent.getHelp5050().setEnabled(true);
                            parent.getHelpFriend().setEnabled(true);
                            parent.getHelpSwap().setEnabled(true);
                            parent.getLoginArea().setEnabled(false);
                        }
                    }
                }
                // LOGIN_OK STATE
                if(line.startsWith("Users:"))
                {   
                    parent.getAllPlayers().removeAllItems();
                    System.out.println(line);
                    String [] token = line.split(":");
                    String all_users = token[2];
                    String [] names = all_users.split(",");
                    int num_users = Integer.parseInt(token[1]);
                    for(int i = 0 ; i < num_users ; i = i + 2)
                    {
                        parent.getAllPlayers().addItem(names[i]);
                    }
                    
                }
            } catch (IOException ex) {
                Logger.getLogger(RecieveMessageFromServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
