package common;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
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
    private boolean rewin;
    private ArrayList<String> likes = new ArrayList<>();
    private ArrayList<String> dislikes = new ArrayList<>();
    private ArrayList<Comment> comments = new ArrayList<>();


    // ########## METHODS ##########

    public Post(String title, String content, String author, String originalAuthor, boolean rewin) throws NullPointerException {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.author = Objects.requireNonNull(author, "author cannot be null");

        this.postId = counter.get();
        if(rewin) this.originalAuthor = Objects.requireNonNull(originalAuthor);
        else this.originalAuthor = author;
        this.rewin = rewin;
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
    public boolean isRewin() {
        return this.rewin;
    }

    // getter
    public synchronized ArrayList<String> getLikes() {
        return this.likes;
    }

    // getter
    public synchronized ArrayList<String> getDislikes() {
        return this.dislikes;
    }

    // getter
    public synchronized ArrayList<Comment> getComments() {
        return this.comments;
    }

    // getter/setter
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    // setter
    public static synchronized void setCounter(int value) {
        counter.set(value);
    }

    // setter
    public synchronized boolean like(User user) {
        if(this.likes.contains(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.likes.add(user.getUsername());
            return true;
        }
    }

    // setter
    public synchronized void addComment(Comment comment) {
        this.comments.add(comment);
    }

    // setter
    public synchronized boolean dislike(User user) {
        if(this.likes.contains(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.dislikes.add(user.getUsername());
            return true;
        }
    }
}
