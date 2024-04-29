
package quiz_client;

import java.awt.Color;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


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
                    parent.getRoleBox().setEnabled(true);
                }
                
                if(line.startsWith("TypeFailure"))
                {
                    JOptionPane.showMessageDialog(null, "Login info format error \n Should be xxx:xxx", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                // IF block that checks players credidentials
                if(line.startsWith("Login ok:"))
                {
                        parent.getLogOutButton().setEnabled(true);
                        parent.getEnterButton().setEnabled(true);
                        parent.getCheckButton().setEnabled(false);
                        String[]token = line.split(":");
                        String loginInfo = token[1] + ":" + token[2] + ":" + token[3].trim();
                        parent.getLoginArea().setText(loginInfo);
                        String [] check_role = line.split(":");
                        if(check_role[3].equals("admin"))                    // Log in admin
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
                            parent.getStartButton().setVisible(false);
                            parent.getRequestSetButton().setEnabled(false);
                            parent.getRequestSetButton().setVisible(false);
                        }
                        else if (check_role[2].equals("contestant"))        // Log in contestant
                        {
                            parent.getQuestionSet().setVisible(false);
                            parent.getAllPlayers().setEnabled(true);                     
                            
                            parent.getQuestionSet().addItem("Set 1");
                            parent.getQuestionSet().addItem("Set 2");
                            parent.getQuestionSet().addItem("Set 3");
                            parent.getQuestionSet().addItem("Set 4");
                            
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
                            parent.getStartButton().setEnabled(false);
                            parent.getRequestSetButton().setEnabled(true);
                            parent.getRequestSetButton().setVisible(false);
                            
                            // Contestant cant use this
                            parent.getAddRemovePlayerArea().setVisible(false);
                            parent.getAddPlayer().setVisible(false);
                            parent.getRemovePlayer().setVisible(false);
                            parent.getAddRemovePlayerArea().setEnabled(false);
                            parent.getAddPlayer().setEnabled(false);
                            parent.getRemovePlayer().setEnabled(false);
                      
                        }   
                }
                
                if(line.startsWith("FailFormat"))
                {
                    JOptionPane.showMessageDialog(null, "Wrong login format!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                if(line.startsWith("Failed login"))
                {
                    JOptionPane.showMessageDialog(null, "This user is not in database!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                if(line.startsWith("AddOk"))
                {
                    parent.getAddRemovePlayerArea().setText("");
                }
                if(line.startsWith("RemoveOk"))
                {
                    parent.getAddRemovePlayerArea().setText("");
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
                    String [] token2 = parent.getLoginArea().getText().split(":");
                    String currentPlayerRole = token2[2];
                    if(currentPlayerRole.equals("admin"))
                    {
                        parent.getAddRemovePlayerArea().setVisible(true);
                        parent.getAddPlayer().setVisible(true);
                        parent.getRemovePlayer().setVisible(true);
                        parent.getStartButton().setVisible(true);
                        parent.getRequestSetButton().setVisible(false);
                        
                    }
                    else if(currentPlayerRole.equals("contestant"))
                    {
                        parent.getStartButton().setVisible(false);
                        parent.getRequestSetButton().setVisible(true);
                        parent.getRequestSetButton().setEnabled(true);
                        parent.getAllPlayers().setEnabled(true);
                    }
                }
                
                // If block that doesnt allow a player to pick same set two times in a row
                if(line.startsWith("SameSet"))
                {
                    JOptionPane.showMessageDialog(null, "Can not pick the same set again", "Error", JOptionPane.ERROR_MESSAGE);
                }
                if(line.startsWith("NoSet"))
                {
                    JOptionPane.showMessageDialog(null, "Admin did not select a set!", "Error", JOptionPane.ERROR_MESSAGE);
                }
                if(line.startsWith("SetLoaded"))
                {
                    String [] activeSetToken = line.split(":");
                    String activeSet = activeSetToken[1];
                    parent.getActiveSetLabel().setText(activeSet);
                    parent.getAnswerA().setVisible(true);
                    parent.getAnswerA().setEnabled(true);
                    parent.getAnswerB().setVisible(true);
                    parent.getAnswerB().setEnabled(true);
                    parent.getAnswerC().setVisible(true);
                    parent.getAnswerC().setEnabled(true);
                    parent.getAnswerD().setVisible(true);
                    parent.getAnswerD().setEnabled(true);
                    parent.getQuestionButton().setVisible(true);
                    parent.getQuestionButton().setEnabled(true);
                    parent.getSubmintAnswerButton().setVisible(true);
                    parent.getHelp5050().setVisible(true);
                    parent.getHelpFriend().setVisible(true);
                    parent.getHelpSwap().setVisible(true);
                    parent.getLeaderboardButton().setVisible(true);
                    parent.getSubmintAnswerButton().setEnabled(false);
                    //parent.getQuestionSet().setEnabled(false);
                    //parent.getStartButton().setEnabled(false);
                }
                
                if(line.startsWith("RequestSet"))
                {
                    parent.getRequestSetButton().setEnabled(false);
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
                    parent.getAnswerA().setForeground(Color.black);
                    parent.getAnswerB().setText(answerBShrink);
                    parent.getAnswerB().setForeground(Color.black);
                    parent.getAnswerC().setText(answerCShrink);
                    parent.getAnswerC().setForeground(Color.black);
                    parent.getAnswerD().setText(answerDShrink);
                    parent.getAnswerD().setForeground(Color.black);
                    parent.getSubmintAnswerButton().setEnabled(true);
                }
                
                if(line.startsWith("CorrectAnswer"))
                {
                    JOptionPane.showMessageDialog(null, "Correct!", "Good job", JOptionPane.INFORMATION_MESSAGE);
                }
                
                if(line.startsWith("WrongAnswer"))
                {
                    JOptionPane.showMessageDialog(null, "Wrong!", "Not so good job", JOptionPane.ERROR_MESSAGE);
                }
                    
                if(line.startsWith("SwapQ"))
                {
                    String [] swap_token = line.split(":");
                    String swap_text = swap_token[1];
                    String swapA = swap_token[2];
                    String swapAShrink = swapA.substring(swapA.indexOf(')')+2);
                    String swapB = swap_token[3];
                    String swapBShrink = swapB.substring(swapB.indexOf(')')+2);
                    String swapC = swap_token[4];
                    String swapCShrink = swapC.substring(swapC.indexOf(')')+2);
                    String swapD = swap_token[5];
                    String swapDShrink = swapD.substring(swapD.indexOf(')')+2);
                    
                    parent.getQuestionArea().setText("");
                    parent.getQuestionArea().setText(swap_text);
                    parent.getAnswerA().setText(swapAShrink);
                    parent.getAnswerA().setForeground(Color.black);
                    parent.getAnswerB().setText(swapBShrink);
                    parent.getAnswerB().setForeground(Color.black);
                    parent.getAnswerC().setText(swapCShrink);
                    parent.getAnswerC().setForeground(Color.black);
                    parent.getAnswerD().setText(swapDShrink);
                    parent.getAnswerD().setForeground(Color.black);
                    parent.getSubmintAnswerButton().setEnabled(true);
                }
                
                if(line.startsWith("5050"))
                {
                    String [] falseTokens = line.split(":");
                    String falseOne = falseTokens[1].trim();
                    String falseTwo = falseTokens[2].trim();
                    String falseOneShrink = falseOne.substring(falseOne.indexOf(')')+2);
                    String falseTwoShrink = falseTwo.substring(falseTwo.indexOf(')')+2);
                    
                    if(parent.getAnswerA().getText().trim().equals(falseOneShrink) || parent.getAnswerA().getText().trim().equals(falseTwoShrink))
                    {
                        System.out.println("Hi, im on client side and i matched the wrong answer");
                        parent.getAnswerA().setForeground(Color.red);
                    }
                    if(parent.getAnswerB().getText().trim().equals(falseOneShrink) || parent.getAnswerB().getText().trim().equals(falseTwoShrink))
                    {
                        System.out.println("Hi, im on client side and i matched the wrong answer");
                        parent.getAnswerB().setForeground(Color.red);
                    }
                    if(parent.getAnswerC().getText().trim().equals(falseOneShrink) || parent.getAnswerC().getText().trim().equals(falseTwoShrink))
                    {
                        System.out.println("Hi, im on client side and i matched the wrong answer");
                        parent.getAnswerC().setForeground(Color.red);
                    }
                    if(parent.getAnswerD().getText().trim().equals(falseOneShrink) || parent.getAnswerD().getText().trim().equals(falseTwoShrink))
                    {
                        System.out.println("Hi, im on client side and i matched the wrong answer");
                        parent.getAnswerD().setForeground(Color.red);
                    }
                    
                }
                
                if(line.startsWith("Friend"))
                {
                    String []frnd_token = line.split(":");
                    String whoAsks = frnd_token[1];
                    String rcvdQuestion = frnd_token[2];
                    parent.getLeaderboardArea().setText("");
                    parent.getLeaderboardArea().setText("Help requested from : " + whoAsks + ":" + "\n" + rcvdQuestion);
                    //parent.getHelpFriend().setVisible(false);
                    parent.getHelpArea().setEnabled(true);
                    parent.getHelpButton().setEnabled(true);
                }
                
                if(line.startsWith("Response"))
                {
                    String [] rcvdMessageToken = line.split(":");
                    String rcvdMessage = rcvdMessageToken[1];
                    parent.getLeaderboardArea().setText("");
                    parent.getLeaderboardArea().setText(rcvdMessage);
                }
                if(line.startsWith("EndOfSet"))
                {
                    String [] token2 = parent.getLoginArea().getText().split(":");
                    String currentPlayerRole = token2[2];
                    if(currentPlayerRole.equals("admin"))
                    {
                        parent.getStartButton().setEnabled(true);
                        parent.getQuestionSet().setEnabled(true);
                    }
                    else if(currentPlayerRole.equals("contestant"))
                    {
                        parent.getQuestionSet().setEnabled(false);
                        parent.getRequestSetButton().setEnabled(true);
                    }
                    parent.getAnswerA().setEnabled(false);
                    parent.getAnswerB().setEnabled(false);
                    parent.getAnswerC().setEnabled(false);
                    parent.getAnswerD().setEnabled(false);
                    parent.getQuestionButton().setEnabled(false);
                    parent.getSubmintAnswerButton().setEnabled(false);
                    parent.getHelp5050().setEnabled(false);
                    parent.getHelpFriend().setEnabled(false);
                    parent.getHelpSwap().setEnabled(false);
                    
                    
                    parent.getHelpSwap().setVisible(true);
                    parent.getHelp5050().setVisible(true);
                    parent.getHelpFriend().setVisible(true);
                    parent.getQuestionArea().setText("");
                    
                }
                if(line.startsWith("NewLeaderboard"))
                {
                    String [] token = line.split("-");
                    int size = Integer.parseInt(token[1]);
                    String playerList = token[2];
                    String[]player_token = playerList.split(",");
                    String messageToDisplay = "";
                    for(int i=0;i<size;i++)
                    {
                        messageToDisplay+=player_token[i]+"\n";
                    }
                    System.out.println("Client side : " + playerList);
                    parent.getLeaderboardArea().setText("");
                    parent.getLeaderboardArea().setText(messageToDisplay);
                }
                if(line.startsWith("LogOutOK"))
                {
                    parent.getLogOutButton().setEnabled(false);
                    parent.getMainFrame().dispose();
                }
                if(line.startsWith("PlayerOut"))
                {
                    String [] removeToken = line.split(":");
                    String removePlayer = removeToken[1];
                    for(int i = 0 ; i < parent.getAllPlayers().getItemCount() ; i++)
                    {
                        String playerIn = parent.getAllPlayers().getItemAt(i);
                        if(removePlayer.equals(playerIn))
                        {
                            parent.getAllPlayers().removeItemAt(i);
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(RecieveMessageFromServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
