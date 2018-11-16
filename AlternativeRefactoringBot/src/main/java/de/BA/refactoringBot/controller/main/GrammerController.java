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
}
