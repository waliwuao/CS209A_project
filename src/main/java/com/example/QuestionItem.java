package com.example;

import java.util.List;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;

@Entity
@Table
public class QuestionItem {
    private List<String> tags;
    private Owner owner;
    private boolean is_answered;
    private int view_count;
    private Integer accepted_answer_id;
    private int answer_count;
    private int score;
    private int creation_date;
    @Id
    private int question_id;
    private String title;
    private String body;
    // 所有字段的getter/setter
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
    public boolean isIs_answered() { return is_answered; }
    public void setIs_answered(boolean is_answered) { this.is_answered = is_answered; }
    public int getView_count() { return view_count; }
    public void setView_count(int view_count) { this.view_count = view_count; }
    public Integer getAccepted_answer_id() { return accepted_answer_id; }
    public void setAccepted_answer_id(Integer accepted_answer_id) { this.accepted_answer_id = accepted_answer_id; }
    public int getAnswer_count() { return answer_count; }
    public void setAnswer_count(int answer_count) { this.answer_count = answer_count; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCreation_date() { return creation_date; }
    public void setCreation_date(int creation_date) { this.creation_date = creation_date; }
    public int getQuestion_id() { return question_id; }
    public void setQuestion_id(int question_id) { this.question_id = question_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return this.body; }
}