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
        if(Objects.isNull(author)) this.author = ""; else this.author = author;
        if(Objects.isNull(content)) this.content = ""; else this.content = content;
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
        return this.author+": "+this.content;
    }
}
