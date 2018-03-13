package cz.zcu.kiv.nlp.priznej;

import java.io.Serializable;
import java.util.Date;

/**
 * Simple class which represents confession.
 * Follows Messenger design pattern.
 */
public class Confession implements Serializable {

    /**
     * Id of a post (so it can be accessed later).
     */
    private String id;

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

    /**
     * When the confession was posted.
     */
    private Date datePosted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Date getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(Date datePosted) {
        this.datePosted = datePosted;
    }

    @Override
    public String toString() {
        return "Confession{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", upvotes=" + upvotes +
                ", downvotes=" + downvotes +
                ", commentCount=" + commentCount +
                '}';
    }
}
