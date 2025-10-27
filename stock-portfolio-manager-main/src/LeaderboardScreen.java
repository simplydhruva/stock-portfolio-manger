import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.stockportfolio.model.LeaderboardEntry;
import com.stockportfolio.utils.DatabaseManager;

public class LeaderboardScreen extends JPanel {
    private App app;
    private JTable leaderboardTable;
    private JButton refreshButton;
    private JButton backButton;
    private JComboBox<String> periodComboBox;

    public LeaderboardScreen(App app) {
        this.app = app;
        initializeComponents();
        layoutComponents();
        addEventListeners();
        loadLeaderboard();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        String[] columnNames = {"Rank", "Username", "Total Return", "Total Value"};
        Object[][] data = {}; // Will be populated
        leaderboardTable = new JTable(data, columnNames);
        leaderboardTable.setEnabled(false); // Read-only

        refreshButton = new JButton("Refresh");
        backButton = new JButton("Back to Dashboard");

        periodComboBox = new JComboBox<>(new String[]{"DAILY", "WEEKLY", "MONTHLY", "ALL_TIME"});
        periodComboBox.setSelectedItem("ALL_TIME");
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Leaderboard"));
        topPanel.add(new JLabel("Period:"));
        topPanel.add(periodComboBox);
        topPanel.add(refreshButton);
        topPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addEventListeners() {
        refreshButton.addActionListener(e -> loadLeaderboard());
        backButton.addActionListener(e -> app.showDashboard());
        periodComboBox.addActionListener(e -> loadLeaderboard());
    }

    private void loadLeaderboard() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            String selectedPeriod = (String) periodComboBox.getSelectedItem();
            List<LeaderboardEntry> entries = db.getLeaderboardEntries(selectedPeriod); // Assuming we add this method

            Object[][] data = new Object[entries.size()][4];
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                data[i][0] = entry.getRank();
                data[i][1] = entry.getUsername();
                data[i][2] = String.format("%.2f%%", entry.getTotalReturn());
                data[i][3] = String.format("$%.2f", entry.getTotalValue());
            }

            leaderboardTable.setModel(new javax.swing.table.DefaultTableModel(data, new String[]{"Rank", "Username", "Total Return", "Total Value"}));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading leaderboard: " + ex.getMessage());
        }
    }
}
