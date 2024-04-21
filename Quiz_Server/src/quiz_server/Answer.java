package quiz_server;

public class Answer {
    private String answerText;
    private boolean correct;
    
    public  Answer(String text , boolean correct)
    {
        this.answerText = text;
        this.correct = correct;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
    
    @Override
    public String toString()
    {
        return "Answer Text: " + this.answerText + " Validity : " + this.correct; 
    }
    
    
    
}
