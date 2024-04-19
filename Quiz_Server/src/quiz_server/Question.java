
package quiz_server;

import java.util.HashMap;


public class Question {
    private String text;
    private HashMap <Boolean,String> answers;    
    
    public Question(String text,HashMap<Boolean,String> answers)
    {
        this.text = text;
        this.answers = answers;
    }
    
    @Override
    public String toString()
    {
        return "Text: " + this.text + "Answers: " + answers.toString();
    }
    
}
