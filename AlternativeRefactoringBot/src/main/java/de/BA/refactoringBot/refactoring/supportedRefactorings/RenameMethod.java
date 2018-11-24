package de.BA.refactoringBot.refactoring.supportedRefactorings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

	public String performRefactoring(String issuePath, Integer line, BotIssue issue, GitConfiguration gitConfig)
			throws IOException {

		// Init all java files path-list
		List<String> allJavaFiles = new ArrayList<String>();

		// Init needed variables
		String globalMethodSignature = null;
		String localMethodSignature = null;
		String methodClassSignature = null;
		String oldMethodName = null;
		MethodDeclaration methodToRefactor = null;
		List<String> methodClassImplements = new ArrayList<String>();

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
		typeSolver.add(new JavaParserTypeSolver(
				"C:/Users/stefa/Bachelorarbeit-Repos/Test Pullrequest-Repo/TestPullRequest/src"));
		typeSolver.add(new ReflectionTypeSolver());
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser.getStaticConfiguration().setSymbolResolver(javaSymbolSolver);

		// Read file
		FileInputStream methodPath = new FileInputStream(methodDeclarationPath);
		CompilationUnit renameMethodUnit = LexicalPreservingPrinter.setup(JavaParser.parse(methodPath));

		// Search all Types
		for (TypeDeclaration<?> type : renameMethodUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// Get all methods
				List<MethodDeclaration> methods = classOrInterface.getMethods();
				// Search methods
				for (MethodDeclaration method : methods) {
					// check if method = desired method
					globalMethodSignature = getFullMethodSignature(method, line);
					localMethodSignature = getMethodDeclarationAsString(method, line);
					oldMethodName = method.getNameAsString();
					// If it is
					if (globalMethodSignature != null) {
						// Set method and class signatures of the method
						methodToRefactor = method;
						methodClassSignature = classOrInterface.resolve().getQualifiedName();
						break;
					}
				}

				// If class of method found
				if (methodClassSignature != null) {
					// Check all implements + extends classes/interfaces
					NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
					NodeList<ClassOrInterfaceType> ext = classOrInterface.getExtendedTypes();
					// Add all implements signatures to list
					for (int i = 0; i < impl.size(); i++) {
						methodClassImplements.add(impl.get(i).resolve().getQualifiedName());
					}
					// Add all extends signatures to list
					for (int i = 0; i < ext.size(); i++) {
						methodClassImplements.add(ext.get(i).resolve().getQualifiedName());
					}
					break;
				}
			}
		}

		// If refactor-method not found
		if (methodToRefactor == null || methodClassSignature == null || globalMethodSignature == null
				|| localMethodSignature == null) {
			System.out.println("Method-Object: " + methodToRefactor);
			System.out.println("Method-Declaration: " + localMethodSignature);
			System.out.println("Method-Signature: " + globalMethodSignature);
			System.out.println("Method-Class-Signature: " + methodClassSignature);
			System.out.println("Old-Method-Name: " + oldMethodName);
			return null;
		}

