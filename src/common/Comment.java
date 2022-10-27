package common;

import java.io.Serializable;
import java.util.Objects;

public class Comment implements Serializable {

    private static final long serialVersionUID = 7724L;
    
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


    @Override
    public String toString() {
        return "Comment: [Author: "+this.author+"\nContent: "+this.content+"]";
    }
}
