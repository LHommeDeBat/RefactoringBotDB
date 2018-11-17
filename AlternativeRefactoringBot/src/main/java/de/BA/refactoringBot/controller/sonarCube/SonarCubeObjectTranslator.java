package de.BA.refactoringBot.controller.sonarCube;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.sonarQube.SonarCubeIssues;
import de.BA.refactoringBot.model.sonarQube.SonarIssue;

/**
 * Diese Klasse übersetzt das SonarCube-Objekte in vom Bot verstandene Objekte.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class SonarCubeObjectTranslator {

	/**
	 * Diese Methode übersetzt einen SonarCube-Issue in einem dem Bot bekannten
	 * Bot-Issue.
	 * 
	 * @param issue
	 * @return botIssue
	 */
	public List<BotIssue> translateSonarIssue(SonarCubeIssues issues, GitConfiguration gitConfig) {
		// Initiiere objekt
		List<BotIssue> botIssues = new ArrayList<BotIssue>();

		for (SonarIssue issue : issues.getIssues()) {
			// Initiiere Objekt
			BotIssue botIssue = new BotIssue();

			// Erstelle Pfad
			String project = issue.getProject();
			String component = issue.getComponent();
			String path = component.substring(project.length() + 1, component.length());
			path = gitConfig.getProjectRootFolder() + "/" + path;
			botIssue.setFilePath(path);

			// Fülle Objekt
			botIssue.setLine(issue.getLine());
			botIssue.setCommentServiceID(issue.getKey());

			// Übersetze Regel
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
			
			// Füge Objekt zur Ergebnismenge hinzu
			botIssues.add(botIssue);
		}

		return botIssues;
	}
}
