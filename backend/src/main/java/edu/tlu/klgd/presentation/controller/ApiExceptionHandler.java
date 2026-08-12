package edu.tlu.klgd.presentation.controller;

import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.MessageResponse;
import edu.tlu.klgd.infracstructure.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> badCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new MessageResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage(), request.getServletPath()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageResponse> apiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus())
            .body(new MessageResponse(exception.getStatus().value(), exception.getMessage(), request.getServletPath()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), request.getServletPath()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .toList();
        return ResponseEntity.badRequest()
            .body(new MessageResponse(HttpStatus.BAD_REQUEST.value(), ApiMessage.INVALID_DATA, request.getServletPath(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> constraint(ConstraintViolationException exception, HttpServletRequest request) {
        List<String> errors = exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ApiMessage.VALIDATION_FIELD_SEPARATOR + violation.getMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new MessageResponse(HttpStatus.BAD_REQUEST.value(), ApiMessage.INVALID_DATA, request.getServletPath(), errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MessageResponse> fileTooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new MessageResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(), ApiMessage.FILE_TOO_LARGE, request.getServletPath()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<MessageResponse> io(IOException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new MessageResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiMessage.FILE_PROCESSING_FAILED, request.getServletPath()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ApiMessage.VALIDATION_FIELD_SEPARATOR + error.getDefaultMessage();
    }
}
