package de.BA.refactoringBot.model.javaparser;

import java.util.ArrayList;
import java.util.List;

public class ParserRefactoringCollection {

	private List<ParserRefactoring> refactorings = new ArrayList<ParserRefactoring>();
	private List<String> doneFiles = new ArrayList<String>();

	public List<ParserRefactoring> getRefactoring() {
		return refactorings;
	}

	public void setRefactoring(List<ParserRefactoring> refactorings) {
		this.refactorings = refactorings;
	}
	
	public void addRefactoring(ParserRefactoring refactoring) {
		this.refactorings.add(refactoring);
	}
	
	public void addRefactorings(List<ParserRefactoring> refactorings) {
		this.refactorings.addAll(refactorings);
	}

	public List<String> getDoneFiles() {
		return doneFiles;
	}

	public void setDoneFiles(List<String> doneFiles) {
		this.doneFiles = doneFiles;
	}
	
	public void addDoneFile(String file) {
		this.doneFiles.add(file);
	}
	
	public void addDoneFiles(List<String> files) {
		this.doneFiles.addAll(files);
	}
}
