package com.apartment.rental.notification.controller;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.notification.dto.NotificationView;
import com.apartment.rental.notification.service.NotificationInboxService;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private static final int PAGE_SIZE = 20;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NotificationInboxService notificationInboxService;
    private final UserRepository userRepository;

    public NotificationController(NotificationInboxService notificationInboxService, UserRepository userRepository) {
        this.notificationInboxService = notificationInboxService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String inbox(
        Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model
    ) {
        requireValidPage(page);
        User actor = currentUser(authentication);
        Page<NotificationView> notifications = notificationInboxService.listMine(actor, PageRequest.of(page, PAGE_SIZE));
        if (page > 0 && page >= notifications.getTotalPages()) {
            return "redirect:/notifications?page=" + Math.max(0, notifications.getTotalPages() - 1);
        }
        addLayoutModel(actor, page, model);
        model.addAttribute("notificationPage", notifications);
        return "notification/inbox";
    }

    @GetMapping("/{id}")
    public String detail(
        @PathVariable("id") Long id,
        Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model
    ) {
        requireValidPage(page);
        User actor = currentUser(authentication);
        try {
            model.addAttribute("notification", notificationInboxService.getMine(actor, id));
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo.");
        }
        addLayoutModel(actor, page, model);
        return "notification/detail";
    }

    @PostMapping("/{id}/read")
    public String markRead(
        @PathVariable("id") Long id,
        Authentication authentication,
        @RequestParam(name = "page", defaultValue = "0") int page,
        RedirectAttributes redirectAttributes
    ) {
        requireValidPage(page);
        User actor = currentUser(authentication);
        try {
            notificationInboxService.markRead(actor, id);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo.");
        }
        redirectAttributes.addFlashAttribute("notificationMessage", "Đã đánh dấu thông báo là đã đọc.");
        return "redirect:/notifications/" + id + "?page=" + page;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Vui lòng đăng nhập để xem thông báo.");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đang đăng nhập."));
    }

    private static void requireValidPage(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số trang không hợp lệ.");
        }
    }

    private static void addLayoutModel(User actor, int page, Model model) {
        boolean admin = actor.getRole() == Role.ADMIN;
        model.addAttribute("notificationAdmin", admin);
        model.addAttribute("notificationLayout", admin ? "fragments/admin" : "fragments/customer");
        model.addAttribute("notificationZone", DISPLAY_ZONE);
        model.addAttribute("inboxPage", page);
    }
}
