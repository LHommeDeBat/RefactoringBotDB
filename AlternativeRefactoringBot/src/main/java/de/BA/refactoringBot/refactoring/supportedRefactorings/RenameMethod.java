package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;

@Component
public class RenameMethod {

	@Autowired
	BotConfiguration botConfig;

	public String performRefactoring(String issuePath/* BotIssue issue */, GitConfiguration gitConfig)
			throws IOException {

		// Init all java files path-list
		List<String> allJavaFiles = new ArrayList<String>();

		// Get root folder of project
		File dir = new File(/*
							 * botConfig.getBotRefactoringDirectory() + gitConfig.getConfigurationId() + "/"
							 * + gitConfig.getProjectRootFolder()
							 */ "C:/Users/stefa/Bachelorarbeit-Repos/Test Pullrequest-Repo/TestPullRequest");

		// Get paths to all java files of the project
		List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		for (File file : files) {
			if (file.getCanonicalPath().endsWith(".java")) {
				allJavaFiles.add(file.getCanonicalPath());
			}
		}

		// Get method declaration path
		// String methodDeclarationPath = issue.getFilePath();
		String methodDeclarationPath = issuePath;
		
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new JavaParserTypeSolver("C:/Users/stefa/Bachelorarbeit-Repos/Test Pullrequest-Repo/TestPullRequest/src"));
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser.getStaticConfiguration().setSymbolResolver(javaSymbolSolver);

		// parse a file
		CompilationUnit renameMethodUnit = JavaParser.parse(new File(methodDeclarationPath));

		// visit and change the methods names and parameters
		renameMethodUnit.accept(new MethodChangerVisitor(), null);


		for (String javafile : allJavaFiles) {
			// parse a file
			CompilationUnit renameMethodCallUnit = JavaParser.parse(new File(javafile));
			renameMethodCallUnit.accept(new MethodCallExprChangerVisitor(), null);
		}

		return null;
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes.
	 */
	private static class MethodChangerVisitor extends VoidVisitorAdapter<Void> {
		@Override
		public void visit(MethodDeclaration n, Void arg) {
			// change the name of the method to upper case
			System.out.println("Method: " + n.getSignature());
			System.out.println("");

		}
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes.
	 */
	private static class MethodCallExprChangerVisitor extends VoidVisitorAdapter<Void> {
		@Override
		public void visit(MethodCallExpr n, Void arg) {
			// add a new parameter to the metho
			ResolvedMethodDeclaration calledMethod = n.resolve();
			System.out.println("Invokes: " + calledMethod.getQualifiedSignature());
		}
	}

}
