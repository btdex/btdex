package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import btdex.core.Globals;
import btdex.ledger.LedgerService;
import btdex.ledger.LedgerService.PubKeyCallBack;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

public class Welcome extends JDialog implements ActionListener, PubKeyCallBack {
	private static final long serialVersionUID = 1L;

	private JTextArea passphrase;

	private JLabel introText;

	private JPasswordField pin;
	private JPasswordField pinCheck;

	private JCheckBox acceptBox, recoverBox, licenseBox;

	private JButton okButton;
	private JButton calcelButton;
	private JButton useLedgerButton;
	private JButton licenseButton;
	private boolean resetPin;

	private int ret;

	private static Logger logger = LogManager.getLogger();

	public Welcome(JFrame owner) {
		this(owner, false);
	}

	public Welcome(JFrame owner, boolean resetPin) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.resetPin = resetPin;

		setTitle(tr(resetPin ? "welc_reset_pin" : "welc_welcome"));
		setResizable(false);

		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		if(!resetPin) {
			JPanel introPanel = new JPanel(new BorderLayout());
			String title = "<html><h2>" + tr("welc_intro_header") + "</h2>";
			introPanel.add(new JLabel(title), BorderLayout.PAGE_START);
			String intro = "<html>" + tr("welc_intro_text") + "<br><br>" + tr("welc_intro_pin");
			introText = new JLabel(intro);
			introText.setPreferredSize(new Dimension(60, 120));
			introPanel.add(introText, BorderLayout.PAGE_END);

			useLedgerButton = new JButton(tr("welc_use_ledger"));
			useLedgerButton.addActionListener(this);
			if(Globals.getInstance().isLedgerEnabled())
				introPanel.add(useLedgerButton, BorderLayout.CENTER);
			topPanel.add(introPanel, BorderLayout.CENTER);
		}

		passphrase = new JTextArea(2, 40);
		passphrase.setWrapStyleWord(true);
		passphrase.setLineWrap(true);
		passphrase.setEditable(false);

		pin = new JPasswordField(12);
		pinCheck = new JPasswordField(12);

		acceptBox = new JCheckBox(tr("welc_wrote"));
		recoverBox = new JCheckBox(tr("welc_reuse"));

		recoverBox.addActionListener(this);

		JPanel recoveryPanel = new JPanel(new BorderLayout());
		recoveryPanel.setBorder(BorderFactory.createTitledBorder(tr(resetPin ?
				"welc_prhase_to_redefine" :
				"welc_recovery_phrase")));
		recoveryPanel.add(recoverBox, BorderLayout.PAGE_START);
		recoveryPanel.add(passphrase, BorderLayout.CENTER);
		recoveryPanel.add(acceptBox, BorderLayout.PAGE_END);

