package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;
import de.BA.refactoringBot.model.sonarQube.Issue;

/**
 * This class is used for executing the add override annotation refactoring.
 *
 * @author Timo Pfaff
 */
@Component
public class AddOverrideAnnotation extends VoidVisitorAdapter<Object> {

	Integer line;
	String methodName;
	CompilationUnit compilationUnit;

	@Autowired
	BotConfiguration botConfig;

	/**
	 * Diese Methode besucht die Methode, welche die Annotation benötigt und fügt
	 * diese dort hinzu.
	 * 
	 * @param declaration
	 * @param line
	 */
	public void visit(MethodDeclaration declaration, Object arg) {
	
		if (line == declaration.getName().getBegin().get().line) {
			methodName = declaration.getNameAsString();
			declaration.addMarkerAnnotation("Override");
		}
	}

	/**
	 * Diese Methode führt das Refactoring durch.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return
	 * @throws FileNotFoundException
	 */
	public RefactoredIssue performRefactoring(Issue issue, GitConfiguration gitConfig) throws FileNotFoundException {
		String project = issue.getProject();
		String component = issue.getComponent();
		String path = component.substring(project.length() + 1, component.length());
		line = issue.getLine();
		FileInputStream in = new FileInputStream(
				botConfig.getBotRefactoringDirectory() + gitConfig.getProjectRootFolder() + "/" + path);
		compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(in));
		visit(compilationUnit, null);
		
		System.out.println(LexicalPreservingPrinter.print(compilationUnit));

		/**
		 * Actually apply changes to the File
		 */

		PrintWriter out = new PrintWriter(
				botConfig.getBotRefactoringDirectory() + gitConfig.getProjectRootFolder() + "/" + path);
		out.println(LexicalPreservingPrinter.print(compilationUnit));
		out.close();

		return buildRefactoredIssue(issue, gitConfig);
	}

	/**
	 * Diese Methode erstellt das Objekt, welches das durchgeführte Refactoring
	 * beschreibt.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return refactoredIssue
	 */
	public RefactoredIssue buildRefactoredIssue(Issue issue, GitConfiguration gitConfig) {
		// Erstelle Objekt
		RefactoredIssue refactoredIssue = new RefactoredIssue();

		// Erstelle Zeitstempel
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = new Date();
		String date = sdf.format(now);

		// Fülle Objekt
		refactoredIssue.setRepoName(gitConfig.getRepoName());
		refactoredIssue.setRepoOwner(gitConfig.getRepoOwner());
		refactoredIssue.setRepoService(gitConfig.getRepoService());
		refactoredIssue.setDateOfRefactoring(date);

		// TODO: dynamischer Branch für SonarCube
		refactoredIssue.setRepoBranch("master");
		refactoredIssue.setCommitMessage(getCommitMessage());

		refactoredIssue.setSonarCubeProjectKey(gitConfig.getSonarCubeProjectKey());
		refactoredIssue.setSonarCubeIssueRule(issue.getRule());
		refactoredIssue.setKindOfRefactoring(getRefactoringName());

		return refactoredIssue;
	}

	/**
	 * Diese Methode gibt die Art des Refactorings zurück.
	 * 
	 * @return refactoringName
	 */
	public String getRefactoringName() {
		return "Add Override Annotation";
	}

	/**
	 * Diese Methode gibt die Commit-Nachricht zurück.
	 * 
	 * @return commitMessage
	 */
	public String getCommitMessage() {
		return "Added override annotation to method " + methodName;
	}

}
