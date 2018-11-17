package de.BA.refactoringBot.controller.main;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequest;
import de.BA.refactoringBot.model.outputModel.myPullRequest.BotPullRequests;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;

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
	 * Diese Methode checkt, ob die maximale Anzahl an offenen PullRequests des Bots
	 * in dem entsprechenden Repository erreicht wurde.
	 * 
	 * @param requests
	 * @param gitConfig
	 * @throws Exception
	 */
	public void checkAmountOfBotRequests(BotPullRequests requests, GitConfiguration gitConfig) throws Exception {

		// Initiiere Zähler
		int counter = 0;
		// Gehe alle Requests durch
		for (BotPullRequest request : requests.getAllPullRequests()) {
			// Falls Request dem Bot gehört
			if (request.getCreatorName().equals(gitConfig.getBotName())) {
				counter++;
			}
		}

		// Prüfe ob Maximum an Requests erreicht oder überschritten
		if (counter >= gitConfig.getMaxAmountRequests()) {
			throw new Exception("Maximale Anzahl an Requests erreicht bzw. überschritten." + "(Maximum = "
					+ gitConfig.getMaxAmountRequests() + "; Aktuell = " + counter + " Requests des Bots offen)");
		}
	}

	/**
	 * Diese Methode erstellt das Objekt, welches das durchgeführte Refactoring
	 * beschreibt.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return refactoredIssue
	 */
	public RefactoredIssue buildRefactoredIssue(BotIssue issue, GitConfiguration gitConfig) {
		// Erstelle Objekt
		RefactoredIssue refactoredIssue = new RefactoredIssue();

		// Erstelle Zeitstempel
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = new Date();
		String date = sdf.format(now);

		// Fülle Objekt
		refactoredIssue.setCommentServiceID(issue.getCommentServiceID());
		refactoredIssue.setRepoName(gitConfig.getRepoName());
		refactoredIssue.setRepoOwner(gitConfig.getRepoOwner());
		refactoredIssue.setRepoService(gitConfig.getRepoService());
		refactoredIssue.setDateOfRefactoring(date);
		refactoredIssue.setAnalysisService(gitConfig.getAnalysisService());
		refactoredIssue.setAnalysisServiceProjectKey(gitConfig.getAnalysisServiceProjectKey());
		refactoredIssue.setRefactoringOperation(issue.getRefactoringOperation());

		return refactoredIssue;
	}
	
	/**
	 * Diese Methode erstellt das Objekt, welches das durchgeführte Refactoring
	 * beschreibt.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return refactoredIssue
	 */
	public RefactoredIssue buildRefactoredIssue(BotPullRequest request, BotPullRequestComment comment, GitConfiguration gitConfig) {
		// Erstelle Objekt
		RefactoredIssue refactoredIssue = new RefactoredIssue();

		// Erstelle Zeitstempel
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = new Date();
		String date = sdf.format(now);

		// Fülle Objekt
		refactoredIssue.setCommentServiceID(comment.getCommentID().toString());
		refactoredIssue.setRepoName(gitConfig.getRepoName());
		refactoredIssue.setRepoOwner(gitConfig.getRepoOwner());
		refactoredIssue.setRepoService(gitConfig.getRepoService());
		refactoredIssue.setDateOfRefactoring(date);

		if (gitConfig.getAnalysisService() != null) {
			refactoredIssue.setAnalysisService(gitConfig.getAnalysisService());
		}

		if(gitConfig.getAnalysisServiceProjectKey() != null) {
			refactoredIssue.setAnalysisServiceProjectKey(gitConfig.getAnalysisServiceProjectKey());
		}
		
		return refactoredIssue;
	}
}
