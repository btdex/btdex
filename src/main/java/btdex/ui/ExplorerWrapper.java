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
	private String accountPath, transactionPath, tokenPath;

	public static final String BURSTCOIN_NETWORK = "burstcoin.network";
	public static final String BURSTCOIN_RO = "burstcoin.ro";
	public static final String BURSTSCAN_NET = "burstscan.net";

	public static ExplorerWrapper getExplorer(String exp) {
		if(exp!=null) {
			switch (exp) {
			case BURSTCOIN_RO:
				if(!Globals.getInstance().isTestnet())
					return burstcoinRo();
			case BURSTSCAN_NET:
				return burstScanNet();
			case BURSTCOIN_NETWORK:
				return burstcoinNetwork();
			}
		}
		return burstScanNet();
	}

	public static ExplorerWrapper burstcoinNetwork() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"https://testnet.explorer.burstcoin.network" : "https://explorer.burstcoin.network";
		return new ExplorerWrapper(BURSTCOIN_NETWORK, baseURL,
				"/?action=account&account=", "/?action=transaction&id=", "/?action=token_inspect&id=");
	}

	public static ExplorerWrapper burstcoinRo() {
		return new ExplorerWrapper(BURSTCOIN_RO, "https://explore.burstcoin.ro",
				"/account/", "/transaction/", "/asset/");
	}

	public static ExplorerWrapper burstScanNet() {
		String baseURL = Globals.getInstance().isTestnet() ?
				"https://testnet.burstscan.net" : "https://burstscan.net";
		return new ExplorerWrapper(BURSTSCAN_NET, baseURL,
				"/address/", "/tx/", "/asset/");
	}

	public ExplorerWrapper(String key, String baseURL, String accountPath, String transactionPath, String tokenPath) {
		this.key = key;
		this.baseURL = baseURL;
		this.accountPath = accountPath;
		this.transactionPath = transactionPath;
		this.tokenPath = tokenPath;
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
	public String openToken(String tokenID) {
		return baseURL + tokenPath + tokenID;
	}
}
