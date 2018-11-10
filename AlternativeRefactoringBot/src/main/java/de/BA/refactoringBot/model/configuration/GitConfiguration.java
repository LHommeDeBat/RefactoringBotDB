package de.BA.refactoringBot.model.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GIT_CONFIGURATIONS")
public class GitConfiguration {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String repoName;
	private String repoOwner;
	private String repoApiLink;
	private String repoGitLink;
	private String repoService;
	
	private String botName;
	private String botPassword;
	private String botToken;
	private String forkApiLink;
	private String forkGitLink;
	private String sonarCubeProjectKey;
	private Integer maxAmountRequests;
	
	public Long getId() {
		return id;
	}
	
	public String getRepoName() {
		return repoName;
	}
	
	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}
	
	public String getRepoApiLink() {
		return repoApiLink;
	}
	
	public void setRepoApiLink(String repoApiLink) {
		this.repoApiLink = repoApiLink;
	}

	public String getRepoGitLink() {
		return repoGitLink;
	}

	public void setRepoGitLink(String repoGitLink) {
		this.repoGitLink = repoGitLink;
	}

	public String getRepoOwner() {
		return repoOwner;
	}

	public void setRepoOwner(String repoOwner) {
		this.repoOwner = repoOwner;
	}

	public String getRepoService() {
		return repoService;
	}

	public void setRepoService(String repoService) {
		this.repoService = repoService;
	}

	public String getBotName() {
		return botName;
	}

	public void setBotName(String botName) {
		this.botName = botName;
	}

	public String getBotPassword() {
		return botPassword;
	}

	public void setBotPassword(String botPassword) {
		this.botPassword = botPassword;
	}

	public String getBotToken() {
		return botToken;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public String getForkApiLink() {
		return forkApiLink;
	}

	public void setForkApiLink(String forkApiLink) {
		this.forkApiLink = forkApiLink;
	}

	public String getForkGitLink() {
		return forkGitLink;
	}

	public void setForkGitLink(String forkGitLink) {
		this.forkGitLink = forkGitLink;
	}

	public String getSonarCubeProjectKey() {
		return sonarCubeProjectKey;
	}

	public void setSonarCubeProjectKey(String sonarCubeProjectKey) {
		this.sonarCubeProjectKey = sonarCubeProjectKey;
	}

	public Integer getMaxAmountRequests() {
		return maxAmountRequests;
	}

	public void setMaxAmountRequests(Integer maxAmountRequests) {
		this.maxAmountRequests = maxAmountRequests;
	}
	
}
