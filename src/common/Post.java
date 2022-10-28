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

    // getter
    public HashSet<String> getNewLikers() {

        HashSet<String> newLikers = new HashSet<>();
        this.likes.forEach((user, status) -> {
            if(status.equals(PostStatus.NEW.name())) newLikers.add(user);
        });

        return newLikers;
    }

    // getter
    public HashSet<String> getNewDislikers() {

        HashSet<String> newDislikers = new HashSet<>();
        this.dislikes.forEach((user, status) -> {
            if(status.equals(PostStatus.NEW.name())) newDislikers.add(user);
        });

        return newDislikers;
    }

    // getter
    public HashSet<String> getNewCommenters() {

        HashSet<String> newCommenters = new HashSet<>();
        this.comments.forEach((comment, status) -> {
            if(status.equals(PostStatus.NEW.name())) newCommenters.add(comment.getAuthor());
        });

        return newCommenters;
    }

    // getter
    public Set<Comment> getCommentsOfUser(String username) {

        Set<Comment> userComments = new HashSet<>();
        this.comments.forEach((comment, status) -> {
            if(comment.getAuthor().equals(username)) userComments.add(comment);
        });

        return userComments;
    }

    // getter
    public int getTimesRewarded() {
        return this.timesRewarded;
    }

    // getter
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    // setter
    public static void setCounter(int value) {
        counter.set(value);
    }


    // ########## UTILITY FUNCTIONS ##########

    // adds a like from the user
    public boolean like(User user) {
        if(this.likes.containsKey(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.likes.putIfAbsent(user.getUsername(), PostStatus.NEW.name());
            return true;
        }
    }

    // adds a dislike from the user
    public boolean dislike(User user) {
        if(this.likes.containsKey(user.getUsername()) || this.dislikes.contains(user.getUsername())) {
            return false;
        }
        else {
            this.dislikes.putIfAbsent(user.getUsername(), PostStatus.NEW.name());
            return true;
        }
    }

    // adds a comment from the user
    public void addComment(Comment comment) {
        this.comments.putIfAbsent(comment, PostStatus.NEW.name());
    }

    // registers a reward cycle
    public synchronized void registerRewardCycle() {
        this.timesRewarded++;
    }
}
