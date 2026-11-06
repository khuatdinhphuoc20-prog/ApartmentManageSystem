package com.apartment.rental.application.services;

import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.application.entity.RentalApplicationStatus;
import com.apartment.rental.application.repository.RentalApplicationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredApplicationScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final RentalApplicationRepository applicationRepository;


    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void scanAndRejectExpiredApplications() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<RentalApplication> expiredApps = applicationRepository.findByStatusAndRequestedStartDateLessThan(
                RentalApplicationStatus.PENDING, today
        );

        if (expiredApps.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING applications to reject", expiredApps.size());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        for (RentalApplication app : expiredApps) {
            app.setStatus(RentalApplicationStatus.REJECTED);
            app.setProcessedAt(now);
            app.setProcessedByUser(null);
            app.setProcessingNote("Đơn hết hạn do ngày bắt đầu thuê đã qua");
            applicationRepository.save(app);
            log.info("Auto-rejected expired application ID: {}", app.getId());
        }
    }
}