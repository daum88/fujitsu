package com.deliveryfee.common;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Single, centralised error renderer. Every handled exception becomes an RFC 9457 {@link
 * ProblemDetail} body with a fixed message plus a {@code requestId} so a client error can be
 * correlated with the server log. Full stack traces are logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // region Public API
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(GlobalExceptionHandler::formatFieldError)
                        .toList();
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        var errors =
                ex.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage).toList();
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        var errors =
                ex.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .toList();
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'",
                List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter '" + ex.getParameterName() + "'",
                List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body", List.of());
    }

    @ExceptionHandler(ForbiddenVehicleUsageException.class)
    public ProblemDetail handleForbiddenVehicle(ForbiddenVehicleUsageException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NoWeatherDataException.class)
    public ProblemDetail handleNoWeatherData(NoWeatherDataException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error(
                "Unhandled exception request={} requestId={}",
                requestDescription(),
                currentRequestId(),
                ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", List.of());
    }

    // endregion

    // region Private helpers
    private static ProblemDetail problem(HttpStatus status, String detail, List<String> errors) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        var requestId = currentRequestId();
        if (requestId != null) {
            problemDetail.setProperty("requestId", requestId);
        }
        if (!errors.isEmpty()) {
            problemDetail.setProperty("errors", errors);
        }
        if (!status.is5xxServerError()) {
            log.warn(
                    "Request rejected: status={} detail={} request={} requestId={}",
                    status.value(),
                    detail,
                    requestDescription(),
                    requestId);
        }
        return problemDetail;
    }

    private static String currentRequestId() {
        return MDC.get(RequestIdFilter.MDC_KEY);
    }

    private static String requestDescription() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        var request = attributes.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
    // endregion
}
