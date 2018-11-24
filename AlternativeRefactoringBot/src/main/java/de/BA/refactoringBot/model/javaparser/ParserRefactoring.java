package de.BA.refactoringBot.model.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.body.MethodDeclaration;

public class ParserRefactoring {

	private CompilationUnit unit;
	private MethodCallExpr methodCall;
    private MethodDeclaration method;
	private String javaFile;
	
	public CompilationUnit getUnit() {
		return unit;
	}
	
	public void setUnit(CompilationUnit unit) {
		this.unit = unit;
	}

	public MethodCallExpr getMethodCall() {
		return methodCall;
	}

	public void setMethodCall(MethodCallExpr methodCall) {
		this.methodCall = methodCall;
	}

	public MethodDeclaration getMethod() {
		return method;
	}

	public void setMethod(MethodDeclaration method) {
		this.method = method;
	}

	public String getJavaFile() {
		return javaFile;
	}

	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}
	
}
