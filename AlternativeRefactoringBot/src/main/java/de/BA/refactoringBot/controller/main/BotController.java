package de.BA.refactoringBot.controller.main;

import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;

@Component
public class BotController {

	/**
	 * Diese Methode schaut ob ein Kommentar für den BOT bestimmt ist.
	 * 
	 * @param comment
	 * @return boolean
	 */
	public boolean checkIfCommentForBot(BotPullRequestComment comment) {
		// Splitte Kommentar an den Leerzeichen
		String[] splitedComment = comment.getCommentBody().split(" ");
		// Falls erstes Element = BOT -> Kommentar an BOT gerichtet
		if (splitedComment[0].equals("BOT")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Diese Methode aktualisiert die Konfiguration mit den Eigenschaften des
	 * erstellten Forks bei der Erstellung einer Konfiguration.
	 * 
	 * @param config
	 * @return config
	 */
	public GitConfiguration updateConfiguration(GitConfiguration config) {
		// Aktualisiere fehlende Daten
		config.setForkApiLink("https://api.github.com/repos/" + config.getBotName() + "/" + config.getRepoName());
		config.setForkGitLink("https://github.com/" + config.getBotName() + "/" + config.getRepoName() + ".git");
		// Gebe Konfiguration zurück
		return config;
	}
}
