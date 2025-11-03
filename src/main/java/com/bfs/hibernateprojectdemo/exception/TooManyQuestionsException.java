package com.bfs.hibernateprojectdemo.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TooManyQuestionsException extends Exception{

    // allow you to pass in extra detail if needed
    public TooManyQuestionsException(String detail) {
        super(detail);
    }
}
