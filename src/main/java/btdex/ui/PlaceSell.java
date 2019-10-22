package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.Globals;
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

	private JToggleButton buyToken;
	private JToggleButton sellToken;

	public PlaceSell(JFrame owner, Market market) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		isToken = market.getTokenID()!=null;

		if(isToken)
			setTitle(String.format("Exchange %s for BURST", market.toString()));
		else
			setTitle(String.format("Sell BURST for %s", market.toString()));

		this.market = market;

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
		
		if(isToken) {
			buyToken = new JRadioButton(String.format("Buy %s with BURST", market), true);
	        sellToken = new JRadioButton(String.format("Sell %s for BURST", market));
	        
	        fieldPanel.add(buyToken);
	        fieldPanel.add(sellToken);

	        ButtonGroup bgroup = new ButtonGroup();
	        bgroup.add(buyToken);
	        bgroup.add(sellToken);
		}
		
		fieldPanel.add(new Desc("Price (" + (isToken ? "BURST" : market) + ")", price));
		fieldPanel.add(new Desc("Size (" + (isToken ? market : "BURST") + ")", amount));
		fieldPanel.add(new Desc("Total (" + (isToken ? "BURST" : market) + ")", total));

		if(!isToken) {
			Properties conf = Globals.getConf();
			
			for (int i = 0; i < 100; i++) {
				String acNameKey = market.toString() + i + ".name";
				
				String acName = conf.getProperty(acNameKey, null);
				if(acName == null || acName.length()==0)
					break;
				account.addItem(acName);
			}
			
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
	public void setVisible(boolean b) {
		if(account.getItemCount()==0 && !isToken) {
			JOptionPane.showMessageDialog(this, "You need to register a " + market + " account first.",
					"Error", JOptionPane.ERROR_MESSAGE);
			dispose();
			
			// TODO open the settings dialog here
			return;
		}

		super.setVisible(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}
		
		if(e.getSource() == okButton) {
			String error = null;
			
			if(account.getSelectedIndex() < 0 && !isToken) {
				error = "You need to register an account first";
			}
			
			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error).display(okButton);
				return;
			}

		}

	}

}
