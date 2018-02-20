package cz.zcu.kiv.nlp.priznej;

import java.io.Serializable;

/**
 * Simple class which represents confession.
 * Follows Messenger design pattern.
 */
public class Confession implements Serializable {

    /**
     * Confession text.
     */
    private String text;

    /**
     * Number of confession upvotes.
     */
    private int upvotes;

    /**
     * Number of confession downvotes.
     */
    private int downvotes;

    /**
     * Number of confession comments.
     */
    private int commentCount;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public int getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(int downvotes) {
        this.downvotes = downvotes;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    @Override
    public String toString() {
        return "Confession{" +
                "text='" + text + '\'' +
                ", upvotes=" + upvotes +
                ", downvotes=" + downvotes +
                ", commentCount=" + commentCount +
                '}';
    }
}
