package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * This class is used for executing the removal of commented out code
 *
 * @author Justin Kissling
 */
@Component
public class RemoveCommentedOutCode extends VoidVisitorAdapter<Object> {

    Integer line;

    @Autowired
    BotConfiguration botConfig;

    /**
     * This method performs the refactoring and returns the a commit message.
     *
     * @param issue
     * @param gitConfig
     * @return commitMessage
     * @throws FileNotFoundException
     */
    public String performRefactoring(BotIssue issue, GitConfiguration gitConfig) throws FileNotFoundException {
        // Prepare data
        String path = issue.getFilePath();
        line = issue.getLine();

        CompilationUnit compilationUnit = JavaParser.parse(new File(botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId() + "/" + path));

        List<Comment> comments = compilationUnit.getAllContainedComments();

        for (Comment co : comments) {
            System.out.println(co.getRange().get().begin.line);
            if (co.getRange().get().begin.line == line) {
                System.out.println(co.getContent());
                co.remove();
            }
        }

        // Save changes to file
        PrintWriter out = new PrintWriter(
                botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId() + "/" + path);
        out.println(LexicalPreservingPrinter.print(compilationUnit));
        out.close();

        // Return commit message
        return "Removed commented out code at";
    }

}
