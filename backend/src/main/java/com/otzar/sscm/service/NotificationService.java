package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.Notification;
import com.otzar.sscm.entities.NotificationType;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.AdminRepository;
import com.otzar.sscm.repository.NotificationRepository;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public NotificationService(NotificationRepository notificationRepository, ClientRepository clientRepository,
                               UserRepository userRepository, AdminRepository adminRepository) {
        this.notificationRepository = notificationRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    public List<Notification> findForUser(Long userId) { return notificationRepository.findByUserId(userId); }
    public long unreadCount(Long userId) { return notificationRepository.countUnread(userId); }

    public Optional<Notification> markRead(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId)
                .filter(notification -> notification.getUserId().equals(userId))
                .map(notification -> { notification.setRead(true); return notificationRepository.save(notification); });
    }

    @Transactional
    public int markAllRead(Long userId) { return notificationRepository.markAllRead(userId); }

    public void notifyClient(Content content, NotificationType type, String title, String message) {
        clientRepository.findById(content.getClientId())
                .ifPresent(client -> create(client.getUser_id(), type, title, message,
                        content.getContent_id(), content.getContent_id()));
    }

    public void notifyAdmin(Content content, NotificationType type, String title, String message) {
        findAdminForContent(content).ifPresent(admin ->
                create(admin.getUser_id(), type, title, message,
                        content.getContent_id(), content.getContent_id()));
    }

    public void notifyOppositeParty(Content content, User actor, Long commentId, String message) {
        if ("CLIENT".equalsIgnoreCase(actor.getRole())) {
            findAdminForContent(content).filter(user -> !user.getUser_id().equals(actor.getUser_id()))
                    .ifPresent(user -> create(user.getUser_id(), NotificationType.COMMENT_ADDED,
                            "תגובה חדשה", message, content.getContent_id(), commentId));
        } else {
            clientRepository.findById(content.getClientId())
                    .filter(client -> !client.getUser_id().equals(actor.getUser_id()))
                    .ifPresent(client -> create(client.getUser_id(), NotificationType.COMMENT_ADDED,
                            "תגובה חדשה", message, content.getContent_id(), commentId));
        }
    }

    private Optional<User> findAdminForContent(Content content) {
        Optional<Client> client = clientRepository.findById(content.getClientId());
        if (client.isPresent() && client.get().getAdmin_id() != null) {
            Optional<User> assigned = adminRepository.findById(client.get().getAdmin_id())
                    .flatMap(admin -> userRepository.findById(admin.getUserId()))
                    .filter(user -> "ADMIN".equalsIgnoreCase(user.getRole()));
            if (assigned.isPresent()) return assigned;
        }
        return userRepository.findFirstAdmin();
    }

    private Notification create(Long userId, NotificationType type, String title, String message,
                                Long contentId, Long entityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedContentId(contentId);
        notification.setEntityId(entityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }
}
