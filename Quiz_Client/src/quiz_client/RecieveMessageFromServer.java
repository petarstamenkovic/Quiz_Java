
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
                
                // IF block that connects a player to a game
                if(line.startsWith("Success"))
                {
                    parent.getCheckButton().setEnabled(true);
                    parent.getConnectButton().setEnabled(false);
                    parent.getLoginArea().setEnabled(true);
                }
                
                // IF block that checks players credidentials
                if(line.startsWith("Login ok:"))
                {
                        parent.getEnterButton().setEnabled(true);
                        parent.getCheckButton().setEnabled(false);
                        String [] check_role = line.split(":");
                        if(check_role[2].equals("admin"))                    // Log in admin
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
                            parent.getStartButton().setEnabled(true);
                        }
                        else if (check_role[2].equals("contestant"))        // Log in contestant
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
                            parent.getStartButton().setVisible(false);
                      
                        }   
                }
 
                // LOGIN_OK STATE - ENTER_UPDATE
                if(line.startsWith("Users:"))
                {   
                    parent.getAllPlayers().removeAllItems();
                    System.out.println(line);
                    String [] token = line.split(":");
                    int num_users = Integer.parseInt(token[1]);
                    String all_users = token[2];
                    String [] names = all_users.split(",");
                    
                    for(int i = 0 ; i < num_users ;i++)
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
