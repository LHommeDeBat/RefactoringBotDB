package de.BA.refactoringBot.controller.main;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.grammer.botGrammer.BotOperationsBaseListener;
import de.BA.refactoringBot.grammer.botGrammer.BotOperationsLexer;
import de.BA.refactoringBot.grammer.botGrammer.BotOperationsParser;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.outputModel.myPullRequestComment.BotPullRequestComment;

/**
 * Diese Klasse enthält alle wichtigsten Grammatikfunktionen welche von Antlr
 * zur Verfügung gestellt werden.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class GrammerController {

	/**
	 * Diese Methode prüft ob ein Request-Kommentar von einem Filehoster eine
	 * gültige Bot-Grammatik hat.
	 * 
	 * @param comment
	 * @throws Exception
	 */
	public Boolean checkComment(String comment) {
		try {
			// Erstelle Antlr-Lexer ohne Konsolenausgabe
			BotOperationsLexer lexer = new BotOperationsLexer(CharStreams.fromString(comment));
			lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

			// Erstelle Antlr-Parser ohne Konsolenausgabe
			CommonTokenStream token = new CommonTokenStream(lexer);
			BotOperationsParser parser = new BotOperationsParser(token);
			parser.setBuildParseTree(true);
			parser.removeErrorListener(ConsoleErrorListener.INSTANCE);

			// Erstelle Parse-Tree
			ParseTree tree = parser.botCommand();
			ParseTreeWalker walker = new ParseTreeWalker();

			// Lauf Tree mit generierten Listener ab
			BotOperationsBaseListener listener = new BotOperationsBaseListener();
			walker.walk(listener, tree);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Diese Methode übersetzt einen Kommentar welcher die Grammatik des Bots
	 * erfüllt ein BotIssue-Objekt.
	 * 
	 * @param comment
	 * @return issue
	 */
	public BotIssue createIssueFromComment(BotPullRequestComment comment) {
		// Initiiere Objekt
		BotIssue issue = new BotIssue();

		// Splitte Kommentar an den Leerzeichen
		String[] commentArr = comment.getCommentBody().split(" ");

		issue.setCommentServiceID(comment.getCommentID().toString());
		issue.setLine(comment.getPosition());
		issue.setFilePath(comment.getFilepath());

		// Falls Hinzufüg-Operation
		if (commentArr[1].equals("ADD")) {
			// Falls Annotation hinzugefügt wird
			if (commentArr[2].equals("ANNOTATION")) {
				// Falls Override-Annotation
				if (commentArr[3].equals("Override")) {
					issue.setRefactoringOperation("Add Override Annotation");
				}
			}
		}

		// Falls etwas umgestellt wird
		if (commentArr[1].equals("REORDER")) {
			// Falls Modifier umgestellt werden
			if (commentArr[2].equals("MODIFIER")) {
				issue.setRefactoringOperation("Reorder Modifier");
			}
		}

		return issue;
	}
}
