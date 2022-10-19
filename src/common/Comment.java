package common;

import java.util.Objects;

public class Comment {
    
    // ########## DATA ##########

    private String author;
    private String content;


    // ########## METHODS ##########

    public Comment(String author, String content) throws NullPointerException {
        Objects.requireNonNull(author, "author cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        this.author = author;
        this.content = content;
    }


    // getter
    public String getAuthor() {
        return this.author;
    }

    // getter
    public String getContent() {
        return this.content;
    }
}
