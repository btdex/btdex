package btdex.ui;

import btdex.core.Globals;

/**
 * Class handling an *open explorer* action.
 * 
 * By default, this implementation just copy to clipboard the given argument.
 * 
 * @author jjos
 *
 */
public class ExplorerWrapper {
	
	private String baseURL, key;
	private String accountPath, transactionPath;
	
	public static final String BURSTCOIN_NETWORK = "burstcoin.network";
	public static final String BURSTCOIN_RO = "burstcoin.ro";
	public static final String BURST_DEVTRUE = "burst.devtrue";
	
	public static ExplorerWrapper getExplorer(String exp) {
		if(exp!=null) {
			switch (exp) {
			case BURSTCOIN_NETWORK:
				return burstcoinNetwork();
			case BURST_DEVTRUE:
				return burstDevtrue();
			}
		}
		return burstcoinRo();
	}
	
	public static ExplorerWrapper burstcoinNetwork() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"https://testnet.explorer.burstcoin.network" : "https://explorer.burstcoin.network";
		return new ExplorerWrapper(BURSTCOIN_NETWORK, baseURL,
				"/?action=account&account=", "/?action=transaction&id=");
	}

	public static ExplorerWrapper burstcoinRo() {
		return new ExplorerWrapper(BURSTCOIN_RO, "https://explore.burstcoin.ro",
				"/account/", "/transaction/");
	}

	public static ExplorerWrapper burstDevtrue() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"http://explorer.testnet.burst.devtrue.net" : "https://explorer.burst.devtrue.net";
		return new ExplorerWrapper(BURST_DEVTRUE, baseURL,
				"/address/", "/tx/");
	}

	public ExplorerWrapper(String key, String baseURL, String accountPath, String transactionPath) {
		this.key = key;
		this.baseURL = baseURL;
		this.accountPath = accountPath;
		this.transactionPath = transactionPath;
	}
	
	@Override
	public String toString() {
		return key;
	}
	
	public String getKey() {
		return key;
	}
	
	public String openAddress(String addressRS, String addressId) {
		return baseURL + accountPath + addressId;
	}
	
	public String openTransaction(String transaction) {
		return baseURL + transactionPath + transaction;
	}
}
