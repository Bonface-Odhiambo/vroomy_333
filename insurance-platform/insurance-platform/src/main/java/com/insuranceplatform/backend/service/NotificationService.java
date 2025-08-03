package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.entity.Notification;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotification(User sender, User receiver, String message) {
        Notification notification = Notification.builder()
                .sender(sender)
                .receiver(receiver)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByReceiverOrderByCreatedAtDesc(user);
    }
}