package de.BA.refactoringBot.controller.main;

import org.springframework.stereotype.Component;

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
}
