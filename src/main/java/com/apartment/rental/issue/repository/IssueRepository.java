package com.apartment.rental.issue.repository;

import com.apartment.rental.issue.entity.Issue;
import com.apartment.rental.issue.entity.IssueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    @EntityGraph(attributePaths = {"contract", "contract.room"})
    List<Issue> findAllByReporter_IdOrderByCreatedAtDesc(Long reporterId);

    @EntityGraph(attributePaths = {"contract", "contract.room"})
    Optional<Issue> findByIdAndReporter_Id(Long id, Long reporterId);

    @Override
    @EntityGraph(attributePaths = {"contract.room", "reporter", "handledByUser"})
    Optional<Issue> findById(Long id);

    @Query("""
            SELECT i FROM Issue i
            JOIN FETCH i.contract c
            JOIN FETCH c.room r
            JOIN FETCH i.reporter u
            LEFT JOIN FETCH i.handledByUser h
            WHERE (:status IS NULL OR i.status = :status)
            ORDER BY
                CASE i.status
                    WHEN com.apartment.rental.issue.entity.IssueStatus.OPEN THEN 1
                    WHEN com.apartment.rental.issue.entity.IssueStatus.IN_PROGRESS THEN 2
                    ELSE 3
                END ASC,
                i.createdAt DESC
            """)
    List<Issue> findAllForAdmin(@Param("status") IssueStatus status);

    long countByStatusIn(Collection<IssueStatus> statuses);
}