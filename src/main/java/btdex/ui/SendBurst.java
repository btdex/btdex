package btdex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.ContractState;
import btdex.core.Globals;
import burst.kit.entity.response.FeeSuggestion;

public class SendBurst extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextField recipient;
	JTextField message;
	JTextField amount;
	JPasswordField pin;
	JSlider fee;

	private final int N_SLOTS = 1020;
	private final int FEE_QUANT = 735000;

	private JButton okButton;

	private JButton calcelButton;

	public SendBurst(JFrame owner) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle("Send BURST");

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		recipient = new JTextField(26);
		message = new JTextField(26);
		
		pin = new JPasswordField(12);
		
		amount = new JTextField();
		fee = new JSlider(1, N_SLOTS);

		topPanel.add(new Desc("Recipient", recipient));
		topPanel.add(new Desc("Message", message));
		message.setToolTipText("Leave empty for no message");

		panel.add(new Desc("Amount (BURST)", amount));
		Desc feeDesc = new Desc("", fee);
		panel.add(feeDesc);
		fee.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				int value = fee.getValue() * FEE_QUANT;
				feeDesc.setDesc(String.format("Fee (%s) BURST", ContractState.format(value)));
			}
		});
		fee.getModel().setValue(10);
		
		panel.add(new Desc("PIN", pin));

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

		content.add(topPanel, BorderLayout.PAGE_START);
		content.add(panel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		pack();

		try {
			FeeSuggestion suggested = Globals.NS.suggestFee().blockingGet();
			int feeInt = (int)suggested.getPriorityFee().longValue()/FEE_QUANT;
			fee.getModel().setValue(feeInt);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}

	}

}
