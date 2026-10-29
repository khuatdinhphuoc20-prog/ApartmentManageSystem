package com.apartment.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.billing.entity.BillingCycle;
import com.apartment.rental.billing.entity.BillingCycleItem;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractCoOccupant;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.issue.entity.Issue;
import com.apartment.rental.notification.entity.Notification;
import com.apartment.rental.room.entity.Room;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseFoundationMappingTests {

    @Test
    void everyDesignedTableHasAnAuditableEntity() {
        Map<Class<?>, String> entities = Map.of(
            User.class, "users",
            Room.class, "rooms",
            RentalApplication.class, "rental_applications",
            Contract.class, "contracts",
            ContractCoOccupant.class, "contract_co_occupants",
            BillingCycle.class, "billing_cycles",
            BillingCycleItem.class, "billing_cycle_items",
            Invoice.class, "invoices",
            Issue.class, "issues",
            Notification.class, "notifications"
        );

        assertEquals(10, entities.size());
        entities.forEach((entityClass, tableName) -> {
            assertTrue(entityClass.isAnnotationPresent(Entity.class), entityClass.getName());
            assertTrue(AuditableEntity.class.isAssignableFrom(entityClass), entityClass.getName());
            assertEquals(tableName, entityClass.getAnnotation(Table.class).name());
        });
    }
}
