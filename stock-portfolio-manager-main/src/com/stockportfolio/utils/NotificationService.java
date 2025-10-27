package com.stockportfolio.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class NotificationService {
    private static NotificationService instance;
    private List<Notification> notifications;
    private List<NotificationListener> listeners;

    private NotificationService() {
        this.notifications = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void addNotification(String title, String message, NotificationType type) {
        Notification notification = new Notification(title, message, type, LocalDateTime.now());
        notifications.add(notification);

        // Notify all listeners
        for (NotificationListener listener : listeners) {
            listener.onNotification(notification);
        }

        // Show system notification
        showSystemNotification(notification);
    }

    public void sendNotification(String userId, NotificationType type, String title, String message) {
        addNotification(title, message, type);
    }

    public void checkAndAwardAchievements(com.stockportfolio.model.User user) {
        // Placeholder for achievement logic
        // This can be expanded later
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    public List<Notification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public List<Notification> getUnreadNotifications() {
        return notifications.stream()
                .filter(n -> !n.isRead())
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    public void markAsRead(Notification notification) {
        notification.setRead(true);
    }

    public void markAllAsRead() {
        notifications.forEach(n -> n.setRead(true));
    }

    private void showSystemNotification(Notification notification) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    notification.getMessage(),
                    notification.getTitle(),
                    getMessageType(notification.getType()));
        });
    }

    private int getMessageType(NotificationType type) {
        switch (type) {
            case SUCCESS: return JOptionPane.INFORMATION_MESSAGE;
            case WARNING: return JOptionPane.WARNING_MESSAGE;
            case ERROR: return JOptionPane.ERROR_MESSAGE;
            case INFO: default: return JOptionPane.INFORMATION_MESSAGE;
        }
    }

    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }

    public interface NotificationListener {
        void onNotification(Notification notification);
    }

    public static class Notification {
        private String title;
        private String message;
        private NotificationType type;
        private LocalDateTime timestamp;
        private boolean read;

        public Notification(String title, String message, NotificationType type, LocalDateTime timestamp) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.read = false;
        }

        // Getters and setters
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }
}
