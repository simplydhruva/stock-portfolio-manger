import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.SharedPortfolio;
import com.stockportfolio.utils.DatabaseManager;

public class CommunityScreen extends JPanel {
    private App app;
    private DefaultListModel<SharedPortfolio> sharedPortfolioListModel;
    private JList<SharedPortfolio> sharedPortfolioList;
    private JButton likeButton;
    private JButton unlikeButton;
    private JButton viewPortfolioButton;
    private JButton backButton;

    public CommunityScreen(App app) {
        this.app = app;
        initializeComponents();
        layoutComponents();
        addEventListeners();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        sharedPortfolioListModel = new DefaultListModel<>();
        sharedPortfolioList = new JList<>(sharedPortfolioListModel);
        sharedPortfolioList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sharedPortfolioList.setCellRenderer(new SharedPortfolioCellRenderer());

        likeButton = new JButton("Like");
        unlikeButton = new JButton("Unlike");
        viewPortfolioButton = new JButton("View Portfolio");
        backButton = new JButton("Back to Dashboard");

        loadSharedPortfolios();
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Community - Shared Portfolios"));
        topPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(sharedPortfolioList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(likeButton);
        buttonPanel.add(unlikeButton);
        buttonPanel.add(viewPortfolioButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addEventListeners() {
        likeButton.addActionListener(e -> likeSelectedPortfolio());
        unlikeButton.addActionListener(e -> unlikeSelectedPortfolio());
        viewPortfolioButton.addActionListener(e -> viewSelectedPortfolio());
        backButton.addActionListener(e -> app.showDashboard());
    }

    private void loadSharedPortfolios() {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager db = DatabaseManager.getInstance();
                List<SharedPortfolio> sharedPortfolios = db.getPublicSharedPortfolios();
                sharedPortfolioListModel.clear();
                for (SharedPortfolio sp : sharedPortfolios) {
                    sharedPortfolioListModel.addElement(sp);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error loading shared portfolios: " + ex.getMessage());
            }
        });
    }

    private void likeSelectedPortfolio() {
        SharedPortfolio selected = sharedPortfolioList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a portfolio to like.");
            return;
        }
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (db.hasUserLikedPortfolio(selected.getId(), app.getCurrentUser().getId())) {
                JOptionPane.showMessageDialog(this, "You have already liked this portfolio.");
                return;
            }
            db.likePortfolio(new com.stockportfolio.model.PortfolioLike(selected.getId(), app.getCurrentUser().getId()));
            db.incrementPortfolioLikes(selected.getId());
            loadSharedPortfolios();
            JOptionPane.showMessageDialog(this, "You liked the portfolio.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error liking portfolio: " + ex.getMessage());
        }
    }

    private void unlikeSelectedPortfolio() {
        SharedPortfolio selected = sharedPortfolioList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a portfolio to unlike.");
            return;
        }
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.hasUserLikedPortfolio(selected.getId(), app.getCurrentUser().getId())) {
                JOptionPane.showMessageDialog(this, "You have not liked this portfolio yet.");
                return;
            }
            db.unlikePortfolio(selected.getId(), app.getCurrentUser().getId());
            db.decrementPortfolioLikes(selected.getId());
            loadSharedPortfolios();
            JOptionPane.showMessageDialog(this, "You unliked the portfolio.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error unliking portfolio: " + ex.getMessage());
        }
    }

    private void viewSelectedPortfolio() {
        SharedPortfolio selected = sharedPortfolioList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a portfolio to view.");
            return;
        }
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            Portfolio portfolio = db.getPortfolioById(selected.getPortfolioId());
            if (portfolio != null) {
                db.incrementPortfolioViews(selected.getId());
                JOptionPane.showMessageDialog(this, "Viewing portfolio: " + portfolio.getName());
                // TODO: Implement detailed portfolio view dialog or screen
            } else {
                JOptionPane.showMessageDialog(this, "Portfolio not found.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error viewing portfolio: " + ex.getMessage());
        }
    }

    // Custom cell renderer to display portfolio info in the list
    private static class SharedPortfolioCellRenderer extends JLabel implements javax.swing.ListCellRenderer<SharedPortfolio> {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends SharedPortfolio> list, SharedPortfolio value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = String.format("%s (Likes: %d, Views: %d)", value.getPortfolioId(), value.getLikes(), value.getViews());
            setText(text);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setOpaque(true);
            return this;
        }
    }
}
