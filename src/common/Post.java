package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Post implements Serializable {

    private static final long serialVersionUID = 6837L;
    
    // ########## DATA ##########

    private static AtomicInteger counter = new AtomicInteger();
    private int postId;
    private String title;
    private String content;
    private String author;
    private String originalAuthor;
    private boolean isRewin;
    private ArrayList<String> likes;
    private ArrayList<Comment> comments;


    // ########## METHODS ##########

    public Post(String title, String content, String author, String originalAuthor, boolean isRewin) throws NullPointerException {
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(author, "author cannot be null");

        this.postId = counter.get();
        this.title = title;
        this.content = content;
        this.author = author;
        if(isRewin) this.originalAuthor = Objects.requireNonNull(originalAuthor);
        else this.originalAuthor = author;
        this.isRewin = isRewin;
    }


    // getter
    public int getPostId() {
        return this.postId;
    }

    // getter
    public String getTitle() {
        return this.title;
    }

    // getter
    public String getContent() {
        return this.content;
    }

    // getter
    public String getAuthor() {
        return this.author;
    }

    // getter
    public String getOriginalAuthor() {
        return this.originalAuthor;
    }

    // getter
    public boolean getType() {
        return this.isRewin;
    }

    // getter
    public ArrayList<String> getLikes() {
        return this.likes;
    }

    // getter
    public ArrayList<Comment> getComments() {
        return this.comments;
    }

    // getter/setter
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    // setter
    public static void setCounter(int value) {
        counter.set(value);
    }
}
