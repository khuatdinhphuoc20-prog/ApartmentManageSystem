package com.apartment.rental.issue.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.contract.entity.Contract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "issues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Nationalized
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Nationalized
    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IssueStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "handled_by_user_id", nullable = true)
    private User handledByUser;

    @Nationalized
    @Column(name = "admin_response", nullable = true, length = 4000)
    private String adminResponse;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "in_progress_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant inProgressAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "resolved_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant resolvedAt;
}

