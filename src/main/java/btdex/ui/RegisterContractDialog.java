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
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bt.BT;
import bt.compiler.Compiler;
import btdex.core.*;

import static btdex.locale.Translation.tr;
import btdex.sc.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class RegisterContractDialog extends JDialog implements ActionListener, ChangeListener {
	private static final long serialVersionUID = 1L;

	JTextPane conditions;
	JCheckBox acceptBox;

	JSpinner numOfContractsSpinner;

	JPasswordField pin;

	private JButton okButton;
	private JButton cancelButton;

	private Compiler contract;

	private boolean isBuy;

	public RegisterContractDialog(Window owner, boolean isBuy) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.isBuy = isBuy;

		setTitle(tr("reg_register"));

		conditions = new JTextPane();
		conditions.setPreferredSize(new Dimension(240, 220));
		acceptBox = new JCheckBox(tr("dlg_accept_terms"));

		// The number of contracts to register
		SpinnerNumberModel numModel = new SpinnerNumberModel(1, 1, 10, 1);
		numOfContractsSpinner = new JSpinner(numModel);
		JPanel numOfContractsPanel = new Desc(tr("reg_num_contracts"), numOfContractsSpinner);
		numOfContractsSpinner.addChangeListener(this);

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pin = new JPasswordField(12);
		pin.addActionListener(this);

		cancelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc(tr("dlg_pin"), pin));
		buttonPane.add(new Desc(" ", okButton));
		buttonPane.add(new Desc(" ", cancelButton));

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder(tr("dlg_terms_and_conditions")));
		JScrollPane scroll = new JScrollPane(conditions);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(conditions.getPreferredSize());
		conditionsPanel.add(scroll, BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(conditionsPanel, BorderLayout.CENTER);

		content.add(numOfContractsPanel, BorderLayout.PAGE_START);
		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		contract = Contracts.getCompiler(isBuy ? ContractType.BUY : ContractType.SELL);
		stateChanged(null);
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
			setVisible(false);
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && !acceptBox.isSelected()) {
				error = tr("dlg_accept_first");
				acceptBox.requestFocus();
			}

			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = tr("dlg_invalid_pin");
				pin.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

			// all set, lets register the contract
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				int ncontracts = Integer.parseInt(numOfContractsSpinner.getValue().toString());

				for (int c = 0; c < ncontracts; c++) {
					long data[] = Contracts.getNewContractData();

					ByteBuffer dataBuffer = ByteBuffer.allocate(data==null ? 0 : data.length*8);
					dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
					for (int i = 0; data!=null && i < data.length; i++) {
						dataBuffer.putLong(data[i]);
					}

					byte[] creationBytes = BurstCrypto.getInstance().getATCreationBytes((short)2,
							contract.getCode(), dataBuffer.array(), (short)contract.getDataPages(), (short)1, (short)1,
							BurstValue.fromPlanck(SellContract.ACTIVATION_FEE));

					Single<TransactionBroadcast> tx = g.getNS().generateCreateATTransaction(g.getPubKey(),
							BT.getMinRegisteringFee(contract),
							Constants.BURST_EXCHANGE_DEADLINE, "BTDEX" + (isBuy ? "buy" : "sell"), "BTDEX contract " + System.currentTimeMillis(), creationBytes)
							.flatMap(unsignedTransactionBytes -> {
								byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
								return g.getNS().broadcastTransaction(signedTransactionBytes);
							});

					TransactionBroadcast tb = tx.blockingGet();
					tb.getTransactionId();
					setVisible(false);
					// Main.getInstance().showTransactionsPanel();

					Toast.makeText((JFrame) this.getOwner(),
							tr("send_tx_broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();	
				}
			}
			catch (Exception ex) {
				Toast.makeText((JFrame) this.getOwner(), ex.getMessage(), Toast.Style.ERROR).display(okButton);
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		Integer ncontracts = Integer.parseInt(numOfContractsSpinner.getValue().toString());
		StringBuilder terms = new StringBuilder();
		terms.append(tr("reg_terms", ncontracts,
				tr(isBuy ? "reg_buying" : "reg_selling"),
				NumberFormatting.BURST.format(BT.getMinRegisteringFee(contract).longValue())));
		terms.append("\n\n").append(tr("reg_terms_closing",
				tr(isBuy ? "reg_buying" : "reg_selling") ));
		conditions.setText(terms.toString());
		conditions.setCaretPosition(0);
	}
}
