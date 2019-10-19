package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.Market;

public class PlaceSell extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	Market market;

	JComboBox<String> account;
	JTextField accountDetails;
	JTextField amount, price, total;
	JSlider security;
	
	JTextArea conditions;
	JCheckBox acceptBox;
	
	JPasswordField pin;

	private JButton okButton;
	private JButton calcelButton;

	private boolean isToken;

	public PlaceSell(JFrame owner, Market market) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle("Sell BURST for " + market.toString());

		this.market = market;

		isToken = market.getTokenID()!=null;

		account = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		topPanel.setBorder(BorderFactory.createTitledBorder("Account to receive " + market.toString()));
		topPanel.add(account);
		topPanel.add(accountDetails);

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amount = new JTextField(16);
		price = new JTextField(16);
		total = new JTextField(16);
		total.setEditable(false);

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Offer configuration"));
		fieldPanel.add(new Desc("Price (" + market + ")", price));
		fieldPanel.add(new Desc("Size (BURST)", amount));
		fieldPanel.add(new Desc("Total (" + market + ")", total));

		if(!isToken) {
			security = new JSlider(0, 20);

			Desc securityDesc = new Desc("", security);
			fieldPanel.add(securityDesc);

			security.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					int value = security.getValue();
					String desc = String.format("Security deposit %d %%", value);
					if(value==0)
						desc = "Security deposit 0.1 %";
						
					securityDesc.setDesc(desc);
				}
			});
			security.getModel().setValue(5);
		}
		
		conditions = new JTextArea();		
		conditions.setEditable(false);
		conditions.setPreferredSize(new Dimension(80, 80));
		
		acceptBox = new JCheckBox("I accept the terms and conditions");

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		pin = new JPasswordField(12);

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc("PIN", pin));
		buttonPane.add(new Desc(" ", calcelButton));
		buttonPane.add(new Desc(" ", okButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		// If not a token, we need the top panel
		if(!isToken)
			content.add(topPanel, BorderLayout.PAGE_START);
		
		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder("Terms and conditions"));
		conditionsPanel.add(conditions, BorderLayout.CENTER);
		
		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(fieldPanel, BorderLayout.CENTER);
		centerPanel.add(conditionsPanel, BorderLayout.PAGE_END);
		
		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}

	}

}
