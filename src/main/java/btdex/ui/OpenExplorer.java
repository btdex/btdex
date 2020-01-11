package btdex.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import btdex.core.Globals;

/**
 * Class handling an *open explorer* action.
 * 
 * By default, this implementation just copy to clipboard the given argument.
 * 
 * @author jjos
 *
 */
public class OpenExplorer {
	
	private String baseURL, desc, key;
	private String accountPath, transactionPath;
	
	public static final String CLIPBOARD = "clipboard";
	public static final String BURSTCOIN_NETWORK = "burstcoin_network";
	public static final String BURSTCOIN_RO = "burstcoin_ro";
	public static final String BURST_DEVTRUE = "burst_devtrue";
	
	public static OpenExplorer getExplorer(String exp) {
		if(exp!=null) {
			switch (exp) {
			case CLIPBOARD:
				return clipboard();
			case BURSTCOIN_NETWORK:
				return burstcoinNetwork();
			case BURST_DEVTRUE:
				return burstDevtrue();
			}
		}
		return burstcoinRo();
	}
	
	public static OpenExplorer clipboard() {
		return new OpenExplorer(CLIPBOARD, "Copy to clipboard", null, null, null);
	}

	public static OpenExplorer burstcoinNetwork() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"https://testnet.explorer.burstcoin.network" : "https://explorer.burstcoin.network";
		return new OpenExplorer(BURSTCOIN_NETWORK, baseURL, baseURL,
				"/?action=account&account=", "/?action=transaction&id=");
	}

	public static OpenExplorer burstcoinRo() {
		return new OpenExplorer(BURSTCOIN_RO, "https://explore.burstcoin.ro", "https://explore.burstcoin.ro",
				"/account/", "/transaction/");
	}

	public static OpenExplorer burstDevtrue() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"http://explorer.testnet.burst.devtrue.net" : "https://explorer.burst.devtrue.net";
		return new OpenExplorer(BURST_DEVTRUE, baseURL, baseURL,
				"/address/", "/tx/");
	}

	public OpenExplorer(String key, String desc, String baseURL, String accountPath, String transactionPath) {
		this.key = key;
		this.desc = desc;
		this.baseURL = baseURL;
		this.accountPath = accountPath;
		this.transactionPath = transactionPath;
	}
	
	@Override
	public String toString() {
		return desc;
	}
	
	public String getKey() {
		return key;
	}
	
	public void openAddress(String addressRS, String addressId) {
		if(baseURL!=null) {
			String url = baseURL + accountPath + addressId;
			Main.getInstance().browse(url);
		}
		else
			copyToClipboard(addressRS);
	}
	
	public void openTransaction(String transaction) {
		if(baseURL!=null) {
			String url = baseURL + transactionPath + transaction;
			Main.getInstance().browse(url);
		}
		else
			copyToClipboard(transaction);
	}
	
	private void copyToClipboard(String t) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection stringSelection = new StringSelection(t);
		clipboard.setContents(stringSelection, null);
		
		Toast.makeText(Main.getInstance(), t + " copied to clipboard.", Toast.Style.SUCCESS).display();
	}
}
