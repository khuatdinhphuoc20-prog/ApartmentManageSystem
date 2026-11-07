package com.apartment.rental.issue.service;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.exception.BusinessException;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.issue.dto.request.CreateIssueRequest;
import com.apartment.rental.issue.dto.request.UpdateIssueStatusRequest;
import com.apartment.rental.issue.dto.response.ActiveContractOptionResponse;
import com.apartment.rental.issue.dto.response.AdminIssueResponse;
import com.apartment.rental.issue.dto.response.IssueResponse;
import com.apartment.rental.issue.entity.Issue;
import com.apartment.rental.issue.entity.IssueStatus;
import com.apartment.rental.issue.event.IssueStatusUpdatedEvent;
import com.apartment.rental.issue.repository.IssueRepository;
import com.apartment.rental.room.entity.Room;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ActiveContractOptionResponse> getActiveContractsForTenant(Long tenantId) {
        return issueRepository.findActiveContractsByTenantId(tenantId)
                .stream()
                .map(c -> new ActiveContractOptionResponse(
                        c.getId(),
                        c.getRoom().getId(),
                        c.getRoom().getRoomCode(),
                        c.getRoom().getFloorNumber(),
                        c.getStartDate(),
                        c.getPlannedEndDate()
                ))
                .toList();
    }

    @Override
    @Transactional
    public Long createIssue(Long reporterId, CreateIssueRequest request) {
        String title = request.getTitle().trim();
        String description = request.getDescription().trim();

        if (title.isEmpty()) {
            throw new BusinessException(400, "Tiêu đề phản ánh không được để trống!");
        }
        if (description.isEmpty()) {
            throw new BusinessException(400, "Nội dung chi tiết không được để trống!");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin người dùng!"));

        //Chỉ người đại diện có hợp đồng ACTIVE mới được gửi Issue mới
        Contract activeContract = issueRepository
                .findActiveContractByIdAndTenantId(request.getContractId(), reporterId)
                .orElseThrow(() -> new BusinessException(
                        403,
                        "Chỉ người đại diện của hợp đồng đang hiệu lực (ACTIVE) mới được gửi phản ánh/yêu cầu sửa chữa!"
                ));

        Issue newIssue = new Issue();
        newIssue.setContract(activeContract);
        newIssue.setReporter(reporter);
        newIssue.setTitle(title);
        newIssue.setDescription(description);
        newIssue.setStatus(IssueStatus.OPEN);

        Issue saved = issueRepository.save(newIssue);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueResponse> getMyIssues(Long reporterId) {
        return issueRepository.findAllByReporterIdWithContractAndRoom(reporterId)
                .stream()
                .map(this::toTenantResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse getMyIssueDetail(Long reporterId, Long issueId) {
        Issue issue = issueRepository.findDetailByIdAndReporterId(issueId, reporterId)
                .orElseThrow(() -> new BusinessException(
                        404,
                        "Không tìm thấy phản ánh hoặc bạn không có quyền xem phản ánh này!"
                ));

        return toTenantResponse(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminIssueResponse> getAllIssuesForAdmin(IssueStatus statusFilter) {
        return issueRepository.findAllForAdmin(statusFilter)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminIssueResponse getIssueDetailForAdmin(Long issueId) {
        Issue issue = issueRepository.findAdminDetailById(issueId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy phản ánh #" + issueId + "!"));
        return toAdminResponse(issue);
    }

    @Override
    @Transactional
    public boolean updateIssueByAdmin(Long issueId, Long adminUserId, UpdateIssueStatusRequest request) {
        Issue issue = issueRepository.findAdminDetailById(issueId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy phản ánh #" + issueId + "!"));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản Admin!"));

        IssueStatus currentStatus = issue.getStatus();
        IssueStatus targetStatus = request.getStatus();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        if (currentStatus == IssueStatus.RESOLVED) {
            throw new BusinessException(400, "Phản ánh này đã ở trạng thái Đã giải quyết (RESOLVED), không thể cập nhật lại!");
        }

        // Bảo đảm vòng đời một chiều: OPEN -> IN_PROGRESS -> RESOLVED
        if (currentStatus == IssueStatus.OPEN) {
            if (targetStatus != IssueStatus.IN_PROGRESS) {
                throw new BusinessException(
                        400,
                        "Phản ánh mới (OPEN) phải được chuyển sang trạng thái Đang xử lý (IN_PROGRESS) trước khi hoàn tất!"
                );
            }
            issue.setStatus(IssueStatus.IN_PROGRESS);
            issue.setHandledByUser(adminUser);
            issue.setInProgressAt(now);
            issue.setResolvedAt(null);
        } else if (currentStatus == IssueStatus.IN_PROGRESS) {
            if (targetStatus == IssueStatus.OPEN) {
                throw new BusinessException(400, "Không được phép chuyển ngược trạng thái từ IN_PROGRESS về OPEN!");
            }
            issue.setHandledByUser(adminUser);
            if (targetStatus == IssueStatus.RESOLVED) {
                issue.setStatus(IssueStatus.RESOLVED);
                // Đảm bảo thời điểm resolved_at không nhỏ hơn in_progress_at
                Instant resolvedTime = (issue.getInProgressAt() != null && now.isBefore(issue.getInProgressAt()))
                        ? issue.getInProgressAt()
                        : now;
                issue.setResolvedAt(resolvedTime);
            }
        }

        //Nếu chuỗi rỗng thì lưu NULL thay vì chuỗi trắng
        String trimmedResponse = request.getAdminResponse() != null ? request.getAdminResponse().trim() : "";
        issue.setAdminResponse(trimmedResponse.isEmpty() ? null : trimmedResponse);

        issueRepository.saveAndFlush(issue);

        //Phát sự kiện thông báo mềm, lỗi không làm hỏng cập nhật Issue
        if (currentStatus != issue.getStatus()) {
            publishIssueNotificationSafely(issue);
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnresolvedIssues() {
        return issueRepository.countByStatusIn(List.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));
    }

    private void publishIssueNotificationSafely(Issue issue) {
        try {
            String roomCode = issue.getContract().getRoom().getRoomCode();
            String eventKey = "ISSUE_" + issue.getStatus().name() + ":" + issue.getId();
            String statusText = issue.getStatus() == IssueStatus.IN_PROGRESS ? "đang được xử lý" : "đã được giải quyết";
            String title = "Cập nhật phản ánh #" + issue.getId() + " (Phòng " + roomCode + ")";
            String body = "Phản ánh \"" + issue.getTitle() + "\" của bạn hiện " + statusText + ".";

            eventPublisher.publishEvent(new IssueStatusUpdatedEvent(
                    issue.getId(),
                    issue.getReporter().getId(),
                    issue.getStatus(),
                    eventKey,
                    title,
                    body
            ));
        } catch (Exception ex) {
            log.error("Không thể phát sự kiện thông báo cho Issue #{}: {}", issue.getId(), ex.getMessage());
        }
    }

    private IssueResponse toTenantResponse(Issue issue) {
        Contract contract = issue.getContract();
        Room room = contract.getRoom();
        return new IssueResponse(
                issue.getId(),
                contract.getId(),
                contract.getStatus().name(),
                room.getRoomCode(),
                room.getFloorNumber(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getAdminResponse(),
                issue.getCreatedAt(),
                issue.getInProgressAt(),
                issue.getResolvedAt()
        );
    }

    private AdminIssueResponse toAdminResponse(Issue issue) {
        Contract contract = issue.getContract();
        Room room = contract.getRoom();
        User reporter = issue.getReporter();
        User handler = issue.getHandledByUser();

        return new AdminIssueResponse(
                issue.getId(),
                contract.getId(),
                contract.getStatus().name(),
                room.getRoomCode(),
                room.getFloorNumber(),
                reporter.getId(),
                reporter.getFullName(),
                reporter.getEmail(),
                reporter.getPhone(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                handler != null ? handler.getFullName() : null,
                issue.getAdminResponse(),
                issue.getCreatedAt(),
                issue.getInProgressAt(),
                issue.getResolvedAt()
        );
    }
}