package com.apartment.rental.application.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(basePackages = "com.apartment.rental.application.controller")
public class RentalApplicationExceptionHandler {

    @ExceptionHandler(RentalApplicationException.class)
    public String handleRentalApplicationException(
            RentalApplicationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Business exception in RentalApplication: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
