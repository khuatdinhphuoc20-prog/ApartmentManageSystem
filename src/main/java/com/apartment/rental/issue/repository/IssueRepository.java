package com.apartment.rental.issue.repository;

import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.issue.entity.Issue;
import com.apartment.rental.issue.entity.IssueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    @Query("""
            SELECT i FROM Issue i
            JOIN FETCH i.contract c
            JOIN FETCH c.room r
            WHERE i.reporter.id = :reporterId
            ORDER BY i.createdAt DESC
            """)
    List<Issue> findAllByReporterIdWithContractAndRoom(@Param("reporterId") Long reporterId);

    @Query("""
            SELECT i FROM Issue i
            JOIN FETCH i.contract c
            JOIN FETCH c.room r
            WHERE i.id = :issueId AND i.reporter.id = :reporterId
            """)
    Optional<Issue> findDetailByIdAndReporterId(
            @Param("issueId") Long issueId,
            @Param("reporterId") Long reporterId
    );

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.room r
            WHERE c.tenant.id = :tenantId
              AND c.status = com.apartment.rental.contract.entity.ContractStatus.ACTIVE
            ORDER BY r.roomCode ASC
            """)
    List<Contract> findActiveContractsByTenantId(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.room r
            WHERE c.id = :contractId
              AND c.tenant.id = :tenantId
              AND c.status = com.apartment.rental.contract.entity.ContractStatus.ACTIVE
            """)
    Optional<Contract> findActiveContractByIdAndTenantId(
            @Param("contractId") Long contractId,
            @Param("tenantId") Long tenantId
    );

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

    @Query("""
            SELECT i FROM Issue i
            JOIN FETCH i.contract c
            JOIN FETCH c.room r
            JOIN FETCH i.reporter u
            LEFT JOIN FETCH i.handledByUser h
            WHERE i.id = :issueId
            """)
    Optional<Issue> findAdminDetailById(@Param("issueId") Long issueId);

    long countByStatusIn(Collection<IssueStatus> statuses);
}