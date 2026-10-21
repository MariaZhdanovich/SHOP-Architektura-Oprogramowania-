package edu.arch.shop.notification.service;

import edu.arch.shop.notification.dto.NotificationRequest;
import edu.arch.shop.notification.dto.NotificationResponse;
import edu.arch.shop.notification.entity.Notification;
import edu.arch.shop.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponse> findAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse findById(Long id) {
        return toResponse(findNotification(id));
    }

    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        validate(request);
        Notification notification = new Notification();
        notification.setMessage(request.message());
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse update(Long id, NotificationRequest request) {
        validate(request);
        Notification notification = findNotification(id);
        notification.setMessage(request.message());

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(Long id) {
        Notification notification = findNotification(id);
        notificationRepository.delete(notification);
    }

    private Notification findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validate(NotificationRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification message is required"
            );
        }

        if (request.message().length() > 300) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification message is too long"
            );
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage()
        );
    }
}