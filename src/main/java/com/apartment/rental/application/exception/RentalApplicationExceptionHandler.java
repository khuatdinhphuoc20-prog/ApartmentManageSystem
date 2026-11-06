package com.apartment.rental.application.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.apartment.rental.application.controller")
public class RentalApplicationExceptionHandler {

    @ExceptionHandler(RentalApplicationException.class)
    public String handleRentalApplicationException(
            RentalApplicationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Business exception in RentalApplication: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String roomId = request.getParameter("roomId");
        if (roomId != null && roomId.matches("[1-9]\\d*")) {
            return "redirect:/customer/applications/apply/" + roomId;
        }
        return "redirect:/customer/applications";
    }
}
