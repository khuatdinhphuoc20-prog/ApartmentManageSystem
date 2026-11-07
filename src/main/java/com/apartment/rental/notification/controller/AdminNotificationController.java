package com.apartment.rental.notification.controller;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.notification.dto.ManualNotificationForm;
import com.apartment.rental.notification.service.ManualNotificationService;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);

    private final ManualNotificationService manualNotificationService;
    private final UserRepository userRepository;

    public AdminNotificationController(ManualNotificationService manualNotificationService, UserRepository userRepository) {
        this.manualNotificationService = manualNotificationService;
        this.userRepository = userRepository;
    }

    @InitBinder("notificationForm")
    public void configureFormBinding(WebDataBinder binder) {
        binder.setAllowedFields("title", "body", "recipientIds");
    }

    @GetMapping("/new")
    public String compose(Authentication authentication, Model model) {
        currentAdmin(authentication);
        model.addAttribute("notificationForm", new ManualNotificationForm());
        addRecipients(model);
        return "admin/notifications/new";
    }

    @PostMapping
    public String send(
        @Valid @ModelAttribute("notificationForm") ManualNotificationForm form,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        User admin = currentAdmin(authentication);
        if (bindingResult.hasErrors()) {
            addRecipients(model);
            return "admin/notifications/new";
        }

        try {
            List<Long> notificationIds = manualNotificationService.sendManual(
                admin, form.getTitle(), form.getBody(), form.getRecipientIds()
            );
            redirectAttributes.addFlashAttribute("sentRecipientCount", notificationIds.size());
            return "redirect:/admin/notifications/new";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("notification.invalid",
                "Không thể gửi thông báo. Vui lòng kiểm tra nội dung và chọn lại người nhận.");
        } catch (SecurityException exception) {
            throw new AccessDeniedException("Bạn không có quyền gửi thông báo.", exception);
        } catch (DataAccessException exception) {
            log.error("Could not save manual notifications sent by admin {}", admin.getId(), exception);
            bindingResult.reject("notification.saveFailed",
                "Chưa gửi được thông báo do lỗi lưu dữ liệu. Vui lòng thử lại.");
        }
        addRecipients(model);
        return "admin/notifications/new";
    }

    private User currentAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Vui lòng đăng nhập bằng tài khoản Admin.");
        }
        User admin = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đang đăng nhập."));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ Admin được gửi thông báo thủ công.");
        }
        return admin;
    }

    private void addRecipients(Model model) {
        List<RecipientOption> recipients = userRepository.findAllByRole(Role.USER).stream()
            .map(user -> new RecipientOption(user.getId(), user.getFullName(), user.getEmail()))
            .sorted(Comparator.comparing(RecipientOption::fullName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RecipientOption::id))
            .toList();
        model.addAttribute("recipientOptions", recipients);
    }

    public record RecipientOption(Long id, String fullName, String email) {
    }
}
