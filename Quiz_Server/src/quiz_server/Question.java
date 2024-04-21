package quiz_server;



public class Question {
    private String text;
    private Answer answerA;
    private Answer answerB;
    private Answer answerC;
    private Answer answerD;
    
    public Question(String text,Answer A,Answer B,Answer C,Answer D)
    {
        this.text = text;
        this.answerA = A;
        this.answerB = B;
        this.answerC = C;
        this.answerD = D;
    }

    public Answer getAnswerA() {
        return answerA;
    }

    public Answer getAnswerB() {
        return answerB;
    }

    public Answer getAnswerC() {
        return answerC;
    }

    public Answer getAnswerD() {
        return answerD;
    }

    
    public String getText() {
        return text;
    }    
    
    @Override
    public String toString()
    {
        return "Text: " + this.text + this.answerA  + this.answerB + this.answerC + this.answerD; 
    }
    
    
}
