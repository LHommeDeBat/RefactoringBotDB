package de.BA.refactoringBot.model.outputModel.myPullRequest;

import java.util.ArrayList;
import java.util.List;

public class MyPullRequests {

	List<MyPullRequest> allPullRequests = new ArrayList<MyPullRequest>();

	public List<MyPullRequest> getAllPullRequests() {
		return allPullRequests;
	}

	public void setAllPullRequests(List<MyPullRequest> allPullRequests) {
		this.allPullRequests = allPullRequests;
	}
	
	public void addPullRequest(MyPullRequest pullRequest) {
		this.allPullRequests.add(pullRequest);
	}
	
	public void removePullRequest(MyPullRequest pullRequest) {
		this.allPullRequests.remove(pullRequest);
	}
	
}
