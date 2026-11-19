package com.example.nerdflix.exception;

import com.example.nerdflix.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class GlobalExceptionHandler {//Checked: Use one handler for all


    @ExceptionHandler({
                    StatNotFoundException.class,
                    VideoNotFoundException.class,
                    CategoryNotFoundException.class,
                    UserNotFoundException.class,
                    RefreshTokenNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(RuntimeException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    // handler for UserAlreadyExistsException and use the global handler for similar
    // exception types (same http response returned)

    @ExceptionHandler({UserAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleBadRequests(RuntimeException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(RuntimeException ex) {
        return buildResponse(ex, HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<ErrorResponse> buildResponse(Exception ex, HttpStatus status) {
        ErrorResponse resp = new ErrorResponse();
        resp.setMessage(ex.getMessage());
        resp.setTimestamp(System.currentTimeMillis());
        resp.setStatus(status.value());
        return new ResponseEntity<>(resp, status);
    }

    //handler for all exceptions that are not handled above (global handler)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
