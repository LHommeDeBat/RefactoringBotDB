package de.BA.refactoringBot.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssue;
import de.BA.refactoringBot.model.refactoredIssue.RefactoredIssueRepository;
import io.swagger.annotations.ApiOperation;

/**
 * Diese Klasse bietet ein CRUD-Interface als REST-Schnittstelle für die
 * durchgeführten Refactorings an an.
 * 
 * @author Stefan Basaric
 *
 */
@RestController
@RequestMapping(path = "/refactoredIssues")
public class RefactoredIssuesController {

	@Autowired
	RefactoredIssueRepository repo;

	/**
	 * Diese Methode holt alle bearbeiteten Issues aus der DB.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Lese alle vom Bot bearbeiteten Issues aus")
	public ResponseEntity<?> getAllIssues() {
		// Hole alle Issues und gebe sie zurück
		Iterable<RefactoredIssue> allIssues = repo.findAll();
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}

	/**
	 * Diese Methode holt alle bearbeiteten Issues eines Services aus der DB.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllServiceIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Lese alle vom Bot bearbeiteten Issues eines Services aus")
	public ResponseEntity<?> getAllServiceIssues(
			@RequestParam(value = "repoService", required = true, defaultValue = "github") String repoService) {
		// Hole alle Issues und gebe sie zurück
		Iterable<RefactoredIssue> allIssues = repo.getAllServiceRefactorings(repoService);
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}
	
	/**
	 * Diese Methode holt alle bearbeiteten Issues eines Users aus der DB.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllUserIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Lese alle vom Bot bearbeiteten Issues eines Users aus")
	public ResponseEntity<?> getAllUserIssues(
			@RequestParam(value = "repoService", required = true, defaultValue = "github") String repoService,
			@RequestParam(value = "ownerName", required = true, defaultValue = "LHommeDeBat") String repoOwner) {
		// Hole alle Issues und gebe sie zurück
		Iterable<RefactoredIssue> allIssues = repo.getAllUserIssues(repoService, repoOwner);
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}

}
