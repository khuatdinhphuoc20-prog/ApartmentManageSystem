
package com.apartment.rental.contract.scheduler;

import com.apartment.rental.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class ContractScheduler {

    private final ContractService contractService;

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    // UC20 + UC21 - Daily contract lifecycle
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void processContractLifecycle() {

        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        // UC20: SCHEDULED -> ACTIVE
        contractService.activateScheduledContracts(today);

        // UC21: ACTIVE -> ENDED
        contractService.expireActiveContracts(today);
    }
}
