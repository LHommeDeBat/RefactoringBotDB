package de.BA.refactoringBot.refactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.BA.refactoringBot.model.botIssue.BotIssue;
import de.BA.refactoringBot.model.configuration.GitConfiguration;
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
	public String pickRefactoring(BotIssue issue, GitConfiguration gitConfig) throws Exception {
	
		// Wähle Refactoring nach Operation
		try {
			switch (issue.getRefactoringOperation()) {
			case "Add Override Annotation":
				return addOverride.performRefactoring(issue, gitConfig);
			case "Reorder Modifier":
				return reorderModifier.performRefactoring(issue, gitConfig);
			default:
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Etwas ist beim Refactoring schiefgelaufen!");
		}
	}
}
