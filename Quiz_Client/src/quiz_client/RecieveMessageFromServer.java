
package quiz_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


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
                    parent.getStartButton().setVisible(false);
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
                            parent.getQuestionSet().setVisible(false);
                            
                            parent.getQuestionSet().addItem("Set 1");
                            parent.getQuestionSet().addItem("Set 2");
                            parent.getQuestionSet().addItem("Set 3");
                            parent.getQuestionSet().addItem("Set 4");
                            parent.getAnswerA().setEnabled(true);
                            parent.getAnswerA().setVisible(false);
                            parent.getAnswerB().setEnabled(true);
                            parent.getAnswerB().setVisible(false);
                            parent.getAnswerC().setEnabled(true);
                            parent.getAnswerC().setVisible(false);
                            parent.getAnswerD().setEnabled(true);
                            parent.getAnswerD().setVisible(false);
                            
                            parent.getHelp5050().setEnabled(true);
                            parent.getHelp5050().setVisible(false);
                            parent.getHelpFriend().setEnabled(true);
                            parent.getHelpFriend().setVisible(false);
                            parent.getHelpSwap().setEnabled(true);
                            parent.getHelpSwap().setVisible(false);
                            parent.getLoginArea().setEnabled(false);
                            
                            parent.getLeaderboardButton().setVisible(false);
                            parent.getAddRemovePlayerArea().setEnabled(true);
                            parent.getAddRemovePlayerArea().setVisible(false);
                            parent.getAddPlayer().setEnabled(true);
                            parent.getAddPlayer().setVisible(false);
                            parent.getRemovePlayer().setEnabled(true);
                            parent.getRemovePlayer().setVisible(false);
                            parent.getStartButton().setEnabled(true);
                        }
                        else if (check_role[2].equals("contestant"))        // Log in contestant
                        {
                            parent.getAllPlayers().setEnabled(true);
                            parent.getAllPlayers().setVisible(false);
                            
                            // Hide answer options until you load the set
                            parent.getAnswerA().setEnabled(true);
                            parent.getAnswerA().setVisible(false);
                            parent.getAnswerB().setEnabled(true);
                            parent.getAnswerB().setVisible(false);
                            parent.getAnswerC().setEnabled(true);
                            parent.getAnswerC().setVisible(false);
                            parent.getAnswerD().setEnabled(true);
                            parent.getAnswerD().setVisible(false);
                            
                            // Hide help buttons until needed
                            parent.getHelp5050().setEnabled(true);
                            parent.getHelp5050().setVisible(false);
                            parent.getHelpFriend().setEnabled(true);
                            parent.getHelpFriend().setVisible(false);
                            parent.getHelpSwap().setEnabled(true);
                            parent.getHelpSwap().setVisible(false);
                            
                            parent.getLeaderboardButton().setVisible(false);
                            parent.getLoginArea().setEnabled(false);
                            parent.getStartButton().setVisible(false);
                            
                            // Contestant cant use this
                            parent.getAddRemovePlayerArea().setVisible(false);
                            parent.getAddPlayer().setVisible(false);
                            parent.getRemovePlayer().setVisible(false);
                      
                        }   
                }
                
                if(line.startsWith("Fail"))
                {
                    JOptionPane.showMessageDialog(null, "Wrong login format!", "Error", JOptionPane.ERROR_MESSAGE);
                }
 
                // LOGIN_OK STATE - ENTER_UPDATE - HERE IS WHAT HAPPENS WITH GUI AFTER PRESSING ENTER BUTTON
                if(line.startsWith("Users:"))
                {   
                    parent.getAllPlayers().removeAllItems();
                    parent.getQuestionSet().setVisible(true);
                    
                    System.out.println(line);
                    String [] token = line.split(":");
                    int num_users = Integer.parseInt(token[1]);
                    String all_users = token[2];
                    String [] names = all_users.split(",");
                    
                    for(int i = 0 ; i < num_users ;i++)
                    {
                        parent.getAllPlayers().addItem(names[i]);
                    }
                    parent.getStartButton().setVisible(true);   
                }
                
                // If block that doesnt allow a player to pick same set two times in a row
                if(line.startsWith("SameSet"))
                {
                    JOptionPane.showMessageDialog(null, "Can not pick the same set again", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                if(line.startsWith("SetLoaded"))
                {
                    parent.getAnswerA().setVisible(true);
                    parent.getAnswerB().setVisible(true);
                    parent.getAnswerC().setVisible(true);
                    parent.getAnswerD().setVisible(true);
                    parent.getQuestionButton().setVisible(true);
                    parent.getSubmintAnswerButton().setVisible(true);
                    parent.getHelp5050().setVisible(true);
                    parent.getHelpFriend().setVisible(true);
                    parent.getHelpSwap().setVisible(true);
                    parent.getLeaderboardButton().setVisible(true);
                    parent.getQuestionSet().setEnabled(false);
                    parent.getStartButton().setEnabled(false);
                }
                // Question recieving
                if(line.startsWith("NewQuestion"))
                {
                    String [] question_token = line.split(":");
                    System.out.println(line);
                    String question_text = question_token[1];
                    String answerA = question_token[2];
                    String answerAShrink = answerA.substring(answerA.indexOf(')')+2);
                    
                    String answerB = question_token[3];
                    String answerBShrink = answerB.substring(answerB.indexOf(')')+2);
                    
                    String answerC = question_token[4];
                    String answerCShrink = answerC.substring(answerC.indexOf(')')+2);

                    String answerD = question_token[5];
                    String answerDShrink = answerD.substring(answerD.indexOf(')')+2);

                    parent.getQuestionArea().setText("");
                    parent.getQuestionArea().append(question_text);
                    parent.getAnswerA().setText(answerAShrink);
                    parent.getAnswerB().setText(answerBShrink);
                    parent.getAnswerC().setText(answerCShrink);
                    parent.getAnswerD().setText(answerDShrink);
                }
                if(line.startsWith("EndOfSet"))
                {
                    parent.getAnswerA().setEnabled(false);
                    parent.getAnswerB().setEnabled(false);
                    parent.getAnswerC().setEnabled(false);
                    parent.getAnswerD().setEnabled(false);
                    parent.getQuestionButton().setEnabled(false);
                    parent.getSubmintAnswerButton().setEnabled(false);
                    parent.getHelp5050().setEnabled(false);
                    parent.getHelpFriend().setEnabled(false);
                    parent.getHelpSwap().setEnabled(false);
                    parent.getStartButton().setEnabled(true);
                    parent.getQuestionSet().setEnabled(true);
                    
                }
                if(line.startsWith("NewLeaderboard"))
                {
                    String [] token = line.split("-");
                    String playerList = token[1];
                    JTextArea textArea = new JTextArea();
                    textArea.setEditable(false); // Make it non-editable
                    textArea.setText("This is the leaderboard.\n" + playerList);
                    JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "Leaderboard", JOptionPane.PLAIN_MESSAGE);      
                }
                
            } catch (IOException ex) {
                Logger.getLogger(RecieveMessageFromServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
