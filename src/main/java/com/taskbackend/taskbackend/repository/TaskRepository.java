package com.taskbackend.taskbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskbackend.taskbackend.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
