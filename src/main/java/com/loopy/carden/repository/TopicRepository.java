package com.loopy.carden.repository;

import com.loopy.carden.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    @Query("SELECT t FROM Topic t WHERE t.parentTopic IS NULL ORDER BY t.displayOrder ASC")
    List<Topic> findRootTopicsOrderByDisplayOrder();
    
    @Query("SELECT t FROM Topic t WHERE t.parentTopic = :parentTopic ORDER BY t.displayOrder ASC")
    List<Topic> findChildTopicsOrderByDisplayOrder(@Param("parentTopic") Topic parentTopic);
    
    @Query("SELECT t FROM Topic t WHERE t.isSystemTopic = true ORDER BY t.displayOrder ASC")
    List<Topic> findSystemTopicsOrderByDisplayOrder();
    
    @Query("SELECT t FROM Topic t WHERE t.isSystemTopic = false ORDER BY t.displayOrder ASC")
    List<Topic> findNonSystemTopicsOrderByDisplayOrder();
}


