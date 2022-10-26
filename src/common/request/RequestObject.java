package common.request;

import java.util.Objects;

import common.User;
import common.Post;
import common.Comment;

public class RequestObject {

    private String token;
    private String command;
    private String username;
    private String password;
    private Post post;
    private Comment comment;
    private boolean vote;

    
    public RequestObject(String token, String command, String username, String password, Post post, Comment comment, boolean vote) throws NullPointerException {
        if(Objects.isNull(token)) this.token =""; else this.token = token;
        this.command = Objects.requireNonNull(command, "a request's command cannot be null");
        if(Objects.isNull(username)) this.username = ""; else this.username = username;
        if(Objects.isNull(password)) this.password = ""; else this.password = password;
        if(Objects.isNull(post)) this.post = new Post("", "", "", "", false); else this.post = post;
        if(Objects.isNull(comment)) this.comment = new Comment("", ""); else this.comment = comment;
        this.vote= vote;
    }

    public String getToken() {
        return this.token;
    }

    public String getCommand() {
        return this.command;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public Post getPost() {
        return this.post;
    }

    public Comment getComment() {
        return this.comment;
    }

    public boolean getVote() {
        return this.vote;
    }
}
