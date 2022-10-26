package common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Post {
    
    // ########## DATA ##########

    private static int postId;
    private String title;
    private String content;
    private String author;
    private boolean isRewin;
    private ArrayList<String> likes;
    private ArrayList<Comment> comments;


    // ########## METHODS ##########

    public Post(String title, String content, String author, boolean isRewin) throws NullPointerException {
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(author, "author cannot be null");

        // generate postId
        
        this.title = title;
        this.content = content;
        this.author = author;
        this.isRewin = isRewin;
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
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
    public boolean getType() {
        return this.isRewin;
    }

    // getter
    public List<String> getLikes() {
        return this.likes;
    }

    // getter
    public List<Comment> getComments() {
        return this.comments;
    }
}
