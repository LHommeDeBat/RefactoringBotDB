package de.BA.refactoringBot.controller.sonarCube;

import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.sonarQube.SonarIssue;

/**
 * Diese Klasse übersetzt das SonarCube-Objekte in vom Bot verstandene Objekte.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class ObjectTranslator {

	/**
	 * Diese Methode übersetzt einen SonarCube-Issue in einem dem Bot bekannten
	 * Bot-Issue.
	 * 
	 * @param issue
	 * @return botIssue
	 */
	public BotIssue translateSonarIssue(SonarIssue issue, GitConfiguration gitConfig) {
		// Initiiere Objekt
		BotIssue botIssue = new BotIssue();

		// Erstelle Pfad
		String project = issue.getProject();
		String component = issue.getComponent();
		String path = component.substring(project.length() + 1, component.length());
		path = gitConfig.getProjectRootFolder() + "/" + path;
		botIssue.setFilePath(path);

		botIssue.setLine(issue.getLine());
		botIssue.setCommentServiceID(issue.getKey());

		switch (issue.getRule()) {
		case "squid:S1161":
			botIssue.setRefactoringOperation("Add Override Annotation");
			break;
		case "squid:ModifiersOrderCheck":
			botIssue.setRefactoringOperation("Reorder Modifier");
			break;
		default:
			botIssue.setRefactoringOperation("Unknown Refactoring");
			break;
		}

		return botIssue;
	}
}
