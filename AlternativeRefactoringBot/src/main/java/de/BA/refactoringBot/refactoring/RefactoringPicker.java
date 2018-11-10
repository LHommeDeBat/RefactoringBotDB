package de.BA.refactoringBot.refactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;
import de.BA.refactoringBot.model.sonarQube.Issue;
import de.BA.refactoringBot.refactoring.supportedRefactorings.AddOverrideAnnotation;

/**
 * Diese Klasse entscheidet welches Refactoring durchgeführt werden muss und
 * wählt dafür die passende Klasse, an dem sie das Refactoring-Issue
 * weiterleitet.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class RefactoringPicker {

	@Autowired
	AddOverrideAnnotation addOverride;

	/**
	 * Diese Methode wählt das passende Refactoring anhand des Issue-Objekts von
	 * Sonarcube aus.
	 * 
	 * @param issue
	 * @return refactoredIssue
	 * @throws Exception 
	 */
	public RefactoredIssue pickRefactoring(Issue issue, GitConfiguration gitConfig) throws Exception {

		// Wähle Refactoring nach SonarCube-Key
		try {
			switch (issue.getKey()) {
			case "squid:S1161":
				return addOverride.performRefactoring(issue, gitConfig);
			default:
				throw new Exception("Refactoring wird nicht unterstützt " + issue.getKey());
			}
		} catch (Exception e) {
			throw new Exception("Etwas ist beim Refactoring schiefgelaufen!");
		}
	}
}
