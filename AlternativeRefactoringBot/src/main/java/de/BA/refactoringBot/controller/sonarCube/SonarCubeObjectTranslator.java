package de.BA.refactoringBot.controller.sonarCube;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.sonarQube.SonarCubeIssues;
import de.BA.refactoringBot.model.sonarQube.SonarIssue;

/**
 * This class translates SonarCube Objects into Bot-Objects.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class SonarCubeObjectTranslator {

	/**
	 * This method translates all SonarCubeIssues to BotIssues.
	 * 
	 * @param issue
	 * @return botIssue
	 */
	public List<BotIssue> translateSonarIssue(SonarCubeIssues issues, GitConfiguration gitConfig) {
		// Create empty list of bot issues
		List<BotIssue> botIssues = new ArrayList<BotIssue>();

		// Iterate all SonarCube issues
		for (SonarIssue issue : issues.getIssues()) {
			// Create bot issue
			BotIssue botIssue = new BotIssue();

			// Create filepath
			String project = issue.getProject();
			String component = issue.getComponent();
			String path = component.substring(project.length() + 1, component.length());
			path = gitConfig.getProjectRootFolder() + "/" + path;
			botIssue.setFilePath(path);

			// Fill object
			botIssue.setLine(issue.getLine());
			botIssue.setCommentServiceID(issue.getKey());

			// Translate SonarCube rule
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

			// Add bot issue to list
			botIssues.add(botIssue);
		}

		return botIssues;
	}
}
