package de.BA.refactoringBot.model.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class GitConfiguration {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String repoName;
	private String repoOwner;
	private String repoApiLink;
	private String repoGitLink;
	private String repoService;
	
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
	
}
