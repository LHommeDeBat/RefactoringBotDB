package de.BA.refactoringBot.model.botIssue;

public class BotIssue {

	private String refactoringOperation;
	private String filePath;
	private Integer line;
	private String commentServiceID;
	private String renameString;
	
	public String getRefactoringOperation() {
		return refactoringOperation;
	}
	
	public void setRefactoringOperation(String refactoringOperation) {
		this.refactoringOperation = refactoringOperation;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public Integer getLine() {
		return line;
	}
	
	public void setLine(Integer line) {
		this.line = line;
	}

	public String getCommentServiceID() {
		return commentServiceID;
	}

	public void setCommentServiceID(String commentServiceID) {
		this.commentServiceID = commentServiceID;
	}

	public String getRenameString() {
		return renameString;
	}

	public void setRenameString(String renameString) {
		this.renameString = renameString;
	}
	
}
