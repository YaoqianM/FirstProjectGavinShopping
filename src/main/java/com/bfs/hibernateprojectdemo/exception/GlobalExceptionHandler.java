package com.bfs.hibernateprojectdemo.exception;

import com.bfs.hibernateprojectdemo.dto.common.DataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    // add your own exception handlers
    @ExceptionHandler(TooManyQuestionsException.class)
    public ResponseEntity<DataResponse> handleTooManyQuestionsExceptions(TooManyQuestionsException ex) {
        DataResponse body = DataResponse.builder().message(ex.getMessage()).build();
        return ResponseEntity.status(400).body(body);
    }
}
