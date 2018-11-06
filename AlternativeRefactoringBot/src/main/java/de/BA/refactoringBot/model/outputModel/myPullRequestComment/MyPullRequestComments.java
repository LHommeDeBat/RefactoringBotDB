package de.BA.refactoringBot.model.outputModel.myPullRequestComment;

import java.util.ArrayList;
import java.util.List;

public class MyPullRequestComments {
	List<MyPullRequestComment> comments = new ArrayList<MyPullRequestComment>();

	public List<MyPullRequestComment> getComments() {
		return comments;
	}

	public void setComments(List<MyPullRequestComment> comments) {
		this.comments = comments;
	}
	
	public void addComment(MyPullRequestComment comment) {
		this.comments.add(comment);
	}
	
	public void removeComment(MyPullRequestComment comment) {
		this.comments.remove(comment);
	}
}
