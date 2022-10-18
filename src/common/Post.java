package common;

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

    public Post(String title, String content, String author, boolean isRewin) {
        // generate postId
        this.title = title;
        this.content = content;
        this.author = author;
        this.isRewin = isRewin;
    }


    // getter
    public String getTitle() {
        return this.title
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
    public String getType() {
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
