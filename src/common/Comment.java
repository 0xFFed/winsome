package common;

public class Comment {
    
    // ########## DATA ##########

    private String author;
    private String content;


    // ########## METHODS ##########

    public Comment(String author, String content) {
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
