package com.bfs.hibernateprojectdemo.service;

import com.bfs.hibernateprojectdemo.dao.QuestionDao;
import com.bfs.hibernateprojectdemo.domain.Question;
import com.bfs.hibernateprojectdemo.exception.TooManyQuestionsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class QuestionService {
    private QuestionDao questionDao;

    @Autowired
    public void setQuestionDao(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }

    @Transactional
    public List<Question> getAllQuestions() {
        return questionDao.getAllQuestions();
    }

    @Transactional
    public Question getQuestionById(int id) {
        return questionDao.getQuestionById(id);
    }

    @Transactional
    public void addQuestion(Question... questions) throws TooManyQuestionsException {
        //just a demo, doesn't make sense
        if(!questionDao.getAllQuestions().isEmpty()) {
            System.out.println(questionDao.getAllQuestions());
            throw new TooManyQuestionsException("Too many questions");
        }
        for (Question q : questions) {
            questionDao.addQuestion(q);
        }
    }

}
