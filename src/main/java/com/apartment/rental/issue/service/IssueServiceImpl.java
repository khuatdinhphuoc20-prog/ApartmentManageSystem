package com.apartment.rental.issue.service;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.exception.BusinessException;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.issue.dto.request.CreateIssueRequest;
import com.apartment.rental.issue.dto.response.ActiveContractOptionResponse;
import com.apartment.rental.issue.dto.response.IssueResponse;
import com.apartment.rental.issue.entity.Issue;
import com.apartment.rental.issue.entity.IssueStatus;
import com.apartment.rental.issue.repository.IssueRepository;
import com.apartment.rental.room.entity.Room;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

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
        // BR-ISSUE-02 & BR-ISSUE-03: Xem toàn bộ Issue của mình kể cả khi Contract đã ENDED
        return issueRepository.findAllByReporterIdWithContractAndRoom(reporterId)
                .stream()
                .map(this::toResponse)
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

        return toResponse(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnresolvedIssues() {
        return issueRepository.countByStatusIn(List.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));
    }

    private IssueResponse toResponse(Issue issue) {
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
}