package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.FileNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
     * This method performs the refactoring and returns a commit message.
     *
     * @param issue
     * @param gitConfig
     * @return commitMessage
     * @throws FileNotFoundException
     */
    public String performRefactoring(BotIssue issue, GitConfiguration gitConfig) throws FileNotFoundException, IOException {
        // Prepare data
        String path
                = botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId() + "/" + issue.getFilePath();
        line = issue.getLine();

        // Read file
        FileInputStream in = new FileInputStream(path);
        CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(in));

        List<Comment> comments = compilationUnit.getAllContainedComments();

        for (Comment comment : comments) {
            if ((line >= comment.getBegin().get().line) && (line <= comment.getEnd().get().line)) {
                System.out.println("Removing comment");
                removeLinesFromFile(comment.getRange().get(), path);
                break;
            }
        }

        // Return commit message
        return "Removed commented out code at line " + line;
    }

    private void removeLinesFromFile(Range lines, String path) throws FileNotFoundException, IOException {
        File inputFile = new File(path);
        File tempFile = new File(inputFile.getParent() + File.separator + "temp.java");
        System.out.println(inputFile.getParent() + File.separator + "temp.java");

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String currentLine;
        int lineNumber = 0;

        while ((currentLine = reader.readLine()) != null) {
            lineNumber++;

            if ((lineNumber >= lines.begin.line) && (lineNumber <= lines.end.line)) {
                continue;
            }
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        
        writer.close();
        reader.close();
        inputFile.delete();
        boolean successful = tempFile.renameTo(inputFile);

    }

}
