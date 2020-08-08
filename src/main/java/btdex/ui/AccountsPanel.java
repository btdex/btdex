package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.MarketAccount;
import btdex.core.Markets;
import layout.SpringUtilities;

public class AccountsPanel extends JPanel implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;
	
	private Market market;

	private JButton addButton;

	private JButton removeButton;

	private JComboBox<Market> marketComboBox;

	private JTextField nameField;
	private JPanel formPanel;
	private HashMap<String, JComponent> formFields = new HashMap<>();

	private JButton cancelButton;

	private JButton okButton;

	private JPanel right;

	private JPanel left;

	private Main main;

	private JPanel rightButtonPane;

	public static final int COL_MARKET = 0;
	public static final int COL_NAME = 1;

	static final int PAD = 6;

	static final String[] COLUMN_NAMES = {
			"acc_market_col",
			"acc_account_col",
	};

	public AccountsPanel(Main main) {
		super(new BorderLayout());

		this.main = main;

		table = new JTable(model = new DefaultTableModel(COLUMN_NAMES, 0));
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			table.getColumnModel().getColumn(i).setHeaderValue(tr(COLUMN_NAMES[i]));
		}
		table.setRowHeight(table.getRowHeight()+7);
		table.setPreferredScrollableViewportSize(new Dimension(600, 200));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(this);

		left = new JPanel(new BorderLayout());
		right = new JPanel();
		right.setVisible(false);
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

		left.setBorder(BorderFactory.createTitledBorder(tr("acc_your_accounts")));
		right.setBorder(BorderFactory.createTitledBorder(tr("acc_account_details")));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );			
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.setAutoCreateColumnsFromModel(false);
		table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_MARKET).setPreferredWidth(20);

		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		left.add(buttonPane, BorderLayout.PAGE_END);

		addButton = new JButton(tr("acc_add_button"));
		removeButton = new JButton(tr("acc_remove_button"));
		removeButton.setEnabled(false);

		addButton.addActionListener(this);
		removeButton.addActionListener(this);

		buttonPane.add(addButton);
		buttonPane.add(removeButton);

		marketComboBox = new JComboBox<Market>();
		for(Market m : Markets.getMarkets()) {
			if(m.getTokenID()!=null)
				continue;
			marketComboBox.addItem(m);
		}
		marketComboBox.addActionListener(this);

		JPanel topPanel = new JPanel(new SpringLayout());
		topPanel.add(new Desc(tr("main_market"), marketComboBox), BorderLayout.LINE_START);
		topPanel.add(new Desc(tr("acc_alias"), nameField = new JTextField()), BorderLayout.CENTER);
		SpringUtilities.makeCompactGrid(topPanel, 1, 2, 0, 0, PAD, PAD);
		right.add(topPanel);

		formPanel = new JPanel(new SpringLayout());
		//		JScrollPane formScroll = new JScrollPane(formPanel);
		//		right.add(formScroll);
		right.add(formPanel);

		cancelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		rightButtonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rightButtonPane.add(cancelButton);
		rightButtonPane.add(okButton);
		right.add(rightButtonPane);

		add(left, BorderLayout.LINE_START);
		JPanel rightContainer = new JPanel(new BorderLayout());
		rightContainer.add(right, BorderLayout.PAGE_START);
		add(rightContainer, BorderLayout.CENTER);

		left.add(scrollPane, BorderLayout.CENTER);	

		loadAccounts();
	}

	private void loadAccounts() {
		model.setNumRows(0);
		ArrayList<MarketAccount> accs = Globals.getInstance().getMarketAccounts();
		for (int i = 0; i < accs.size(); i++) {
			Object []row = new Object[2];
			row[0] = accs.get(i).getMarket();
			row[1] = accs.get(i).getName();
			model.addRow(row);
		}
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		market = (Market) marketComboBox.getSelectedItem();
		ArrayList<String> fieldKeys = market.getFieldKeys();

		if(e.getSource() == addButton) {
			right.setVisible(true);
			marketComboBox.setEnabled(true);
			nameField.setEditable(true);
			rightButtonPane.setVisible(true);
			marketComboBox.setSelectedIndex(0);
			addButton.setEnabled(false);
			table.setEnabled(false);
			table.clearSelection();
		}
		if(e.getSource() == cancelButton) {
			right.setVisible(false);
			addButton.setEnabled(true);
			table.setEnabled(true);
		}
		if(e.getSource() == okButton) {
			HashMap<String, String> fields = new HashMap<>();

			for (int i = 0; i < fieldKeys.size(); i++) {
				String key = fieldKeys.get(i);
				market.setFieldValue(key, formFields.get(key), fields);
			}
			try {
				market.validate(fields);
			}
			catch (Exception ex) {
				Toast.makeText(main, ex.getMessage(), Toast.Style.ERROR).display();
				return;
			}

			String name = nameField.getText();
			if(name.trim().length()==0)
				name = market.simpleFormat(fields);

			MarketAccount ac = new MarketAccount(market.toString(), name, fields);
			Globals.getInstance().addAccount(ac);

			loadAccounts();
			right.setVisible(false);
			addButton.setEnabled(true);
			table.setEnabled(true);
		}
		if(e.getSource() == marketComboBox) {
			HashMap<String, String> fields = new HashMap<>();
			for(String key : fieldKeys)
				fields.put(key, "");
			createFields(fields, true);
			nameField.setText("");
			formFields.get(fieldKeys.get(0)).requestFocusInWindow();
		}

		if(e.getSource() == removeButton) {
			int row = table.getSelectedRow();
			if(row >= 0) {
				int ret = JOptionPane.showConfirmDialog(main,
						tr("acc_remove_selected"), tr("acc_remove"),
								JOptionPane.YES_NO_OPTION);
				if(ret == JOptionPane.YES_OPTION) {
					Globals.getInstance().removeAccount(row);
					loadAccounts();
					right.setVisible(false);
				}				
			}
		}
	}
	
	private void createFields(HashMap<String,String> fields, boolean editable) {
		formPanel.removeAll();
		formFields.clear();

		for (String key : market.getFieldKeys()) {
			JLabel l = new JLabel(market.getFieldDescription(key), JLabel.TRAILING);
			formPanel.add(l);
			JComponent editor = market.getFieldEditor(key, editable, fields);
			formFields.put(key, editor);
			l.setLabelFor(editor);
			formPanel.add(editor);
		}			
		SpringUtilities.makeCompactGrid(formPanel, fields.size(), 2, 0, 0, PAD, PAD);
		validate();
		formPanel.repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;
		int row = table.getSelectedRow();
		removeButton.setEnabled(row >= 0);
		
		if(row >= 0) {
			rightButtonPane.setVisible(false);
			right.setVisible(true);
			marketComboBox.setEnabled(false);
			nameField.setEditable(false);
			
			// show this account properties
			MarketAccount ac = Globals.getInstance().getMarketAccounts().get(row);
			
			for (int i = 0; i < marketComboBox.getItemCount(); i++) {
				if(ac.getMarket().equals(marketComboBox.getItemAt(i).toString())) {
					marketComboBox.setSelectedIndex(i);
					break;
				}
			}
			nameField.setText(ac.getName());
			
			createFields(ac.getFields(), false);
		}
	}
}
