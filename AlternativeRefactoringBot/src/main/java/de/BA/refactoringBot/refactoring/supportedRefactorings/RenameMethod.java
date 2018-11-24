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
import de.BA.refactoringBot.model.javaparser.ParserRefactoring;
import de.BA.refactoringBot.model.javaparser.ParserRefactoringCollection;

@Component
public class RenameMethod {

	@Autowired
	BotConfiguration botConfig;

	public String performRefactoring(String issuePath, Integer line, BotIssue issue, GitConfiguration gitConfig)
			throws IOException {

		// Init Refactorings
		ParserRefactoringCollection allRefactorings = new ParserRefactoringCollection();

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
			return null;
		}

		// Add refactoring method to list
		ParserRefactoring methodRefactoring = new ParserRefactoring();
		methodRefactoring.setJavaFile(methodDeclarationPath);
		methodRefactoring.setMethod(methodToRefactor);
		methodRefactoring.setUnit(renameMethodUnit);

		allRefactorings.addRefactoring(methodRefactoring);

		// Go through all java-files and rename method calls for our method
		for (String javafile : allJavaFiles) {
			// rename method calls
			allRefactorings = renameMethodCalls(allRefactorings, javafile,
					globalMethodSignature);
		}
		
		// Add file to 'doneFiles'
		allRefactorings.addDoneFile(issuePath);

		// Refactor superclass/interface
		for (String javafile : allJavaFiles) {
			for (String implement : methodClassImplements) {
				allRefactorings = renameMethodInSuper(allRefactorings,
						javafile, allJavaFiles, localMethodSignature, implement);
				
				// renameMethodInSub
				// renameMethodInSub(javafile, localMethodSignature, "testInterface2",
				// methodClassSignature);
			}
		}

		System.out.println(allRefactorings.getRefactoring().size());

		for (ParserRefactoring refactoring : allRefactorings.getRefactoring()) {

			// If refactoring both call and method -> error
			if (refactoring.getMethod() != null && refactoring.getMethodCall() != null) {
				return null;
			}

			if (refactoring.getMethod() != null) {
				performRenameMethod(refactoring.getMethod(), "pleaseWork");
			}

			if (refactoring.getMethodCall() != null) {
				performRenameMethodCall(refactoring.getMethodCall(), "pleaseWork");
			}

			// Save changes to file
			PrintWriter out = new PrintWriter(refactoring.getJavaFile());
			out.println(LexicalPreservingPrinter.print(refactoring.getUnit()));
			out.close();
		}

