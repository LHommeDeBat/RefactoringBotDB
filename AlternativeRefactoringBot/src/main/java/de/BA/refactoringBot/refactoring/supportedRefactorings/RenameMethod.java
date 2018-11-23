package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import de.BA.refactoringBot.configuration.BotConfiguration;
import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;

@Component
public class RenameMethod {

	@Autowired
	BotConfiguration botConfig;

	static String renameMethodSignature = "";
	static Integer position;

	public String performRefactoring(String issuePath, Integer line, BotIssue issue, GitConfiguration gitConfig)
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

		// Setup variables
		position = line;

		// Get method declaration path
		// String methodDeclarationPath = issue.getFilePath();
		String methodDeclarationPath = issuePath;

		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new JavaParserTypeSolver(
				"C:/Users/stefa/Bachelorarbeit-Repos/Test Pullrequest-Repo/TestPullRequest/src"));
		typeSolver.add(new ReflectionTypeSolver());
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser.getStaticConfiguration().setSymbolResolver(javaSymbolSolver);

		// Read file
		FileInputStream methodPath = new FileInputStream(methodDeclarationPath);
		CompilationUnit renameMethodUnit = LexicalPreservingPrinter.setup(JavaParser.parse(methodPath));

		// Test Interface
		for (TypeDeclaration<?> type : renameMethodUnit.getTypes()) {
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
				for (int i = 0; i < impl.size(); i++) {
					System.out.println(impl.get(i).resolve().getQualifiedName());
				}
			}
		}

		// Construct full method name
		renameMethodUnit.accept(new MethodGetter(), null);

		// Go through all java-files and rename method calls for our method
		for (String javafile : allJavaFiles) {
			// parse a file
			FileInputStream callMethodPath = new FileInputStream(javafile);
			CompilationUnit renameMethodCallUnit = LexicalPreservingPrinter.setup(JavaParser.parse(callMethodPath));
			renameMethodCallUnit.accept(new MethodCallExprChanger(), null);

			// Save changes to file
			PrintWriter out = new PrintWriter(javafile);
			out.println(LexicalPreservingPrinter.print(renameMethodCallUnit));
			out.close();
		}

		// Rename Method
		renameMethodUnit.accept(new MethodChanger(), null);
		// Save changes to file
		PrintWriter out = new PrintWriter(methodDeclarationPath);
		out.println(LexicalPreservingPrinter.print(renameMethodUnit));
		out.close();

		return null;
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes.
	 */
	private static class MethodGetter extends VoidVisitorAdapter<Void> {
		@Override
		public void visit(MethodDeclaration n, Void arg) {
			// change the name of the method to upper case
			if (position == n.getBegin().get().line) {
				ResolvedMethodDeclaration resolvedMethod = n.resolve();
				renameMethodSignature = resolvedMethod.getQualifiedSignature();
			}
		}
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes.
	 */
	private static class MethodChanger extends VoidVisitorAdapter<Void> {
		@Override
		public void visit(MethodDeclaration n, Void arg) {
			// change the name of the method to upper case
			if (position == n.getBegin().get().line) {
				n.setName("nandato");
			}
		}
	}

	/**
	 * Simple visitor implementation for visiting MethodDeclaration nodes.
	 */
	private static class MethodCallExprChanger extends VoidVisitorAdapter<Void> {
		@Override
		public void visit(MethodCallExpr n, Void arg) {
			// add a new parameter to the metho
			ResolvedMethodDeclaration calledMethod = n.resolve();
			System.out.println("Invokes: " + calledMethod.getQualifiedSignature());
			if (calledMethod.getQualifiedSignature().equals(renameMethodSignature)) {
				n.setName("nandato");
			}
		}
	}

}
