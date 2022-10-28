package common;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
    private ConcurrentHashMap<String,String> likes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,String> dislikes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Comment,String> comments = new ConcurrentHashMap<>();
    private int timesRewarded;

    private enum PostStatus {
        NEW,
        REWARDED
    }


    // ########## METHODS ##########

    public Post(String title, String content, String author, String originalAuthor, boolean rewin) throws NullPointerException {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.author = Objects.requireNonNull(author, "author cannot be null");

        this.postId = counter.get();
        if(rewin) this.originalAuthor = Objects.requireNonNull(originalAuthor);
        else this.originalAuthor = author;
        this.rewin = rewin;
        this.timesRewarded = 0;
    }


    
    /** 
     * @return int
     */
    // getter
    public int getPostId() {
        return this.postId;
    }

    
    /** 
     * @return String
     */
    // getter
    public String getTitle() {
        return this.title;
    }

    
    /** 
     * @return String
     */
    // getter
    public String getContent() {
        return this.content;
    }

    
    /** 
     * @return String
     */
    // getter
    public String getAuthor() {
        return this.author;
    }

    
    /** 
     * @return String
     */
    // getter
    public String getOriginalAuthor() {
        return this.originalAuthor;
    }

    
    /** 
     * @return boolean
     */
    // getter
    public boolean isRewin() {
        return this.rewin;
    }

    
    /** 
     * @return ArrayList<String>
     */
    // getter
    public ArrayList<String> getLikes() {
        
        ArrayList<String> likesArray = new ArrayList<>();
        this.likes.forEach((username, value) -> likesArray.add(username));

        return likesArray;
    }

    
    /** 
     * @return ArrayList<String>
     */
    // getter
    public ArrayList<String> getDislikes() {
        
        ArrayList<String> dislikesArray = new ArrayList<>();
        this.dislikes.forEach((username, value) -> dislikesArray.add(username));

        return dislikesArray;
    }

    
    /** 
     * @return ArrayList<Comment>
     */
    // getter
    public ArrayList<Comment> getComments() {

        ArrayList<Comment> commentsArray = new ArrayList<>();
        this.comments.forEach((comment, value) -> commentsArray.add(comment));

        return commentsArray;
    }

    
    /** 
     * @return HashSet<String>
     */
    // getter
    public HashSet<String> getNewLikers() {

        HashSet<String> newLikers = new HashSet<>();
        this.likes.forEach((user, status) -> {
            if(status.equals(PostStatus.NEW.name())) newLikers.add(user);
        });

        return newLikers;
    }

    
    /** 
     * @return HashSet<String>
     */
    public HashSet<String> getNewDislikers() {

        HashSet<String> newDislikers = new HashSet<>();
        this.dislikes.forEach((user, status) -> {
            if(status.equals(PostStatus.NEW.name())) newDislikers.add(user);
        });

        return newDislikers;
    }

    
    /** 
     * @return HashSet<String>
     */
    public HashSet<String> getNewCommenters() {

        HashSet<String> newCommenters = new HashSet<>();
        this.comments.forEach((comment, status) -> {
            if(status.equals(PostStatus.NEW.name())) newCommenters.add(comment.getAuthor());
        });

        return newCommenters;
    }

    
    /** 
     * @param username
     * @return Set<Comment>
     * @throws NullPointerException
     */
    public Set<Comment> getCommentsOfUser(String username) throws NullPointerException {
        Objects.requireNonNull(username);

        Set<Comment> userComments = new HashSet<>();
        this.comments.forEach((comment, status) -> {
            if(comment.getAuthor().equals(username)) userComments.add(comment);
        });

        return userComments;
    }

    
    /** 
     * @return int
     */
    public int getTimesRewarded() {
        return this.timesRewarded;
    }

    
    /** 
     * @return int
     */
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    
    /** 
     * @param value
     */
    public static void setCounter(int value) {
        counter.set(value);
    }


    
    
    // ########## UTILITY FUNCTIONS ##########

    /** 
     * @param user
     * @return boolean
     * @throws NullPointerException
     */
    // adds a like from the user
    public boolean like(User user) throws NullPointerException {
        Objects.requireNonNull(user);

        if(this.likes.containsKey(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.likes.putIfAbsent(user.getUsername(), PostStatus.NEW.name());
            return true;
        }
    }

    
    /** 
     * @param user
     * @return boolean
     * @throws NullPointerException
     */
    // adds a dislike from the user
    public boolean dislike(User user) throws NullPointerException {
        Objects.requireNonNull(user);

        if(this.likes.containsKey(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.dislikes.putIfAbsent(user.getUsername(), PostStatus.NEW.name());
            return true;
        }
    }

    
    /** 
     * @param comment
     * @throws NullPointerException
     */
    // adds a comment from the user
    public void addComment(Comment comment) throws NullPointerException {
        Objects.requireNonNull(comment);
        
        this.comments.putIfAbsent(comment, PostStatus.NEW.name());
    }

    // registers a reward cycle
    public synchronized void registerRewardCycle() {
        this.timesRewarded++;
    }
}
