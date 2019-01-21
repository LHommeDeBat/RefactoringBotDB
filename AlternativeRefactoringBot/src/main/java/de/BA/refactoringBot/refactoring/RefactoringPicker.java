package de.BA.refactoringBot.refactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.configuration.GitConfiguration;
import de.BA.refactoringBot.model.sonarQube.Issue;
import de.BA.refactoringBot.refactoring.supportedRefactorings.AddOverrideAnnotation;
import de.BA.refactoringBot.refactoring.supportedRefactorings.ReorderModifier;

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
	@Autowired
	ReorderModifier reorderModifier;

	/**
	 * Diese Methode wählt das passende Refactoring anhand des Issue-Objekts von
	 * Sonarcube aus.
	 * 
	 * @param issue
	 * @return commitMessage
	 * @throws Exception 
	 */
	public String pickRefactoring(Issue issue, GitConfiguration gitConfig) throws Exception {
	
		// Wähle Refactoring nach SonarCube-Rule
		try {
			switch (issue.getRule()) {
			case "squid:S1161":
				return addOverride.performRefactoring(issue, gitConfig);
			case "squid:ModifiersOrderCheck":
				return reorderModifier.performRefactoring(issue, gitConfig);
			default:
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Etwas ist beim Refactoring schiefgelaufen!");
		}
	}
}