//		// Test output
//		System.out.println("Method-Object: " + methodToRefactor);
//		System.out.println("Method-Local-Signature: " + localMethodSignature);
//		System.out.println("Method-Global-Signature: " + globalMethodSignature);
//		System.out.println("Method-Class-Signature: " + methodClassSignature);
//		System.out.println("Old-Method-Name: " + oldMethodName);
//		for (String implementsSignature : methodClassImplements) {
//			System.out.println("Class-Implements-Signature: " + implementsSignature);
//		}

		// Go through all java-files and rename method calls for our method
		for (String javafile : allJavaFiles) {
			// rename method calls
			renameMethodCalls(javafile, globalMethodSignature, "testInterface2");
			// renameMethodInSuper
			renameMethodInSuper(javafile, localMethodSignature, "testInterface2", methodClassImplements);
			// renameMethodInSub
			renameMethodInSub(javafile, localMethodSignature, "testInterface2", methodClassSignature);
		}

		// Rename Method
		performRenameMethod(methodToRefactor, "testInterface2");

		// Save changes to file
		PrintWriter out = new PrintWriter(methodDeclarationPath);
		out.println(LexicalPreservingPrinter.print(renameMethodUnit));
		out.close();

		return "Renamed method '" + oldMethodName + "' to '" + /* issue.getRenameString() + */ "'";
	}

	/**
	 * This method checks if the refactored method is inherited from another
	 * interface and renames the method there too.
	 * 
	 * @param javafile
	 * @param localMethodSignature
	 * @param newName
	 * @param classInterfaces
	 * @throws FileNotFoundException
	 */
	public void renameMethodInSuper(String javafile, String localMethodSignature, String newName,
			List<String> classInterfaces) throws FileNotFoundException {
		// parse a file
		FileInputStream filepath = new FileInputStream(javafile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// If class is superclass of the class of our refactored method
				if (classInterfaces.contains(classOrInterface.resolve().getQualifiedName())) {
					List<MethodDeclaration> methods = classOrInterface.getMethods();
					// Search methods
					for (MethodDeclaration method : methods) {
						performRenameMethodInAnotherClass(method, localMethodSignature, newName);
					}
				}
			}
		}

		// Save changes to file
		PrintWriter out = new PrintWriter(javafile);
		out.println(LexicalPreservingPrinter.print(compilationUnit));
		out.close();
	}

	/**
	 * This method checks if the refactored method can be found in a subcalls which
	 * inherits it and renames it there too.
	 * 
	 * @param javafile
	 * @param localMethodSignature
	 * @param newName
	 * @param classInterfaces
	 * @throws FileNotFoundException
	 */
	public void renameMethodInSub(String javafile, String localMethodSignature, String newName,
			String methodClassSignature) throws FileNotFoundException {
		// parse a file
		FileInputStream filepath = new FileInputStream(javafile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// Check all interfaces its implementing
				NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
				for (int i = 0; i < impl.size(); i++) {
					// Check if subclass inherits from our refactor-method-class
					if (impl.get(i).resolve().getQualifiedName().equals(methodClassSignature)) {
						// Search methods if it does
						List<MethodDeclaration> methods = classOrInterface.getMethods();
						for (MethodDeclaration method : methods) {
							performRenameMethodInAnotherClass(method, localMethodSignature, newName);
						}
					}
				}
			}
		}

		// Save changes to file
		PrintWriter out = new PrintWriter(javafile);
		out.println(LexicalPreservingPrinter.print(compilationUnit));
		out.close();
	}

	/**
	 * This method reads a java file, performs a refactor and saves the changes to
	 * the file.
	 * 
	 * @param javafile
	 * @param methodSignature
	 * @throws FileNotFoundException
	 */
	public void renameMethodCalls(String javafile, String methodSignature, String newName)
			throws FileNotFoundException {
		// parse a file
		FileInputStream callMethodPath = new FileInputStream(javafile);
		CompilationUnit renameMethodCallUnit = LexicalPreservingPrinter.setup(JavaParser.parse(callMethodPath));

		@SuppressWarnings("deprecation")
		List<MethodCallExpr> methodCalls = renameMethodCallUnit.getNodesByType(MethodCallExpr.class);

		// Rename all suitable method calls
		for (MethodCallExpr methodCall : methodCalls) {
			performRenameMethodCall(methodCall, methodSignature, newName);
		}

		// Save changes to file
		PrintWriter out = new PrintWriter(javafile);
		out.println(LexicalPreservingPrinter.print(renameMethodCallUnit));
		out.close();
	}

	/**
	 * This method returns the global signature of a method as a string.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @return
	 */
	public String getFullMethodSignature(MethodDeclaration methodDeclaration, Integer position) {
		// If method is at the refactored position
		if (position == methodDeclaration.getBegin().get().line) {
			ResolvedMethodDeclaration resolvedMethod = methodDeclaration.resolve();
			return resolvedMethod.getQualifiedSignature();
		}
		return null;
	}

	/**
	 * This method returns the local signature of a method as a string.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @return
	 */
	public String getMethodDeclarationAsString(MethodDeclaration methodDeclaration, Integer position) {
		// If method is at the refactored position
		if (position == methodDeclaration.getBegin().get().line) {
			return methodDeclaration.getSignature().asString();
		}
		return null;
	}

	/**
	 * This method performs the renaming of the method inside the local class.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @param newName
	 */
	public void performRenameMethod(MethodDeclaration methodDeclaration, String newName) {
		methodDeclaration.setName(newName);
	}

	/**
	 * This method performs the renaming of the method in another sub or super
	 * interface/class.
	 * 
	 * @param methodDeclaration
	 * @param localMethodSignature
	 * @param newName
	 */
	public void performRenameMethodInAnotherClass(MethodDeclaration methodDeclaration, String localMethodSignature,
			String newName) {
		// If signatures are the same
		if (methodDeclaration.getSignature().asString().equals(localMethodSignature)) {
			methodDeclaration.setName(newName);
		}
	}

	/**
	 * This method renames all suitable method calls with the help of the method
	 * signature of the renamed method.
	 * 
	 * @param methodCall
	 * @param newName
	 */
	public void performRenameMethodCall(MethodCallExpr methodCall, String globalMethodSignature, String newName) {
		// Resolve method call
		ResolvedMethodDeclaration calledMethod = methodCall.resolve();
		// If call belongs to the refactored method
		if (calledMethod.getQualifiedSignature().equals(globalMethodSignature)) {
			// rename method call
			methodCall.setName(newName);
		}
	}

}
