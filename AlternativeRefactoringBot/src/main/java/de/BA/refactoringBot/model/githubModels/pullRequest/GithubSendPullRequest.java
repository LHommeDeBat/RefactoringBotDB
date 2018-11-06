package de.BA.refactoringBot.model.githubModels.pullRequest;

public class GithubSendPullRequest {

	private String title;
	private String state;
	private String base;
	private String body;
	private boolean maintainer_can_modify;
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getBase() {
		return base;
	}
	
	public void setBase(String base) {
		this.base = base;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public boolean isMaintainer_can_modify() {
		return maintainer_can_modify;
	}
	
	public void setMaintainer_can_modify(boolean maintainer_can_modify) {
		this.maintainer_can_modify = maintainer_can_modify;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
}
