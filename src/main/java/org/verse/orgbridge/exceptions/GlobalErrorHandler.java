package org.verse.orgbridge.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleAuthFailure(
            AuthenticationFailedException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Authentication Failed");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "AUTHENTICATION_FAILED");

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem));
    }

    @ExceptionHandler(SalesforceAuthException.class)
    public ResponseEntity<ProblemDetail> handleSalesforceAuth(
            SalesforceAuthException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Salesforce Authentication Failed");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", ex.getCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Authentication Failed");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "AUTHENTICATION_FAILED");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserExists(UserAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("User Already Exists");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "USER_ALREADY_EXISTS");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
