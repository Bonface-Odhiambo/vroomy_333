package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Notification;
import com.insuranceplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Find all notifications for a specific user, ordered by most recent
    List<Notification> findByReceiverOrderByCreatedAtDesc(User receiver);
}