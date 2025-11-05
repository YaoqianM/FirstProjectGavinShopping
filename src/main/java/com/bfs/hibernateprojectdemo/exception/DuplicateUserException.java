package com.bfs.hibernateprojectdemo.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String msg) { super(msg); }
}