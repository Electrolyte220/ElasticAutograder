package com.autograder.repository;

import com.autograder.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/* 
Chose to use Jpa for simplistic function calls that reduce SQL queries, but we can still introduce more complex queries where needed later :D
*/
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findTop5ByOrderByCreatedAtDesc();
}