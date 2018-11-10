package de.BA.refactoringBot.model.refactoredIssue;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@Transactional
public interface RefactoredIssueRepository extends CrudRepository<RefactoredIssue, Long> {

	@Query("SELECT a FROM RefactoredIssue a WHERE a.repoService=:repoService")
	public Iterable<RefactoredIssue> getAllServiceRefactorings(@Param("repoService") String repoService);

	@Query("SELECT a FROM RefactoredIssue a WHERE a.repoService=:repoService and a.repoOwner=:repoOwner")
	public Iterable<RefactoredIssue> getAllUserIssues(@Param("repoService") String repoService,
			@Param("repoOwner") String repoOwner);

}
