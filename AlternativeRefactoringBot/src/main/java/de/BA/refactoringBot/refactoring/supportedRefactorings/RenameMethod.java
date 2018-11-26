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

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.javaparser.ParserRefactoring;
import de.BA.refactoringBot.model.javaparser.ParserRefactoringCollection;

@Component
public class RenameMethod {

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

		// Configure solver for the project
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new JavaParserTypeSolver(
				"C:/Users/stefa/Bachelorarbeit-Repos/Test Pullrequest-Repo/TestPullRequest/src"));
		typeSolver.add(new ReflectionTypeSolver());
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser.getStaticConfiguration().setSymbolResolver(javaSymbolSolver);

		// Read file
		FileInputStream methodPath = new FileInputStream(issuePath);
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
			}
		}

		// If refactor-method not found
		if (methodToRefactor == null) {
			return null;
		}

		// Add class to the TO-DO list
		allRefactorings.addToDoClass(methodClassSignature);

		// Create super tree recursively
		allRefactorings = getSuperTree(allRefactorings, allJavaFiles, issuePath);

		// Add sub tree recursively
		while (true) {
			// Count classes before adding subclasses
			int beforeSubtree = allRefactorings.getDoneClasses().size();

			// Add subclasses
			for (String javaFile : allJavaFiles) {
				allRefactorings = addSubTree(allRefactorings, javaFile);
			}

			// Count classes after adding subclasses
			int afterSubtree = allRefactorings.getDoneClasses().size();

			// If no more new subclasses found
			if (beforeSubtree == afterSubtree) {
				break;
			}
		}

		// Create refactoring objects for every class
		for (String classSignature : allRefactorings.getDoneClasses()) {
			for (String javaFile : allJavaFiles) {
				createRefactoringObjects(allRefactorings, localMethodSignature, classSignature, javaFile, allJavaFiles);
			}
		}

		System.out.println("Amount of refactorings: " + allRefactorings.getRefactoring().size());

		renameFindings(allRefactorings, allJavaFiles, "esGehtImmerNoch");

		return "Renamed method '" + oldMethodName + "' to '" + /* issue.getRenameString() + */ "'";
	}

	/**
	 * This method renames all findings of method declarations and method calls
	 * inside the java project.
	 * 
	 * @param allRefactorings
	 * @param allJavaFiles
	 * @param newName
	 * @throws FileNotFoundException
	 */
	private void renameFindings(ParserRefactoringCollection allRefactorings, List<String> allJavaFiles, String newName)
			throws FileNotFoundException {

		// Iterate all java files
		for (String javaFile : allJavaFiles) {
			// Create compilation unit
			FileInputStream methodPath = new FileInputStream(javaFile);
			CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(methodPath));

			// for each refactoring
			for (ParserRefactoring refactoring : allRefactorings.getRefactoring()) {

				// if refactoring = method declaration
				if (refactoring.getMethod() != null) {
					@SuppressWarnings("deprecation")
					List<MethodDeclaration> methods = compilationUnit.getNodesByType(MethodDeclaration.class);
					// Search all methods
					for (MethodDeclaration method : methods) {
						// If methods match
						if (method.equals(refactoring.getMethod())) {
							performRenameMethod(method, newName);
						}
					}
				}

				// If refactoring = method call
				if (refactoring.getMethodCall() != null) {
					@SuppressWarnings("deprecation")
					List<MethodCallExpr> methodCalls = compilationUnit.getNodesByType(MethodCallExpr.class);
					// Iterate method calls that need refactoring
					for (MethodCallExpr refExpr : refactoring.getMethodCall()) {
						// For each method call inside the file
						for (MethodCallExpr expr : methodCalls) {
							// If method calls match
							if (expr.equals(refExpr)) {
								performRenameMethodCall(expr, newName);
							}
						}
					}
				}
			}

			// Save changes to file
			PrintWriter out = new PrintWriter(javaFile);
			out.println(LexicalPreservingPrinter.print(compilationUnit));
			out.close();
		}
	}

	/**
	 * This method gathers the super class tree from the class in which the method
	 * that needs to be refactored sits in.
	 * 
	 * @param allRefactorings
	 * @param allJavaFiles
	 * @param currentJavaFile
	 * @return
	 * @throws FileNotFoundException
	 */
	private ParserRefactoringCollection getSuperTree(ParserRefactoringCollection allRefactorings,
			List<String> allJavaFiles, String currentJavaFile) throws FileNotFoundException {

		// Init variable
		String classSignature = null;

		// parse a file
		FileInputStream filepath = new FileInputStream(currentJavaFile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// Check if
				if (allRefactorings.getToDoClasses().contains(classOrInterface.resolve().getQualifiedName())) {
					classSignature = classOrInterface.resolve().getQualifiedName();
					// Check all implements + extends classes/interfaces
					NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
					NodeList<ClassOrInterfaceType> ext = classOrInterface.getExtendedTypes();
					// Add all implements signatures to list
					for (int i = 0; i < impl.size(); i++) {
						allRefactorings.addToDoClass(impl.get(i).resolve().getQualifiedName());
					}
					// Add all extends signatures to list
					for (int i = 0; i < ext.size(); i++) {
						allRefactorings.addToDoClass(ext.get(i).resolve().getQualifiedName());
					}
				}
			}
		}

		// If class not super of previous class
		if (classSignature == null) {
			return allRefactorings;
		}

		// Mark class as done
		allRefactorings.addDoneClass(classSignature);
		allRefactorings.removeToDoClass(classSignature);

		// If super class tree finished
		if (allRefactorings.getToDoClasses().isEmpty()) {
			return allRefactorings;
		}

		// Recursively build super class tree
		for (String javaFile : allJavaFiles) {
			allRefactorings = getSuperTree(allRefactorings, allJavaFiles, javaFile);
		}

		return allRefactorings;
	}

	/**
	 * This method adds all subclasses to the already created super class tree.
	 * 
	 * @param allRefactorings
	 * @param currentJavaFile
	 * @return
	 * @throws FileNotFoundException
	 */
	private ParserRefactoringCollection addSubTree(ParserRefactoringCollection allRefactorings, String currentJavaFile)
			throws FileNotFoundException {
		// parse a file
		FileInputStream filepath = new FileInputStream(currentJavaFile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				// Check if class already marked as done
				if (!allRefactorings.getDoneClasses().contains(classOrInterface.resolve().getQualifiedName())) {
					// Check all implements + extends classes/interfaces
					NodeList<ClassOrInterfaceType> impl = classOrInterface.getImplementedTypes();
					NodeList<ClassOrInterfaceType> ext = classOrInterface.getExtendedTypes();
					// If class implements of one of the done classes
					for (int i = 0; i < impl.size(); i++) {
						if (allRefactorings.getDoneClasses().contains(impl.get(i).resolve().getQualifiedName())) {
							allRefactorings.addDoneClass(classOrInterface.resolve().getQualifiedName());
						}
					}
					// If class extends of one of the done classes
					for (int i = 0; i < ext.size(); i++) {
						if (allRefactorings.getDoneClasses().contains(ext.get(i).resolve().getQualifiedName())) {
							allRefactorings.addDoneClass(classOrInterface.resolve().getQualifiedName());
						}
					}
				}
			}
		}

		return allRefactorings;
	}

	/**
	 * This method creates refactoring objects to rename a method inside a specific
	 * class and to rename all method calls for that specific method inside the
	 * specific class.
	 * 
	 * @param allRefactorings
	 * @param methodSignature
	 * @param classSignature
	 * @param javaFile
	 * @param allJavaFiles
	 * @return
	 * @throws FileNotFoundException
	 */
	private ParserRefactoringCollection createRefactoringObjects(ParserRefactoringCollection allRefactorings,
			String methodSignature, String classSignature, String javaFile, List<String> allJavaFiles)
			throws FileNotFoundException {

		// parse a file
		FileInputStream filepath = new FileInputStream(javaFile);
		CompilationUnit compilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(filepath));

		// Search all Types
		for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
			// If type = class
			if (type.isClassOrInterfaceDeclaration()) {
				ClassOrInterfaceDeclaration classOrInterface = type.asClassOrInterfaceDeclaration();
				if (classSignature.equals(classOrInterface.resolve().getQualifiedName())) {
					// Get all methods
					List<MethodDeclaration> methods = classOrInterface.getMethods();
					// Search methods
					for (MethodDeclaration method : methods) {
						// If method found
						if (methodSignature.equals(getMethodDeclarationAsString(method))) {
							// Get global Signature of method
							String globalMethodSignature = getFullMethodSignature(method);

							// Check all java files and create refactorings for method calls
							for (String file : allJavaFiles) {
								allRefactorings = createRefactoringsForMethodCalls(allRefactorings, file,
										globalMethodSignature);
							}

							// Create refactoring for method
							ParserRefactoring methodRefactoring = new ParserRefactoring();
							methodRefactoring.setJavaFile(javaFile);
							methodRefactoring.setMethod(method);
							methodRefactoring.setUnit(compilationUnit);
							allRefactorings.getRefactoring().add(methodRefactoring);
						}
					}
				}
			}
		}

		return allRefactorings;
	}

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
	private ParserRefactoringCollection createRefactoringsForMethodCalls(ParserRefactoringCollection refactorings,
			String javafile, String methodSignature) throws FileNotFoundException {
		// parse a file
		FileInputStream callMethodPath = new FileInputStream(javafile);
		CompilationUnit renameMethodCallUnit = LexicalPreservingPrinter.setup(JavaParser.parse(callMethodPath));

		@SuppressWarnings("deprecation")
		List<MethodCallExpr> methodCalls = renameMethodCallUnit.getNodesByType(MethodCallExpr.class);

		// Create refactoring
		ParserRefactoring refactoring = new ParserRefactoring();
		List<MethodCallExpr> validCalls = new ArrayList<MethodCallExpr>();

		// Rename all suitable method calls
		for (MethodCallExpr methodCall : methodCalls) {
			// Check if call invokes the refactoring method
			if (checkMethodCall(methodCall, methodSignature) != null) {
				validCalls.add(checkMethodCall(methodCall, methodSignature));
			}
		}

		// Fill refactoring with data
		refactoring.setJavaFile(javafile);
		refactoring.setUnit(renameMethodCallUnit);

		// If no valid call found
		if (validCalls.isEmpty()) {
			return refactorings;
		}

		refactoring.setMethodCall(validCalls);
		refactorings.addRefactoring(refactoring);

		return refactorings;
	}

	/**
	 * This method renames all suitable method calls with the help of the method
	 * signature of the renamed method.
	 * 
	 * @param methodCall
	 * @param newName
	 */
	private MethodCallExpr checkMethodCall(MethodCallExpr methodCall, String globalMethodSignature) {
		// Resolve method call
		ResolvedMethodDeclaration calledMethod = methodCall.resolve();
		// If call belongs to the refactored method
		if (calledMethod.getQualifiedSignature().equals(globalMethodSignature)) {
			// return methodcall
			return methodCall;
		}
		return null;
	}

	/**
	 * This method returns the global signature of a method as a string.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @return
	 */
	private String getFullMethodSignature(MethodDeclaration methodDeclaration, Integer position) {
		// If method is at the refactored position
		if (position == methodDeclaration.getBegin().get().line) {
			ResolvedMethodDeclaration resolvedMethod = methodDeclaration.resolve();
			return resolvedMethod.getQualifiedSignature();
		}
		return null;
	}

	/**
	 * This method returns the global signature of a method as a string.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @return
	 */
	private String getFullMethodSignature(MethodDeclaration methodDeclaration) {
		ResolvedMethodDeclaration resolvedMethod = methodDeclaration.resolve();
		return resolvedMethod.getQualifiedSignature();
	}

	/**
	 * This method returns the local signature of a method as a string.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @return
	 */
	private String getMethodDeclarationAsString(MethodDeclaration methodDeclaration, Integer position) {
		// If method is at the refactored position
		if (position == methodDeclaration.getBegin().get().line) {
			return methodDeclaration.getSignature().asString();
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
	private String getMethodDeclarationAsString(MethodDeclaration methodDeclaration) {
		return methodDeclaration.getSignature().asString();
	}

	/**
	 * This method performs the renaming of the method inside the local class.
	 * 
	 * @param methodDeclaration
	 * @param position
	 * @param newName
	 */
	private void performRenameMethod(MethodDeclaration methodDeclaration, String newName) {
		methodDeclaration.setName(newName);
	}

	/**
	 * This method performs the renaming of a method call.
	 * 
	 * @param methodCall
	 * @param newName
	 */
	private void performRenameMethodCall(MethodCallExpr methodCall, String newName) {
		methodCall.setName(newName);
	}
}
