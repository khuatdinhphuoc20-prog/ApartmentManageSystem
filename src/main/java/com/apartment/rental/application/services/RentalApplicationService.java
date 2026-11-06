package com.apartment.rental.application.services;

import com.apartment.rental.application.dto.RentalApplicationCreateRequest;
import com.apartment.rental.application.dto.RentalApplicationFilterRequest;
import com.apartment.rental.application.dto.RentalApplicationRejectRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RentalApplicationService {
    RentalApplicationResponse createApplication(RentalApplicationCreateRequest request, Long applicantId);

    List<RentalApplicationResponse> getMyApplications(Long applicantId);

    RentalApplicationResponse getApplicationById(Long applicationId);

    void withdrawApplication(Long applicationId, Long applicantId);

    Page<RentalApplicationResponse> getApplicationsForAdmin(RentalApplicationFilterRequest filter);

    void rejectApplication(Long applicationId, RentalApplicationRejectRequest request, Long adminId);

    void rejectOtherPendingApplications(Long roomId, Long excludedApplicationId);

    void approveApplication(Long applicationId, Long adminId);
}
