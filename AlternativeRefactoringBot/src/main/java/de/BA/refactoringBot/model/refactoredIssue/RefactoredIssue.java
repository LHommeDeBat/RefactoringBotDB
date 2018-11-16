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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long issueId;
	
	private String commentServiceID;
	private String repoName;
	private String repoOwner;
	private String repoService;
	private String dateOfRefactoring;
	private String sonarCubeProjectKey;
	private String refactoringOperation;
	private String repoBranch;
	
	public Long getIssueId() {
		return issueId;
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
	
	public String getRefactoringOperation() {
		return refactoringOperation;
	}
	
	public void setRefactoringOperation(String refactoringOperation) {
		this.refactoringOperation = refactoringOperation;
	}

	public String getRepoBranch() {
		return repoBranch;
	}

	public void setRepoBranch(String repoBranch) {
		this.repoBranch = repoBranch;
	}

	public String getCommentServiceID() {
		return commentServiceID;
	}

	public void setCommentServiceID(String commentServiceID) {
		this.commentServiceID = commentServiceID;
	}
	
}
