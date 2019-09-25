package btdex.ui;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import btdex.core.ContractState;
import btdex.core.Market;
import btdex.markets.MarketBTC;
import burst.kit.entity.BurstAddress;

public class Main extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	JTable table;
	HashMap<BurstAddress, ContractState> map = new HashMap<>();
	ArrayList<ContractState> marketContracts = new ArrayList<>();
	
	Market selectedMarket = new MarketBTC();
	
	class MyTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		
		String[] columnNames = {"ASK",
                "SIZE (BURST)",
                "TOTAL",
                "CONTRACT",
                // "TIMEOUT (MINS)",
                "ACTION"};

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return marketContracts.size();
        }

        public String getColumnName(int col) {
        	String colName = columnNames[col];
        	if(col == 0 || col == 2)
        		colName += " (" + selectedMarket.toString() + ")";
        	return colName;
        }

        public Object getValueAt(int row, int col) {
        	ContractState s = marketContracts.get(row);
        	switch (col) {
			case 0:
				return s.getRate();
			case 1:
				return s.getAmount();
			case 2:
				return s.getRate()*s.getAmount();
			case 3:
				return s.getAddress().toString();
			default:
				break;
			}
            return "";
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
	

	public Main() {
		super("BTDEX");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		table = new JTable(new MyTableModel());
		
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		
		add(scrollPane);
		
		ContractState.addContracts(map);
		marketContracts.clear();
		for(ContractState s : map.values()) {
			if(s.getMarket() == selectedMarket.getID())
				marketContracts.add(s);
		}
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new Main();
	}
}
