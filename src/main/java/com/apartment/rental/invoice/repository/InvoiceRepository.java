package com.apartment.rental.invoice.repository;

import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    long countByStatus(InvoiceStatus status);

    @Query("select sum(invoice.totalAmount) from Invoice invoice where invoice.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);

    boolean existsByBillingPeriod_Id(Long billingPeriodId);

    boolean existsByContract_IdAndBillingPeriodEndDate(Long contractId, LocalDate billingPeriodEndDate);

    @EntityGraph(attributePaths = {"contract", "contract.room", "confirmedByUser"})
    List<Invoice> findAllByContract_Tenant_IdOrderByBillingPeriodEndDateDescIdDesc(Long tenantId);

    @EntityGraph(attributePaths = {"contract", "contract.room"})
    Optional<Invoice> findByIdAndContract_Tenant_Id(Long id, Long tenantId);


    @Query("select invoice from Invoice invoice where invoice.id = :invoiceId and invoice.contract.tenant.id = :tenantId")
    Optional<Invoice> findByIdAndContract_Tenant_IdForUpdate(
        @Param("invoiceId") Long invoiceId,
        @Param("tenantId") Long tenantId
    );

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant"})
    List<Invoice> findAllByStatusInOrderByDueAtAscIdAsc(List<InvoiceStatus> statuses);

    boolean existsByPaymentReference(String paymentReference);


    @Query("select invoice from Invoice invoice where invoice.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
        update Invoice invoice
        set invoice.status = :overdueStatus, invoice.updatedAt = :now
        where invoice.status = :unpaidStatus and invoice.dueAt <= :now
        """)
    int markUnpaidInvoicesOverdue(
        @Param("now") Instant now,
        @Param("unpaidStatus") InvoiceStatus unpaidStatus,
        @Param("overdueStatus") InvoiceStatus overdueStatus
    );
}
