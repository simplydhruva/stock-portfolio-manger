package com.stockportfolio.utils;

import java.util.regex.Pattern;

public class InputValidator {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private static final Pattern IMAGE_FILE_PATTERN =
        Pattern.compile(".*\\.(jpg|jpeg|png|gif|bmp)$", Pattern.CASE_INSENSITIVE);

    /**
     * Validates if the given email address is in a valid format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates if the given username meets the requirements
     * Username must be 3-20 characters, alphanumeric and underscores only
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validates if the given password meets security requirements
     * Password must be at least 8 characters with uppercase, lowercase, digit, and special character
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Sanitizes input by removing potentially dangerous HTML/script content
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Basic HTML tag removal - in production, use a proper HTML sanitizer
        return input.replaceAll("<[^>]*>", "")
                   .replaceAll("javascript:", "")
                   .replaceAll("on\\w+=", "")
                   .trim();
    }

    /**
     * Validates if the file is an allowed image type
     */
    public static boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        return IMAGE_FILE_PATTERN.matcher(filename.trim()).matches();
    }

    /**
     * Validates file size (in bytes) - max 5MB for avatars
     */
    public static boolean isValidFileSize(long sizeInBytes) {
        final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        return sizeInBytes > 0 && sizeInBytes <= MAX_FILE_SIZE;
    }

    /**
     * Validates bio length (max 500 characters)
     */
    public static boolean isValidBio(String bio) {
        return bio == null || bio.length() <= 500;
    }

    /**
     * Validates website URL format
     */
    public static boolean isValidWebsite(String website) {
        if (website == null || website.trim().isEmpty()) {
            return true; // Optional field
        }
        String trimmed = website.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}
