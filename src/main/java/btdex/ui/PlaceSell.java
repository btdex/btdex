package btdex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
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
	JTextField amount, rate, timeout;
	JSlider security;

	private JButton okButton;
	private JButton calcelButton;

	private boolean isToken;

	public PlaceSell(JFrame owner, Market market) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle("Sell BURST for " + market.toString());

		this.market = market;

		isToken = market.getBurstTokenID()!=null;

		account = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		topPanel.setBorder(BorderFactory.createTitledBorder("Account to receive " + market.toString()));
		topPanel.add(account);
		topPanel.add(accountDetails);

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		amount = new JTextField(16);
		rate = new JTextField(16);
		timeout = new JTextField(16);


		panel.setBorder(BorderFactory.createTitledBorder("Offer configuration"));
		panel.add(new Desc("Rate (" + market.toString() + ")", rate));
		panel.add(new Desc("Amount (BURST)", amount));
		panel.add(new Desc("Timeout (mins.)", timeout));

		if(!isToken) {
			security = new JSlider(1, 20);

			Desc securityDesc = new Desc("", security);
			panel.add(securityDesc);

			security.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					int value = security.getValue();
					securityDesc.setDesc(String.format("Security deposit %d %%", value));
				}
			});
			security.getModel().setValue(5);
		}

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(calcelButton);
		buttonPane.add(okButton);

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		// If not a token, we need the top panel
		if(!isToken)
			content.add(topPanel, BorderLayout.PAGE_START);

		content.add(panel, BorderLayout.CENTER);
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
