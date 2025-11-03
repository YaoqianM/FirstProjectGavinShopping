package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.dto.common.DataResponse;
import com.bfs.hibernateprojectdemo.domain.Question;
import com.bfs.hibernateprojectdemo.dto.question.QuestionCreationRequest;
import com.bfs.hibernateprojectdemo.exception.TooManyQuestionsException;
import com.bfs.hibernateprojectdemo.service.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/question/all")
    @ResponseBody
    public ResponseEntity<DataResponse> getAllQuestions() {
        DataResponse body =  DataResponse.builder()
                .message("Success")
                .data(questionService.getAllQuestions())
                .build();
        return ResponseEntity
                .status(200)
                .body(body);
    }

    @GetMapping("/question/{id}")
    @ResponseBody
    public ResponseEntity<DataResponse> getQuestionById(@PathVariable int id) {
        DataResponse body = DataResponse.builder()
                .message("Success")
                .data(questionService.getQuestionById(id))
                .build();
        return ResponseEntity
                .status(200)
                .body(body);
    }

    @PostMapping("/question")
    @ResponseBody
    public ResponseEntity<DataResponse> addQuestion(@Valid @RequestBody QuestionCreationRequest request, BindingResult result) throws Exception {

        if (result.hasErrors()) {
            // TODO: you can throw an exception
            throw new Exception("description cannot be null");
        }

        Question question = Question.builder()
                .description(request.getDescription())
                .isActive(request.isActive())
                .build();

        questionService.addQuestion(question);

        DataResponse body = DataResponse.builder()
                .message("Success")
                .build();;

        return ResponseEntity
                .status(200)
                .body(body);
    }
}
