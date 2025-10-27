import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class PortfolioComparisonDialog extends JDialog {
    private App app;
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;

    private JTabbedPane tabbedPane;
    private JTable comparisonTable;
    private DefaultTableModel tableModel;
    private Map<String, JCheckBox> portfolioCheckboxes;
    private List<Portfolio> selectedPortfolios;

    private PerformanceComparisonPanel performancePanel;
    private RiskComparisonPanel riskPanel;
    private AllocationComparisonPanel allocationPanel;

    public PortfolioComparisonDialog(App app, DatabaseManager dbManager) {
        super(app, "Portfolio Comparison", true);
        this.app = app;
        this.dbManager = dbManager;
        this.aiAnalytics = new AIAnalytics();
        this.portfolioCheckboxes = new HashMap<>();
        this.selectedPortfolios = new ArrayList<>();

        initializeUI();
        loadPortfolios();
        setSize(1200, 700);
        setLocationRelativeTo(app);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        add(new JLabel("Compare Multiple Portfolios", SwingConstants.CENTER), BorderLayout.NORTH);

        // Tabbed pane for different comparison views
        tabbedPane = new JTabbedPane();

        // Overview Tab
        JPanel overviewPanel = new JPanel(new BorderLayout());
        initializeOverviewTab(overviewPanel);
        tabbedPane.addTab("Overview", overviewPanel);

        // Performance Comparison Tab
        performancePanel = new PerformanceComparisonPanel();
        tabbedPane.addTab("Performance", performancePanel);

        // Risk Analysis Tab
        riskPanel = new RiskComparisonPanel(dbManager);
        tabbedPane.addTab("Risk Analysis", riskPanel);

        // Allocation Comparison Tab
        allocationPanel = new AllocationComparisonPanel(dbManager);
        tabbedPane.addTab("Allocation", allocationPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel with action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton compareButton = new JButton("Compare Selected");
        compareButton.addActionListener(e -> updateComparison());
        bottomPanel.add(compareButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void initializeOverviewTab(JPanel panel) {
        panel.setLayout(new BorderLayout());

        // Portfolio selection panel
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new java.awt.GridLayout(0, 3, 5, 5));
        selectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Portfolios to Compare"));

        // This will be populated in loadPortfolios()
        JScrollPane selectionScroll = new JScrollPane(selectionPanel);
        selectionScroll.setPreferredSize(new java.awt.Dimension(300, 200));
        panel.add(selectionScroll, BorderLayout.WEST);

        // Summary table
        String[] columnNames = {"Portfolio", "Total Value", "Total Cost", "Total P&L", "Return %", "Risk Level"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        comparisonTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(comparisonTable);
        panel.add(tableScroll, BorderLayout.CENTER);
    }

    private void loadPortfolios() {
        try {
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "No user logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            // Clear existing checkboxes
            portfolioCheckboxes.clear();

            // Get the selection panel from the overview tab
            JPanel overviewPanel = (JPanel) tabbedPane.getComponentAt(0);
            JPanel selectionPanel = (JPanel) ((JScrollPane) overviewPanel.getComponent(0)).getViewport().getView();
            selectionPanel.removeAll();

            // Add checkboxes for each portfolio
            for (Portfolio portfolio : portfolios) {
                JCheckBox checkbox = new JCheckBox(portfolio.getName());
                checkbox.setActionCommand(String.valueOf(portfolio.getId()));
                portfolioCheckboxes.put(String.valueOf(portfolio.getId()), checkbox);
                selectionPanel.add(checkbox);
            }

            selectionPanel.revalidate();
            selectionPanel.repaint();

            // Load initial data
            updateComparison();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading portfolios: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateComparison() {
        selectedPortfolios.clear();

        try {
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "No user logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            List<Portfolio> allPortfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            // Get selected portfolios
            for (Portfolio portfolio : allPortfolios) {
                JCheckBox checkbox = portfolioCheckboxes.get(String.valueOf(portfolio.getId()));
                if (checkbox != null && checkbox.isSelected()) {
                    selectedPortfolios.add(portfolio);
                }
            }

            // Update overview table
            updateOverviewTable();

            // Update other tabs
            performancePanel.setPortfolios(selectedPortfolios);
            riskPanel.setPortfolios(selectedPortfolios);
            allocationPanel.setPortfolios(selectedPortfolios);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating comparison: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateOverviewTable() {
        tableModel.setRowCount(0);

        for (Portfolio portfolio : selectedPortfolios) {
            double returnPct = portfolio.getTotalCost() > 0 ?
                    (portfolio.getTotalPnL() / portfolio.getTotalCost()) * 100 : 0;

            // Get risk assessment
            String riskLevel = "Unknown";
            try {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());
                if (!positions.isEmpty()) {
                    Map<String, Double> weights = calculateWeights(positions);
                    AIAnalytics.RiskAssessment risk = aiAnalytics.assessRisk(weights);
                    riskLevel = risk.getRiskLevel();
                }
            } catch (Exception e) {
                riskLevel = "Error";
            }

            Object[] rowData = {
                    portfolio.getName(),
                    String.format("$%.2f", portfolio.getTotalValue()),
                    String.format("$%.2f", portfolio.getTotalCost()),
                    String.format("$%.2f", portfolio.getTotalPnL()),
                    String.format("%.2f%%", returnPct),
                    riskLevel
            };
            tableModel.addRow(rowData);
        }
    }

    private Map<String, Double> calculateWeights(List<Position> positions) {
        Map<String, Double> weights = new HashMap<>();
        double totalValue = positions.stream()
            .mapToDouble(p -> p.getQuantity() * p.getCurrentPrice())
            .sum();

        for (Position position : positions) {
            double positionValue = position.getQuantity() * position.getCurrentPrice();
            double weight = totalValue > 0 ? positionValue / totalValue : 0.0;
            weights.put(position.getStockSymbol(), weight);
        }

        return weights;
    }
}
