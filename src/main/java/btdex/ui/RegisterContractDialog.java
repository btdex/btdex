package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import bt.BT;
import bt.compiler.Compiler;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.NumberFormatting;
import btdex.sc.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class RegisterContractDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pin;

	private JButton okButton;
	private JButton calcelButton;

	private Compiler contract;

	public RegisterContractDialog(Window owner) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setTitle("Register Smart Contract");

		conditions = new JTextPane();
		conditions.setPreferredSize(new Dimension(80, 120));
		acceptBox = new JCheckBox("I accept the terms and conditions");

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pin = new JPasswordField(12);
		pin.addActionListener(this);

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc("PIN", pin));
		buttonPane.add(new Desc(" ", calcelButton));
		buttonPane.add(new Desc(" ", okButton));

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder("Terms and conditions"));
		JScrollPane scroll = new JScrollPane(conditions);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(conditions.getPreferredSize());
		conditionsPanel.add(scroll, BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(conditionsPanel, BorderLayout.PAGE_END);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		contract = Contracts.getContract();

		String terms = null;
		terms = "You are registering a new smart contract for selling BURST."
				+ "\n\nThis contract can later be configured to sell BURST at any market."
				+ "\n\nRegistering a new contract will cost you %s BURST."
				+ " Your new contract will be available in a few minutes, as soon"
				+ " as this transaction confirms.";
		terms = String.format(terms,
				NumberFormatting.BURST.format(BT.getMinRegisteringFee(contract).longValue()));
		conditions.setText(terms);
		conditions.setCaretPosition(0);
		
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && !acceptBox.isSelected()) {
				error = "You must accept the terms first";
				acceptBox.requestFocus();
			}

			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = "Invalid PIN";
				pin.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

			// all set, lets register the contract
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				Compiler contract = Contracts.getContract();
				long data[] = g.getNewContractData();

				ByteBuffer dataBuffer = ByteBuffer.allocate(data==null ? 0 : data.length*8);
				dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; data!=null && i < data.length; i++) {
					dataBuffer.putLong(data[i]);
				}

				byte[] creationBytes = BurstCrypto.getInstance().getATCreationBytes((short) 1,
						contract.getCode(), dataBuffer.array(), (short)contract.getDataPages(), (short)1, (short)1,
						BurstValue.fromPlanck(SellContract.ACTIVATION_FEE));

				Single<TransactionBroadcast> tx = g.getNS().generateCreateATTransaction(g.getPubKey(),
						BT.getMinRegisteringFee(contract),
						1000, "BTDEX", "BTDEX sell contract", creationBytes)
						.flatMap(unsignedTransactionBytes -> {
							byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
							return g.getNS().broadcastTransaction(signedTransactionBytes);
						});

				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();
				setVisible(false);

				Toast.makeText((JFrame) this.getOwner(),
						String.format("Transaction %s has been broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
			}
			catch (Exception ex) {
				Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}
}
