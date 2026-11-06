package com.apartment.rental.invoice.repository;

import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByBillingPeriod_Id(Long billingPeriodId);

    boolean existsByContract_IdAndBillingPeriodEndDate(Long contractId, LocalDate billingPeriodEndDate);

    @EntityGraph(attributePaths = {"contract", "contract.room", "confirmedByUser"})
    List<Invoice> findAllByContract_Tenant_IdOrderByBillingPeriodEndDateDescIdDesc(Long tenantId);

    @EntityGraph(attributePaths = {"contract", "contract.room"})
    Optional<Invoice> findByIdAndContract_Tenant_Id(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.id = :invoiceId and invoice.contract.tenant.id = :tenantId")
    Optional<Invoice> findByIdAndContract_Tenant_IdForUpdate(
        @Param("invoiceId") Long invoiceId,
        @Param("tenantId") Long tenantId
    );

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant"})
    List<Invoice> findAllByStatusInOrderByDueAtAscIdAsc(List<InvoiceStatus> statuses);

    boolean existsByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
}
