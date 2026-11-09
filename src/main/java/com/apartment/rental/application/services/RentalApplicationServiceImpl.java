package com.apartment.rental.application.services;


import com.apartment.rental.account.entity.User;
import com.apartment.rental.application.dto.RentalApplicationCreateRequest;
import com.apartment.rental.application.dto.RentalApplicationFilterRequest;
import com.apartment.rental.application.dto.RentalApplicationRejectRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.application.entity.RentalApplicationStatus;
import com.apartment.rental.application.exception.RentalApplicationException;
import com.apartment.rental.application.mapper.RentalApplicationMapper;
import com.apartment.rental.application.repository.RentalApplicationRepository;
import com.apartment.rental.contract.dto.request.CreateContractFromApplicationRequest;
import com.apartment.rental.contract.service.ContractService;
import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalApplicationServiceImpl implements RentalApplicationService {
    private final RentalApplicationRepository applicationRepository;
    private final RentalApplicationMapper applicationMapper;
    private final EntityManager entityManager;
    private final ContractService contractService;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional
    public RentalApplicationResponse createApplication(RentalApplicationCreateRequest request, Long applicantId) {
        if (applicantId == null) {
            throw new RentalApplicationException.Validation("Applicant ID cannot be null");
        }

        Room room = entityManager.find(Room.class, request.getRoomId());
        if (room == null) {
            throw new RentalApplicationException.NotFound("Room not found with ID: " + request.getRoomId());
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RentalApplicationException.Conflict("Phòng hiện không còn trống để đăng ký thuê");
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate maxStartDate = today.plusDays(10);
        LocalDate startDate = request.getRequestedStartDate();
        if (startDate == null || startDate.isBefore(today) || startDate.isAfter(maxStartDate)) {
            throw new RentalApplicationException.Validation(
                    "Ngày bắt đầu thuê phải từ hôm nay (" + today + ") đến tối đa 10 ngày tới (" + maxStartDate + ")"
            );
        }

        LocalDate endDate;
        if (request.getEndMonth() != null && request.getEndYear() != null) {
            endDate = LocalDate.of(request.getEndYear(), request.getEndMonth(), 10);
        } else if (request.getEndMonth() != null || request.getEndYear() != null) {
            throw new RentalApplicationException.Validation("Vui lòng chọn đầy đủ cả tháng và năm kết thúc, hoặc để cả hai ở tùy chọn mặc định.");
        } else {
            int months = (request.getDurationMonths() != null && request.getDurationMonths() > 0)
                    ? request.getDurationMonths() : 1;
            endDate = startDate.plusMonths(months).withDayOfMonth(10);
            if (!endDate.isAfter(startDate)) {
                endDate = endDate.plusMonths(1);
            }
        }

        if (!endDate.isAfter(startDate)) {
            throw new RentalApplicationException.Validation("Ngày kết thúc (" + endDate + ") phải sau ngày bắt đầu (" + startDate + ")");
        }

        boolean hasPending = applicationRepository.existsByApplicantIdAndRoomIdAndStatus(
                applicantId, request.getRoomId(), RentalApplicationStatus.PENDING
        );
        if (hasPending) {
            throw new RentalApplicationException.Conflict("Bạn đã có một đơn đăng ký đang chờ xử lý cho phòng này");
        }

        User applicant = entityManager.getReference(User.class, applicantId);
        RentalApplication application = new RentalApplication();
        application.setApplicant(applicant);
        application.setRoom(room);
        application.setRequestedStartDate(startDate);
        application.setRequestedEndDate(endDate);
        application.setStatus(RentalApplicationStatus.PENDING);

        RentalApplication saved = applicationRepository.save(application);
        log.info("Application #{} created for user {} on room {}", saved.getId(), applicantId, room.getRoomCode());
        return applicationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalApplicationResponse> getMyApplications(Long applicantId) {
        return applicationMapper.toResponseList(
                applicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RentalApplicationResponse getApplicationById(Long applicationId) {
        RentalApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RentalApplicationException.NotFound(applicationId));
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public void withdrawApplication(Long applicationId, Long applicantId) {
        RentalApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RentalApplicationException.NotFound(applicationId));
        if (!application.getApplicant().getId().equals(applicantId)) {
            throw new RentalApplicationException.InvalidState("Bạn chỉ có thể rút đơn của chính mình");
        }
        if (application.getStatus() != RentalApplicationStatus.PENDING) {
            throw new RentalApplicationException.InvalidState("Đơn không còn ở trạng thái chờ duyệt để rút");
        }
        User applicant = entityManager.getReference(User.class, applicantId);
        application.setStatus(RentalApplicationStatus.WITHDRAWN);
        application.setProcessedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        application.setProcessedByUser(applicant);
        application.setProcessingNote("Người thuê rút đơn");
        applicationRepository.save(application);
        log.info("Application #{} withdrawn by user {}", applicationId, applicantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalApplicationResponse> getApplicationsForAdmin(RentalApplicationFilterRequest filter) {
        Pageable pageable = PageRequest.of(Math.max(0, filter.getPage()), Math.max(1, filter.getSize()));
        return applicationRepository.findApplicationsForAdmin(filter.getStatus(), filter.getRoomId(), pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    @Transactional
    public void rejectApplication(Long applicationId, RentalApplicationRejectRequest request, Long adminId) {
        RentalApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RentalApplicationException.NotFound(applicationId));
        if (application.getStatus() != RentalApplicationStatus.PENDING) {
            throw new RentalApplicationException.InvalidState("Chỉ có thể từ chối đơn đang chờ duyệt");
        }
        User admin = entityManager.getReference(User.class, adminId);
        application.setStatus(RentalApplicationStatus.REJECTED);
        application.setProcessedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        application.setProcessedByUser(admin);
        application.setProcessingNote(request.getReason());
        applicationRepository.save(application);
        log.info("Application #{} rejected by admin {}", applicationId, adminId);
    }

    @Override
    @Transactional
    public void rejectOtherPendingApplications(Long roomId, Long excludedApplicationId) {
        List<RentalApplication> pendingApps = applicationRepository.findByRoomIdAndStatus(roomId, RentalApplicationStatus.PENDING);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        for (RentalApplication app : pendingApps) {
            if (excludedApplicationId != null && app.getId().equals(excludedApplicationId)) {
                continue;
            }
            app.setStatus(RentalApplicationStatus.REJECTED);
            app.setProcessedAt(now);
            app.setProcessedByUser(null);
            app.setProcessingNote("Phòng đã được thuê");
            applicationRepository.save(app);
            log.info("Auto-rejected competing application #{} for room {}", app.getId(), roomId);
        }
    }

    @Override
    @Transactional
    public void approveApplication(Long applicationId, Long adminId) {
        RentalApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RentalApplicationException.NotFound(applicationId));

        if (application.getStatus() != RentalApplicationStatus.PENDING) {
            throw new RentalApplicationException.InvalidState("Chỉ có thể duyệt đơn đang ở trạng thái chờ duyệt");
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (application.getRequestedStartDate().isBefore(today)) {
            application.setStatus(RentalApplicationStatus.REJECTED);
            application.setProcessedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
            application.setProcessedByUser(null);
            application.setProcessingNote("Đơn hết hạn do ngày bắt đầu thuê đã qua");
            applicationRepository.save(application);
            throw new RentalApplicationException.InvalidState("Đơn đã hết hạn do ngày bắt đầu thuê đã qua");
        }

        CreateContractFromApplicationRequest contractReq = new CreateContractFromApplicationRequest(
                application.getId(),
                application.getApplicant().getId(),
                application.getRoom().getId(),
                application.getRequestedStartDate(),
                application.getRequestedEndDate()
        );
        contractService.createContractFromApplication(contractReq, adminId);

        User admin = entityManager.getReference(User.class, adminId);
        application.setStatus(RentalApplicationStatus.APPROVED);
        application.setProcessedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        application.setProcessedByUser(admin);
        application.setProcessingNote("Đã duyệt");
        applicationRepository.save(application);
        rejectOtherPendingApplications(application.getRoom().getId(), application.getId());
        log.info("WF-01 completed: Application #{} approved, Contract created for room {}", applicationId, application.getRoom().getRoomCode());
    }
}
