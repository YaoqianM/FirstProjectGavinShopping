package com.bfs.hibernateprojectdemo.dao;

import com.bfs.hibernateprojectdemo.domain.Question;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionDao extends AbstractHibernateDao<Question> {

    public QuestionDao() {
        setClazz(Question.class);
    }

    public Question getQuestionById(int id) {
        return this.findById(id);
    }

    public List<Question> getAllQuestions() {
        return this.getAll();
    }

    public void addQuestion(Question question) {
        this.add(question);
    }

}
