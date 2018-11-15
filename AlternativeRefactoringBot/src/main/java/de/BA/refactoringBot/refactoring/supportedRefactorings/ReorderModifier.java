package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.sonarQube.Issue;

/**
 * This class is used to execute the reorder modifier refactoring.
 * 
 * The LexicalPreservationPrinter is not used here, because there are Problems
 * when reordering the Modifiers. The Printer expects the String, that was there
 * before the Refactoring was done and therefore throws an exception. It also
 * has the same problem as the remove of the unused variable.
 *
 * @author Timo Pfaff
 */
@Component
public class ReorderModifier extends ModifierVisitor<Void> {

	@Autowired
	BotConfiguration botConfig;

	/**
	 * Diese Methode führt das Refactoring durch und gibt eine passende
	 * Git-Commit-Nachricht zurück.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return commitMessage
	 * @throws FileNotFoundException
	 */
	public String performRefactoring(Issue issue, GitConfiguration gitConfig) throws FileNotFoundException {
		// Bereite Refactoringdaten vor
		String project = issue.getProject();
		String component = issue.getComponent();
		String path = component.substring(project.length() + 1, component.length());

		// Lese Datei aus
		FileInputStream in = new FileInputStream(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId()
				+ "/" + gitConfig.getProjectRootFolder() + "/" + path);
		CompilationUnit compilationUnit = JavaParser.parse(in);

		// Finde Code in Datei zum Refactoren
		visit(compilationUnit, null);

		// Schreibe Änderungen in Datei
		PrintWriter out = new PrintWriter(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId() + "/"
				+ gitConfig.getProjectRootFolder() + "/" + path);
		out.println(compilationUnit.toString());
		out.close();

		// Gebe passende Commit-Nachricht zurück
		return "Reordered modifier";
	}

	/**
	 * Diese Methode sortiert alle Modifier.
	 */
	public Node visit(FieldDeclaration declarator, Void args) {
		EnumSet<Modifier> modifiers = declarator.getModifiers();
		declarator.setModifiers(modifiers);
		return declarator;

	}
}
