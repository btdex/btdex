package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import btdex.core.Globals;
import btdex.core.NumberFormatting;
import btdex.markets.MarketBurstToken;
import btdex.ui.orderbook.TokenMarketPanel;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class CreateTokenDialog extends JDialog implements ActionListener, ChangeListener, DocumentListener {
	private static final long serialVersionUID = 1L;

	JTextPane conditions;
	JCheckBox acceptBox;

	JSpinner numOfDecimalPlacesSpinner;
	JTextField tickerField;
	JTextField totalSupplyField;
	JTextArea descriptionField;

	JPasswordField pin;

	private JButton okButton;
	private JButton cancelButton;

	private long totalSupplyLong;

	private int ndecimals;

	private String ticker;
	
	private int returnValue = JOptionPane.OK_OPTION;

	private TokenMarketPanel orderBook;

	public CreateTokenDialog(TokenMarketPanel orderBook) {
		super((JFrame) SwingUtilities.getWindowAncestor(orderBook), ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.orderBook = orderBook;
		
		setTitle(tr("token_create"));

		conditions = new JTextPane();
		conditions.setPreferredSize(new Dimension(240, 180));
		acceptBox = new JCheckBox(tr("dlg_accept_terms"));
		
		JPanel detailsPanel = new JPanel(new BorderLayout());
		detailsPanel.setBorder(BorderFactory.createTitledBorder(tr("token_details")));

		tickerField = new JTextField(10);
		tickerField.getDocument().addDocumentListener(this);
		// The number of decimal places
		SpinnerNumberModel numModel = new SpinnerNumberModel(4, 0, 8, 1);
		numOfDecimalPlacesSpinner = new JSpinner(numModel);
		numOfDecimalPlacesSpinner.addChangeListener(this);
		
		totalSupplyField = new JFormattedTextField(NumberFormatting.BURST.getFormat());
		totalSupplyField.getDocument().addDocumentListener(this);
		
		descriptionField = new JTextArea(4, 20);
		descriptionField.setLineWrap(true);
		
		JPanel topDetails = new JPanel(new GridLayout(0, 3, 4, 4));
		topDetails.add(new Desc(tr("token_ticker"), tickerField));
		topDetails.add(new Desc(tr("token_supply"), totalSupplyField));
		topDetails.add(new Desc(tr("token_decimal_places"), numOfDecimalPlacesSpinner));
		detailsPanel.add(topDetails, BorderLayout.PAGE_START);
		detailsPanel.add(new Desc(tr("token_description"), descriptionField), BorderLayout.PAGE_END);

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

		content.add(detailsPanel, BorderLayout.PAGE_START);
		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		stateChanged(null);
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
			returnValue = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && (ticker==null || ticker.length()<3 || ticker.length()>8)) {
				error = tr("token_invalid_ticker");
			}
			
			if(error == null && totalSupplyLong <= 0) {
				error = tr("token_invalid_supply");
			}

			if(error == null && (descriptionField.getText().length()>1000)) {
				error = tr("token_max_desc_length");
			}
			
			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

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
				
				Single<TransactionBroadcast> tx = g.getNS().generateIssueAssetTransaction(g.getPubKey(), ticker,
						descriptionField.getText(), BurstValue.fromPlanck(totalSupplyLong), ndecimals, BurstValue.fromBurst(1000), 1000)
						.flatMap(unsignedTransactionBytes -> {
							byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
							return g.getNS().broadcastTransaction(signedTransactionBytes);
						});
				TransactionBroadcast tb = tx.blockingGet();
				setCursor(Cursor.getDefaultCursor());

				JOptionPane.showMessageDialog(getParent(),
						tr("token_create_success", ticker, tb.getTransactionId().getID()),
						tr("token_create"), JOptionPane.INFORMATION_MESSAGE);
				
				setVisible(false);
				
				MarketBurstToken newMarket = new MarketBurstToken(tb.getTransactionId(), ticker, ndecimals);
				orderBook.addMarket(newMarket);
			}
			catch (Exception ex) {
				Toast.makeText((JFrame) this.getOwner(), ex.getMessage(), Toast.Style.ERROR).display(okButton);
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	public int getReturnValue() {
		return returnValue;
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		somethingChanged();
	}
	
	private void somethingChanged() {
		try {
			ticker = tickerField.getText().trim().toUpperCase();
			
			ndecimals = Integer.parseInt(numOfDecimalPlacesSpinner.getValue().toString());
			Number totalSupply = NumberFormatting.parse(totalSupplyField.getText());
			
			totalSupplyLong = (long)(totalSupply.doubleValue() * Math.pow(10d, ndecimals));

			StringBuilder terms = new StringBuilder();
			terms.append(tr("token_terms",
					ticker,
					1000,
					totalSupply,
					ndecimals));
			conditions.setText(terms.toString());
			conditions.setCaretPosition(0);
		}
		catch (Exception e) {
			conditions.setText("");
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		somethingChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		somethingChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		somethingChanged();
	}
}
