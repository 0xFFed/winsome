package common;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<String,Boolean> likes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,Boolean> dislikes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Comment,Boolean> comments = new ConcurrentHashMap<>();


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
    public ArrayList<String> getLikes() {
        
        ArrayList<String> likesArray = new ArrayList<>();
        this.likes.forEach((username, value) -> likesArray.add(username));

        return likesArray;
    }

    // getter
    public ArrayList<String> getDislikes() {
        
        ArrayList<String> dislikesArray = new ArrayList<>();
        this.dislikes.forEach((username, value) -> dislikesArray.add(username));

        return dislikesArray;
    }

    // getter
    public ArrayList<Comment> getComments() {

        ArrayList<Comment> commentsArray = new ArrayList<>();
        this.comments.forEach((comment, value) -> commentsArray.add(comment));

        return commentsArray;
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
            this.likes.putIfAbsent(user.getUsername(), Boolean.FALSE);
            return true;
        }
    }

    // setter
    public synchronized boolean dislike(User user) {
        if(this.likes.contains(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.dislikes.putIfAbsent(user.getUsername(), Boolean.FALSE);
            return true;
        }
    }

    // setter
    public synchronized void addComment(Comment comment) {
        this.comments.putIfAbsent(comment, Boolean.FALSE);
    }
}
