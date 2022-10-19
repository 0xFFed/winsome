package common;

import java.util.Objects;

public class Post {
    
    // ########## DATA ##########

    private static int postId;
    private String title;
    private String content;
    private String author;
    private boolean isRewin;
    private String[] likes;
    private Comment[] comments;


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
    public String[] getLikes() {
        return this.likes;
    }

    // getter
    public Comment[] getComments() {
        return this.comments;
    }
}
