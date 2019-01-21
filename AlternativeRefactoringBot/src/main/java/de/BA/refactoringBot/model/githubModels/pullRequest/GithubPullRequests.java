package de.BA.refactoringBot.model.githubModels.pullRequest;

import java.util.ArrayList;
import java.util.List;

public class GithubPullRequests {

	List<PullRequest> allPullRequests = new ArrayList<PullRequest>();

	public List<PullRequest> getAllPullRequests() {
		return allPullRequests;
	}

	public void setAllPullRequests(List<PullRequest> allPullRequests) {
		this.allPullRequests = allPullRequests;
	}
	
	public void addPullRequest(PullRequest pullRequest) {
		this.allPullRequests.add(pullRequest);
	}
	
	public void removePullRequest(PullRequest pullRequest) {
		this.allPullRequests.remove(pullRequest);
	}
	
}
