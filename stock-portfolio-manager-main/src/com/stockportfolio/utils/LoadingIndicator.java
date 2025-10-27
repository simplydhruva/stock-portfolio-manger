package com.stockportfolio.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class LoadingIndicator extends JPanel {
    private JProgressBar progressBar;
    private JLabel messageLabel;
    private boolean indeterminate;

    public LoadingIndicator(String message) {
        this(message, true);
    }

    public LoadingIndicator(String message, boolean indeterminate) {
        this.indeterminate = indeterminate;
        initializeUI(message);
    }

    private void initializeUI(String message) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setStringPainted(true);
        }
        progressBar.setPreferredSize(new Dimension(300, 20));

        add(messageLabel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setProgress(int progress) {
        if (!indeterminate) {
            progressBar.setValue(progress);
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setStringPainted(true);
        } else {
            progressBar.setStringPainted(false);
        }
    }

    public void start() {
        progressBar.setIndeterminate(true);
    }

    public void stop() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(100);
    }

    public static LoadingIndicator createOverlay(String message) {
        LoadingIndicator indicator = new LoadingIndicator(message);
        indicator.setOpaque(false);
        indicator.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        return indicator;
    }
}
