
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.stockportfolio.model.ForumCategory;
import com.stockportfolio.model.ForumComment;
import com.stockportfolio.model.ForumPost;
import com.stockportfolio.model.User;
import com.stockportfolio.utils.DatabaseManager;

public class ForumScreen extends JPanel {
    private User currentUser;
    private DatabaseManager dbManager;
    private JList<ForumCategory> categoryList;
    private JList<ForumPost> postList;
    private JTextArea postContentArea;
    private JTextArea commentContentArea;
    private JButton newPostButton;
    private JButton newCommentButton;
    private JButton refreshButton;

    public ForumScreen(User user) {
        this.currentUser = user;
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        initializeComponents();
        setupLayout();
        loadCategories();
        setupEventHandlers();
    }

    private void initializeComponents() {
        categoryList = new JList<>();
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setCellRenderer(new CategoryListCellRenderer());

        postList = new JList<>();
        postList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postList.setCellRenderer(new PostListCellRenderer());

        postContentArea = new JTextArea();
        postContentArea.setEditable(false);
        postContentArea.setWrapStyleWord(true);
        postContentArea.setLineWrap(true);
        postContentArea.setFont(new Font("Arial", Font.PLAIN, 12));

        commentContentArea = new JTextArea();
        commentContentArea.setEditable(false);
        commentContentArea.setWrapStyleWord(true);
        commentContentArea.setLineWrap(true);
        commentContentArea.setFont(new Font("Arial", Font.PLAIN, 12));

        newPostButton = new JButton("New Post");
        newCommentButton = new JButton("Add Comment");
        refreshButton = new JButton("Refresh");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(newPostButton);
        topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel leftPanel = new JPanel(new BorderLayout());

        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Categories"));
        categoryPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        leftPanel.add(categoryPanel, BorderLayout.NORTH);

        JPanel postsPanel = new JPanel(new BorderLayout());
        postsPanel.setBorder(BorderFactory.createTitledBorder("Posts"));
        postsPanel.add(new JScrollPane(postList), BorderLayout.CENTER);
        leftPanel.add(postsPanel, BorderLayout.CENTER);

        mainSplit.setLeftComponent(leftPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Content"));

        JPanel postContentPanel = new JPanel(new BorderLayout());
        postContentPanel.setBorder(BorderFactory.createTitledBorder("Post Content"));
        postContentPanel.add(new JScrollPane(postContentArea), BorderLayout.CENTER);

        JPanel commentsPanel = new JPanel(new BorderLayout());
        commentsPanel.setBorder(BorderFactory.createTitledBorder("Comments"));
        commentsPanel.add(new JScrollPane(commentContentArea), BorderLayout.CENTER);

        JPanel commentButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        commentButtonPanel.add(newCommentButton);
        commentsPanel.add(commentButtonPanel, BorderLayout.SOUTH);

        JSplitPane contentSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        contentSplit.setTopComponent(postContentPanel);
        contentSplit.setBottomComponent(commentsPanel);
        contentSplit.setDividerLocation(300);

        rightPanel.add(contentSplit, BorderLayout.CENTER);

        mainSplit.setRightComponent(rightPanel);
        mainSplit.setDividerLocation(300);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ForumCategory selectedCategory = categoryList.getSelectedValue();
                if (selectedCategory != null) {
                    loadPostsForCategory(selectedCategory.getId());
                }
            }
        });

        postList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ForumPost selectedPost = postList.getSelectedValue();
                if (selectedPost != null) {
                    loadPostContent(selectedPost);
                    loadCommentsForPost(selectedPost.getId());
                    try {
                        dbManager.incrementForumPostViewCount(selectedPost.getId());
                    } catch (SQLException ex) {
                        System.err.println("Error incrementing view count: " + ex.getMessage());
                    }
                }
            }
        });

        newPostButton.addActionListener(e -> showNewPostDialog());
        newCommentButton.addActionListener(e -> showNewCommentDialog());
        refreshButton.addActionListener(e -> refreshContent());
    }

    private void loadCategories() {
        try {
            List<ForumCategory> categories = dbManager.getAllForumCategories();
            DefaultListModel<ForumCategory> model = new DefaultListModel<>();
            for (ForumCategory category : categories) {
                model.addElement(category);
            }
            categoryList.setModel(model);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPostsForCategory(int categoryId) {
        try {
            List<ForumPost> posts = dbManager.getForumPostsByCategoryId(categoryId);
            DefaultListModel<ForumPost> model = new DefaultListModel<>();
            for (ForumPost post : posts) {
                model.addElement(post);
            }
            postList.setModel(model);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading posts: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPostContent(ForumPost post) {
        StringBuilder content = new StringBuilder();
        content.append("Title: ").append(post.getTitle()).append("\n\n");
        content.append("Author: ").append(post.getUsername()).append("\n");
        content.append("Posted: ").append(post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        content.append("Views: ").append(post.getViewCount()).append(" | Comments: ").append(post.getCommentCount()).append("\n");
        if (post.isPinned()) {
            content.append("[PINNED] ");
        }
        if (post.isLocked()) {
            content.append("[LOCKED]");
        }
        content.append("\n\n").append(post.getContent());

        postContentArea.setText(content.toString());
        postContentArea.setCaretPosition(0);
    }

    private void loadCommentsForPost(int postId) {
        try {
            List<ForumComment> comments = dbManager.getForumCommentsByPostId(postId);
            StringBuilder commentsText = new StringBuilder();

            for (ForumComment comment : comments) {
                commentsText.append("[").append(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("] ");
                commentsText.append(comment.getUsername()).append(":\n");
                commentsText.append(comment.getContent()).append("\n\n");
            }

            if (comments.isEmpty()) {
                commentsText.append("No comments yet.");
            }

            commentContentArea.setText(commentsText.toString());
            commentContentArea.setCaretPosition(0);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading comments: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNewPostDialog() {
        ForumCategory selectedCategory = categoryList.getSelectedValue();
        if (selectedCategory == null) {
            JOptionPane.showMessageDialog(this, "Please select a category first.",
                "No Category Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "New Post", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField titleField = new JTextField();
        JTextArea contentArea = new JTextArea();
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);

        panel.add(new JLabel("Title:"), BorderLayout.NORTH);
        panel.add(titleField, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JLabel("Content:"), BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton postButton = new JButton("Post");
        JButton cancelButton = new JButton("Cancel");

        postButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();

            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in both title and content.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ForumPost post = new ForumPost();
                post.setUserId(currentUser.getId());
                post.setUsername(currentUser.getUsername());
                post.setCategoryId(selectedCategory.getId());
                post.setTitle(title);
                post.setContent(content);
                post.setCreatedAt(java.time.LocalDateTime.now());
                post.setUpdatedAt(java.time.LocalDateTime.now());
                post.setViewCount(0);
                post.setCommentCount(0);
                post.setPinned(false);
                post.setLocked(false);

                dbManager.saveForumPost(post);

                selectedCategory.setPostCount(selectedCategory.getPostCount() + 1);
                dbManager.updateForumCategory(selectedCategory);

                dialog.dispose();
                refreshContent();

                JOptionPane.showMessageDialog(this, "Post created successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating post: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(postButton);
        buttonPanel.add(cancelButton);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showNewCommentDialog() {
        ForumPost selectedPost = postList.getSelectedValue();
        if (selectedPost == null) {
            JOptionPane.showMessageDialog(this, "Please select a post first.",
                "No Post Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedPost.isLocked()) {
            JOptionPane.showMessageDialog(this, "This post is locked and cannot accept new comments.",
                "Post Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "New Comment", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea contentArea = new JTextArea();
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);

        panel.add(new JLabel("Comment:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton commentButton = new JButton("Comment");
        JButton cancelButton = new JButton("Cancel");

        commentButton.addActionListener(e -> {
            String content = contentArea.getText().trim();

            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a comment.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ForumComment comment = new ForumComment();
                comment.setPostId(selectedPost.getId());
                comment.setUserId(currentUser.getId());
                comment.setUsername(currentUser.getUsername());
                comment.setContent(content);
                comment.setCreatedAt(java.time.LocalDateTime.now());
                comment.setUpdatedAt(java.time.LocalDateTime.now());
                comment.setParentCommentId(null);

                dbManager.saveForumComment(comment);

                selectedPost.setCommentCount(selectedPost.getCommentCount() + 1);
                dbManager.updateForumPost(selectedPost);

                dialog.dispose();
                loadCommentsForPost(selectedPost.getId());

                JOptionPane.showMessageDialog(this, "Comment added successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding comment: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(commentButton);
        buttonPanel.add(cancelButton);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void refreshContent() {
        loadCategories();
        categoryList.clearSelection();
        postList.setModel(new DefaultListModel<>());
        postContentArea.setText("");
        commentContentArea.setText("");
    }

    private class CategoryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ForumCategory) {
                ForumCategory category = (ForumCategory) value;
                setText(category.getName() + " (" + category.getPostCount() + " posts)");
                setToolTipText(category.getDescription());
            }

            return this;
        }
    }

    private class PostListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ForumPost) {
                ForumPost post = (ForumPost) value;
                StringBuilder text = new StringBuilder();
                if (post.isPinned()) {
                    text.append("📌 ");
                }
                text.append(post.getTitle());
                text.append(" - ").append(post.getUsername());
                text.append(" (").append(post.getCommentCount()).append(" comments)");
                setText(text.toString());
            }

            return this;
        }
    }
}