		return "Renamed method '" + oldMethodName + "' to '" + /* issue.getRenameString() + */ "'";
	}

	/**
	 * This method checks if the refactored method is inherited from another
	 * interface and renames the method there too.
	 * 
	 * @param superRefactorings
	 * 
	 * @param javafile
	 * @param localMethodSignature
	 * @param newName
	 * @param classInterfaces
	 * @throws FileNotFoundException
	 */
	public ParserRefactoringCollection renameMethodInSuper(ParserRefactoringCollection allRefactorings, String javafile,
			List<String> allJavaFiles, String localMethodSignature, String implement) throws FileNotFoundException {

		// If file already refactored
		System.out.println("");
		System.out.println(javafile);
		if (allRefactorings.getDoneFiles().contains(javafile)) {
			return allRefactorings;
		}
		
		// Init files
		String globalMethodSignature = null;
		MethodDeclaration recursiveMethodToRefactor = null;
		List<String> allSuper = new ArrayList<String>();

		// parse a file
		FileInputStream filepath = new FileInputStream(javafile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// If class is superclass of the class of our refactored method
				if (implement.equals(classOrInterface.resolve().getQualifiedName())) {
					List<MethodDeclaration> methods = classOrInterface.getMethods();
					// Search methods
					for (MethodDeclaration method : methods) {
						globalMethodSignature = getMethodInAnotherClass(method, localMethodSignature);
						// If method found
						if (globalMethodSignature != null) {
							recursiveMethodToRefactor = method;
							break;
						}
					}
					// Check all implements + extends classes/interfaces
					NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
					NodeList<ClassOrInterfaceType> ext = classOrInterface.getExtendedTypes();
					// Add all implements signatures to list
					for (int i = 0; i < impl.size(); i++) {
						allSuper.add(impl.get(i).resolve().getQualifiedName());
					}
					// Add all extends signatures to list
					for (int i = 0; i < ext.size(); i++) {
						allSuper.add(ext.get(i).resolve().getQualifiedName());
					}
				}
			}
		}

		// If Method to refactor exists
		if (recursiveMethodToRefactor != null) {
			// Create refactoring for method
			ParserRefactoring methodRefactoring = new ParserRefactoring();
			methodRefactoring.setJavaFile(javafile);
			methodRefactoring.setMethod(recursiveMethodToRefactor);
			methodRefactoring.setUnit(compilationUnit);
			allRefactorings.getRefactoring().add(methodRefactoring);
			System.out.println("Habe Methode umbenannt!");

			// Create refactoring for method calls
			for (String javaRecursiveFile : allJavaFiles) {
				allRefactorings = renameMethodCalls(allRefactorings, javaRecursiveFile,
						globalMethodSignature);
				System.out.println("Habe Aufruf umbenannt!");
			}
		}

		// Recursion
		for (String javaRecursiveFile : allJavaFiles) {
			for (String recursiveImplement : allSuper) {
				allRefactorings = renameMethodInSuper(allRefactorings, javaRecursiveFile,
						allJavaFiles, localMethodSignature, recursiveImplement);
			}
		}

		return allRefactorings;
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
	// public void renameMethodInSub(String javafile, String localMethodSignature,
	// String newName,
	// String methodClassSignature) throws FileNotFoundException {
	// // parse a file
	// FileInputStream filepath = new FileInputStream(javafile);
	// CompilationUnit compilationUnit =
	// LexicalPreservingPrinter.setup(JavaParser.parse(filepath));
	//
	// // Search all Types
	// for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
	// // If type = class
	// if (type.isClassOrInterfaceDeclaration()) {
	// ClassOrInterfaceDeclaration classOrInterface =
	// type.asClassOrInterfaceDeclaration();
	// // Check all interfaces its implementing
	// NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
	// for (int i = 0; i < impl.size(); i++) {
	// // Check if subclass inherits from our refactor-method-class
	// if (impl.get(i).resolve().getQualifiedName().equals(methodClassSignature)) {
	// // Search methods if it does
	// List<MethodDeclaration> methods = classOrInterface.getMethods();
	// for (MethodDeclaration method : methods) {
	// performRenameMethodInAnotherClass(method, localMethodSignature, newName);
	// }
	// }
	// }
	// }
	// }
	//
	// // Save changes to file
	// PrintWriter out = new PrintWriter(javafile);
	// out.println(LexicalPreservingPrinter.print(compilationUnit));
	// out.close();
	// }

	/**
	 * This method reads a java file, performs a refactor and saves the changes to
	 * the file.
	 * 
	 * @param list
	 * 
	 * @param javafile
	 * @param methodSignature
	 * @throws FileNotFoundException
	 */
	public ParserRefactoringCollection renameMethodCalls(ParserRefactoringCollection refactorings, String javafile,
			String methodSignature) throws FileNotFoundException {
		// parse a file
		FileInputStream callMethodPath = new FileInputStream(javafile);
		CompilationUnit renameMethodCallUnit = LexicalPreservingPrinter.setup(JavaParser.parse(callMethodPath));

		@SuppressWarnings("deprecation")
		List<MethodCallExpr> methodCalls = renameMethodCallUnit.getNodesByType(MethodCallExpr.class);

		// Rename all suitable method calls
		for (MethodCallExpr methodCall : methodCalls) {
			// Create refactoring
			ParserRefactoring refactoring = new ParserRefactoring();

			// Check if call invokes the refactoring method
			if (checkMethodCall(methodCall, methodSignature) != null) {
				refactoring.setMethodCall(checkMethodCall(methodCall, methodSignature));
			}

			// Fill refactoring with data
			refactoring.setJavaFile(javafile);
			refactoring.setUnit(renameMethodCallUnit);

			// Add to list if call invokes the refactoring method
			if (refactoring.getMethodCall() != null) {
				refactorings.getRefactoring().add(refactoring);
			}
		}

		return refactorings;
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
	 * This method performs the renaming of a method call.
	 * 
	 * @param methodCall
	 * @param newName
	 */
	public void performRenameMethodCall(MethodCallExpr methodCall, String newName) {
		methodCall.setName(newName);
	}

	public String getMethodInAnotherClass(MethodDeclaration methodDeclaration, String localMethodSignature) {
		// If signatures are the same
		if (methodDeclaration.getSignature().asString().equals(localMethodSignature)) {
			ResolvedMethodDeclaration resolvedMethod = methodDeclaration.resolve();
			return resolvedMethod.getQualifiedSignature();
		}

		return null;
	}

	/**
	 * This method renames all suitable method calls with the help of the method
	 * signature of the renamed method.
	 * 
	 * @param methodCall
	 * @param newName
	 */
	public MethodCallExpr checkMethodCall(MethodCallExpr methodCall, String globalMethodSignature) {
		// Resolve method call
		ResolvedMethodDeclaration calledMethod = methodCall.resolve();
		// If call belongs to the refactored method
		if (calledMethod.getQualifiedSignature().equals(globalMethodSignature)) {
			// return methodcall
			return methodCall;
		}
		return null;
	}

}
