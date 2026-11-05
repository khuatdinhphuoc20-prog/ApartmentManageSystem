package com.apartment.rental.account.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public RedirectView handleException(IllegalArgumentException e, RedirectAttributes attributes) {
        log.error("Handling IllegalArgumentException: {}", e.getMessage());
        attributes.addFlashAttribute("error", e.getMessage());

        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("/");
        redirectView.setContextRelative(true);
        return redirectView;
    }

    @ExceptionHandler(BusinessException.class)
    public String handle(BusinessException e, HttpServletRequest request, RedirectAttributes attributes) {
        log.error("Handling Business Exception: {}", e.getMessage());
        attributes.addFlashAttribute("error", e.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/register?error";
    }

    @ExceptionHandler(Exception.class)
    public String handle(Exception e) throws Exception {
        // Giữ nguyên các mã lỗi HTTP chuẩn của Spring (như 403 Forbidden ở LayoutController)
        if (e instanceof ErrorResponse) {
            throw e;
        }
        log.error("Handling Exception: {}", e.getMessage());
        return "error";
    }
}