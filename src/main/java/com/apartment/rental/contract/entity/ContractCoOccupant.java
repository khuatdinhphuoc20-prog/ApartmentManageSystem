package com.apartment.rental.contract.entity;

import com.apartment.rental.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "contract_co_occupants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractCoOccupant extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Nationalized
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Nationalized
    @Column(name = "identity_document_no", nullable = false, length = 30)
    private String identityDocumentNo;

    @Column(name = "phone", nullable = true, length = 20)
    private String phone;
}

