package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionItem, Long>{
    List<QuestionItem> findAll();
}