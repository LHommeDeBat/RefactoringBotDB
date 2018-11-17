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
 * This class offers an CRUD-Interface as a REST-API for the refactored issues.
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
	 * This method returns all refactored issues from the database.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all refactored issues.")
	public ResponseEntity<?> getAllIssues() {
		Iterable<RefactoredIssue> allIssues = repo.findAll();
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}

	/**
	 * This method returns all refactored issues of a filehoster from the database.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllServiceIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all refactored issues from a specific filehoster.")
	public ResponseEntity<?> getAllServiceIssues(
			@RequestParam(value = "repoService", required = true, defaultValue = "github") String repoService) {
		Iterable<RefactoredIssue> allIssues = repo.getAllServiceRefactorings(repoService);
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}

	/**
	 * This methor returns all refactored issues of a specific user from a specific
	 * filehoster.
	 * 
	 * @return allIssues
	 */
	@RequestMapping(value = "/getAllUserIssues", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all refactored issues of a specific user from a specific filehoster.")
	public ResponseEntity<?> getAllUserIssues(
			@RequestParam(value = "repoService", required = true, defaultValue = "github") String repoService,
			@RequestParam(value = "ownerName", required = true, defaultValue = "LHommeDeBat") String repoOwner) {
		Iterable<RefactoredIssue> allIssues = repo.getAllUserIssues(repoService, repoOwner);
		return new ResponseEntity<Iterable<RefactoredIssue>>(allIssues, HttpStatus.OK);
	}

}
