package de.BA.refactoringBot.model.refactoredIssue;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

@Transactional
public interface RefactoredIssueRepository extends CrudRepository<RefactoredIssue, Long> {

}