		topPanel.add(recoveryPanel, BorderLayout.PAGE_END);

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));
		getRootPane().setDefaultButton(okButton);

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		panel.add(new Desc(tr("dlg_pin"), pin));
		panel.add(new Desc(tr("welc_reenter_pin"), pinCheck));

		if(!resetPin) {
			licenseButton = new JButton(tr("welc_license"));
			licenseButton.addActionListener(this);
			licenseBox = new JCheckBox(tr("welc_accept_license"));
			buttonPane.add(licenseBox);
			buttonPane.add(licenseButton);
		}

		buttonPane.add(okButton);
		buttonPane.add(calcelButton);

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		content.add(topPanel, BorderLayout.PAGE_START);
		content.add(panel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		if(resetPin) {
			passphrase.setText("");
			passphrase.setEditable(true);
			passphrase.requestFocus();

			recoverBox.setVisible(false);
			acceptBox.setSelected(true);
			acceptBox.setVisible(false);
		}
		else
			newPass();
		pack();
		logger.trace("Welcome screen showed");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						acceptBox.requestFocusInWindow();
					}
				});
			}
		});
	}

	private void newPass() {
		String pass;
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
		pass = sb.toString();

		passphrase.setText(pass);
		logger.trace("New pasw set");
	}

	public int getReturn() {
		return ret;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == useLedgerButton) {

			JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 128, 1));
			JPanel spinnerPannel = new JPanel(new BorderLayout());
			spinnerPannel.add(new JLabel("<html>" + tr("ledger_account_index_message")), BorderLayout.CENTER);
			spinnerPannel.add(spinner, BorderLayout.PAGE_END);
			int option = JOptionPane.showOptionDialog(this, spinnerPannel, tr("ledger_account_index"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (option == JOptionPane.CANCEL_OPTION) {
				return;
			}
			int index = (Integer)spinner.getValue();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			LedgerService.getInstance().setCallBack(this, index);
			return;
		}
		if(e.getSource() == licenseButton) {
			JTextArea licenseText = new JTextArea(20, 45);
			try {
				InputStream stream = Welcome.class.getResourceAsStream("/license/LICENSE");
				
				String content = new BufferedReader(new InputStreamReader(stream))
				  .lines().collect(Collectors.joining("\n"));
				licenseText.append(content);
				licenseText.setCaretPosition(0);
			} catch (Exception e1) {
				logger.debug("Cannot read license file {}", e1.getLocalizedMessage());
			}
			JOptionPane.showMessageDialog(this, new JScrollPane(licenseText), tr("welc_license"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(e.getSource() == recoverBox) {
			if(recoverBox.isSelected()) {
				passphrase.setText("");
				passphrase.setEditable(true);
				passphrase.requestFocus();
			}
			else {
				passphrase.setEditable(false);
				newPass();
			}
			acceptBox.setSelected(recoverBox.isSelected());
			acceptBox.setEnabled(!recoverBox.isSelected());
		}
		if(e.getSource() == calcelButton) {
			ret = 0;
			setVisible(false);
		}
		if(e.getSource() == okButton) {
			String error = null;
			String phrase = passphrase.getText();

			if(!acceptBox.isSelected()) {
				error = tr("welc_write_phrase");
				acceptBox.requestFocus();
			}
			else if(phrase.length()==0) {
				error = tr("welc_phrase_empty");
				passphrase.requestFocus();
			}
			else if(pin.getPassword() == null || pin.getPassword().length < 4) {
				error = tr("welc_min_pin");
				pin.requestFocus();
			}
			else if(!Arrays.equals(pin.getPassword(), pinCheck.getPassword())) {
				pin.requestFocus();
				error = tr("welc_wrong_pin");
			}
			else if(licenseBox!=null && !licenseBox.isSelected()) {
				error = tr("welc_must_accept_license");
				licenseBox.requestFocus();
			}
			
			if(error == null) {
				Globals g = Globals.getInstance();

				// no error, so we have a new phrase
				byte[] privKey = Globals.BC.getPrivateKey(phrase);
				byte[] pubKey = Globals.BC.getPublicKey(privKey);

				try {
					if(resetPin) {
						if(!Arrays.equals(g.getPubKey(), pubKey)) {
							error = tr("welc_wrong_phrase");
						}
					}

					if(error == null) {
						g.setKeys(pubKey, privKey, pin.getPassword());
						g.saveConfs();
					}
				} catch (Exception e1) {
					error = e1.getMessage();
					logger.error("Error 1: {}", e1.getLocalizedMessage());
				}
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
			}
			else {
				ret = 1;
				setVisible(false);
			}
		}
	}

	@Override
	public void returnedError(String error) {
		// Clear the call back
		LedgerService.getInstance().setCallBack(null, 0);

		Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(useLedgerButton);
		setCursor(Cursor.getDefaultCursor());
	}

	@Override
	public void returnedKey(byte[] pubKey, int index) {
		Globals g = Globals.getInstance();
		g.setKeys(pubKey, index);

		Toast.makeText((JFrame) this.getOwner(), tr("ledger_show_address"),
				Toast.LENGTH_LONG, Toast.Style.NORMAL).display(useLedgerButton);
		try {
			Globals.getInstance().saveConfs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// all good
		ret = 1;
		setVisible(false);
	}
}
