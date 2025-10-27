package com.stockportfolio.utils;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class SearchableTable extends JPanel {
    private JTable table;
    private JTextField searchField;
    private TableRowSorter<TableModel> sorter;
    private JButton clearButton;

    public SearchableTable(JTable table) {
        this.table = table;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create search panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField(20);
        searchField.setToolTipText("Search across all columns...");
        clearButton = new JButton("Clear");

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(clearButton, BorderLayout.EAST);

        // Set up table sorter
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        // Add search functionality
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                sorter.setRowFilter(null);
            }
        });

        // Add components
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void performSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Create filter that searches across all columns
            RowFilter<TableModel, Object> filter = new RowFilter<TableModel, Object>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        Object value = entry.getValue(i);
                        if (value != null && value.toString().toLowerCase().contains(text.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            sorter.setRowFilter(filter);
        }
    }

    public JTable getTable() {
        return table;
    }

    public void setData(List<Object[]> data, String[] columnNames) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(columnNames);

        for (Object[] row : data) {
            model.addRow(row);
        }
    }

    public List<Object[]> getFilteredData() {
        List<Object[]> filteredData = new ArrayList<>();
        TableModel model = table.getModel();

        for (int i = 0; i < table.getRowCount(); i++) {
            int modelRow = table.convertRowIndexToModel(i);
            Object[] row = new Object[model.getColumnCount()];
            for (int j = 0; j < model.getColumnCount(); j++) {
                row[j] = model.getValueAt(modelRow, j);
            }
            filteredData.add(row);
        }

        return filteredData;
    }
}
