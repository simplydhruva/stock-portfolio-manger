package com.stockportfolio.utils;

import java.awt.Component;

import javax.swing.JOptionPane;

public class ErrorHandler {
    public enum ErrorLevel {
        INFO, WARNING, ERROR
    }

    public static void showError(Component parent, String message, String title, ErrorLevel level) {
        int messageType;
        switch (level) {
            case INFO:
                messageType = JOptionPane.INFORMATION_MESSAGE;
                break;
            case WARNING:
                messageType = JOptionPane.WARNING_MESSAGE;
                break;
            case ERROR:
            default:
                messageType = JOptionPane.ERROR_MESSAGE;
                break;
        }

        JOptionPane.showMessageDialog(parent, formatMessage(message), title, messageType);
    }

    public static void showError(Component parent, Exception e, String context) {
        String message = context + "\n\nError: " + e.getMessage() +
                        "\n\nPlease try again or contact support if the problem persists.";
        showError(parent, message, "Error", ErrorLevel.ERROR);
    }

    public static void showDatabaseError(Component parent, Exception e) {
        showError(parent, e, "Database operation failed");
    }

    public static void showNetworkError(Component parent, Exception e) {
        showError(parent, e, "Network operation failed");
    }

    public static void showValidationError(Component parent, String field, String message) {
        String fullMessage = "Validation Error in " + field + ":\n" + message +
                           "\n\nPlease correct the input and try again.";
        showError(parent, fullMessage, "Validation Error", ErrorLevel.WARNING);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean showConfirmation(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(parent, message, title,
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    private static String formatMessage(String message) {
        // Add helpful suggestions based on common error patterns
        if (message.toLowerCase().contains("connection")) {
            message += "\n\nSuggestion: Check your internet connection and try again.";
        } else if (message.toLowerCase().contains("permission")) {
            message += "\n\nSuggestion: You may not have permission to perform this action.";
        } else if (message.toLowerCase().contains("not found")) {
            message += "\n\nSuggestion: The requested item may have been deleted or moved.";
        } else if (message.toLowerCase().contains("timeout")) {
            message += "\n\nSuggestion: The operation took too long. Please try again.";
        }

        return message;
    }

    public static void logError(String context, Exception e) {
        System.err.println("[" + context + "] Error: " + e.getMessage());
        e.printStackTrace();
    }
}
