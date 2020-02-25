package cz.zcu.kiv.nlp.reddit;

/**
 * One reddit comment.
 */
public class Comment {

    /**
     * Commenter's username.
     */
    private String username;

    /**
     * Comment text.
     */
    private String text;

    /**
     * Comment score.
     */
    private int score;

    /**
     * Timestamp.
     */
    private String timestamp;

    public Comment() {
    }

    public Comment(String username, String text, int score) {
        this.username = username;
        this.text = text;
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "username='" + username + '\'' +
                ", text='" + text + '\'' +
                ", score=" + score +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
