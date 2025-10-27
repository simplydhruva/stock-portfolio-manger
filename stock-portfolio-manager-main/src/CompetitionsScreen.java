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

import com.stockportfolio.model.Competition;
import com.stockportfolio.model.LeaderboardEntry;
import com.stockportfolio.utils.DatabaseManager;

public class CompetitionsScreen extends JPanel {
    private App app;
    private JList<String> competitionsList;
    private DefaultListModel<String> competitionsListModel;
    private JButton joinButton;
    private JButton viewLeaderboardButton;
    private JButton backButton;

    public CompetitionsScreen(App app) {
        this.app = app;
        initializeComponents();
        layoutComponents();
        addEventListeners();
        loadCompetitions();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        competitionsListModel = new DefaultListModel<>();
        competitionsList = new JList<>(competitionsListModel);
        competitionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        joinButton = new JButton("Join Competition");
        viewLeaderboardButton = new JButton("View Leaderboard");
        backButton = new JButton("Back to Dashboard");
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Trading Competitions"));
        topPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(competitionsList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(joinButton);
        buttonPanel.add(viewLeaderboardButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addEventListeners() {
        joinButton.addActionListener(e -> joinSelectedCompetition());
        viewLeaderboardButton.addActionListener(e -> viewCompetitionLeaderboard());
        backButton.addActionListener(e -> app.showDashboard());
    }

    private void loadCompetitions() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Competition> competitions = db.getActiveCompetitions(); // Assuming we add this method
            competitionsListModel.clear();
            for (Competition comp : competitions) {
                competitionsListModel.addElement(comp.getName() + " - " + comp.getStatus());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading competitions: " + ex.getMessage());
        }
    }

    private void joinSelectedCompetition() {
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selectedCompetition = competitionsList.getSelectedValue();
        if (selectedCompetition == null) {
            JOptionPane.showMessageDialog(this, "Please select a competition to join.");
            return;
        }

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            String compName = selectedCompetition.split(" - ")[0];
            Competition competition = db.getCompetitionByName(compName); // Assuming we add this method
            if (competition != null) {
                db.joinCompetition(app.getCurrentUser().getId(), competition.getId()); // Assuming we add this method
                JOptionPane.showMessageDialog(this, "You have joined the competition: " + compName);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error joining competition: " + ex.getMessage());
        }
    }

    private void viewCompetitionLeaderboard() {
        String selectedCompetition = competitionsList.getSelectedValue();
        if (selectedCompetition == null) {
            JOptionPane.showMessageDialog(this, "Please select a competition to view leaderboard.");
            return;
        }

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            String compName = selectedCompetition.split(" - ")[0];
            Competition competition = db.getCompetitionByName(compName);
            if (competition != null) {
                List<LeaderboardEntry> entries = db.getCompetitionLeaderboard(competition.getId()); // Assuming we add this method
                // Display leaderboard - for now, show a simple dialog
                StringBuilder sb = new StringBuilder("Competition Leaderboard:\n");
                for (LeaderboardEntry entry : entries) {
                    sb.append(entry.getRank()).append(". ").append(entry.getUsername())
                      .append(" - ").append(String.format("%.2f%%", entry.getTotalReturn())).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error viewing leaderboard: " + ex.getMessage());
        }
    }
}
