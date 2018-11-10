package de.BA.refactoringBot.model.refactoredIssue;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "REFACTORED_ISSUES")
public class RefactoredIssue {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String repoName;
	private String repoOwner;
	private String repoService;
	private String dateOfRefactoring;
	private String sonarCubeProjectKey;
	private String sonarCubeIssueKey;
	private String kindOfRefactoring;
	private String commitMessage;
	private String repoBranch;
	
	public Long getId() {
		return id;
	}
	
	public String getRepoName() {
		return repoName;
	}
	
	public void setRepoName(String repoName) {
		this.repoName = repoName;
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
	
	public String getDateOfRefactoring() {
		return dateOfRefactoring;
	}
	
	public void setDateOfRefactoring(String dateOfRefactoring) {
		this.dateOfRefactoring = dateOfRefactoring;
	}
	
	public String getSonarCubeProjectKey() {
		return sonarCubeProjectKey;
	}
	
	public void setSonarCubeProjectKey(String sonarCubeProjectKey) {
		this.sonarCubeProjectKey = sonarCubeProjectKey;
	}
	
	public String getSonarCubeIssueKey() {
		return sonarCubeIssueKey;
	}
	
	public void setSonarCubeIssueKey(String sonarCubeIssueKey) {
		this.sonarCubeIssueKey = sonarCubeIssueKey;
	}
	
	public String getKindOfRefactoring() {
		return kindOfRefactoring;
	}
	
	public void setKindOfRefactoring(String kindOfRefactoring) {
		this.kindOfRefactoring = kindOfRefactoring;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public String getRepoBranch() {
		return repoBranch;
	}

	public void setRepoBranch(String repoBranch) {
		this.repoBranch = repoBranch;
	}
	
}