package btdex.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.google.gson.JsonObject;

import bt.BT;
import btdex.markets.MarketBurstToken;
import btdex.ui.ExplorerWrapper;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.service.BurstNodeService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Globals {

	private BurstNodeService NS;
	public static final BurstCrypto BC = BurstCrypto.getInstance();

	static String confFile = Constants.DEF_CONF_FILE;
	private Properties conf = new Properties();

	private ArrayList<MarketAccount> accounts = new ArrayList<>();

	private boolean testnet = false;
	private BurstAddress address;
	private int ledgerIndex;
	
	private Mediators mediators;

	static Globals INSTANCE;

	public static Globals getInstance() {
		if(INSTANCE==null)
			INSTANCE = new Globals();
		return INSTANCE;
	}
	public static void setConfFile(String file) {
		confFile = file;
	}

	public Globals() {
		try {
			// Read properties from file
			File f = new File(confFile);
			if (f.exists() && f.isFile()) {
				try {
					FileInputStream input = new FileInputStream(confFile);
					conf.load(input);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			testnet = Boolean.parseBoolean(conf.getProperty(Constants.PROP_TESTNET, "false"));
			setNode(conf.getProperty(Constants.PROP_NODE, isTestnet() ? Constants.NODE_TESTNET2 : BT.NODE_BURSTCOIN_RO));
			BT.activateCIP20(true);
			
			// possible ledger account index
			ledgerIndex = Integer.parseInt(conf.getProperty(Constants.PROP_LEDGER, "-1"));

			// load the markets
			Markets.loadStandardMarkets(testnet, NS);
			loadUserMarkets();
			
			mediators = new Mediators(testnet);

			checkPublicKey();

			loadAccounts();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean usingLedger() {
		return ledgerIndex >= 0;
	}
	
	public int getLedgerIndex() {
		return ledgerIndex;
	}
	
	public Mediators getMediators() {
		return mediators;
	}

	private void checkPublicKey() {
		String publicKeyStr = conf.getProperty(Constants.PROP_PUBKEY);
		if(publicKeyStr == null || publicKeyStr.length()!=64) {
			// no public key or invalid, show the welcome screen
			conf.remove(Constants.PROP_PUBKEY);
		}
		else {
			// get the updated public key and continue
			publicKeyStr = conf.getProperty(Constants.PROP_PUBKEY);
			byte []publicKey = BC.parseHexString(publicKeyStr);
			address = BC.getBurstAddressFromPublic(publicKey);
		}
	}

	public String getNode() {
		return conf.getProperty(Constants.PROP_NODE);
	}

	public void saveConfs() throws Exception {
		File f = new File(confFile);
		if(f.getParentFile()!=null)
			f.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(f);
		conf.store(fos, "BTDEX configuration file, private key is encrypted, only edit if you know what you're doing");
	}

	public void clearConfs() throws Exception {
		File f = new File(confFile);
		f.delete();
	}

	/**
	 * Set the public key and index of a ledger device
	 * @param pubKey
	 * @param index
	 */
	public void setKeys(byte []pubKey, int index) {
		conf.setProperty(Constants.PROP_PUBKEY, Globals.BC.toHexString(pubKey));
		conf.setProperty(Constants.PROP_LEDGER, Integer.toString(index));
		this.ledgerIndex = index;

		address = BC.getBurstAddressFromPublic(pubKey);
	}
	
	public void setKeys(byte []pubKey, byte []privKey, char []pin) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		byte[] encPrivKey = Globals.BC.aesEncrypt(privKey, pinKey);

		conf.setProperty(Constants.PROP_PUBKEY, Globals.BC.toHexString(pubKey));
		conf.setProperty(Constants.PROP_ENC_PRIVKEY, Globals.BC.toHexString(encPrivKey));

		address = BC.getBurstAddressFromPublic(pubKey);
	}

	/**
	 * @param pin
	 * @return true for a correct pin, false otherwise
	 */
	public boolean checkPIN(char []pin) {
		try {
			byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
			String encPrivKey = conf.getProperty(Constants.PROP_ENC_PRIVKEY);
			byte []privKey = BC.aesDecrypt(BC.parseHexString(encPrivKey), pinKey);
			String pubKey = BC.toHexString(BC.getPublicKey(privKey));

			return pubKey.equals(conf.getProperty(Constants.PROP_PUBKEY));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Signs a transaction using the user PIN (not by hardware wallet device)
	 * @param pin
	 * @param unsigned
	 * @return
	 * @throws Exception
	 */
	public byte[] signTransaction(char []pin, byte[]unsigned) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		String encPrivKey = conf.getProperty(Constants.PROP_ENC_PRIVKEY);
		byte []privKey = BC.aesDecrypt(BC.parseHexString(encPrivKey), pinKey);

		return BC.signTransaction(privKey, unsigned);
	}

	public ArrayList<MarketAccount> getMarketAccounts() {
		return accounts;
	}

	private void loadAccounts() {
		// load the accounts
		int i = 1;
		while(true) {
			String accountMarket = conf.getProperty(Constants.PROP_ACCOUNT + i, null);
			if(accountMarket == null || accountMarket.length()==0)
				break;

			String accountName = conf.getProperty(Constants.PROP_ACCOUNT + i + ".name", null);

			Market m = null;
			ArrayList<Market> markets = Markets.getMarkets();
			for (int j = 0; j < markets.size(); j++) {
				if(markets.get(j).toString().equals(accountMarket)) {
					m = markets.get(j);
					break;
				}
			}
			if(m == null)
				break;

			ArrayList<String> fieldNames = m.getFieldKeys();
			HashMap<String, String> fields = new HashMap<>();
			for(String key : fieldNames) {
				fields.put(key, conf.getProperty(Constants.PROP_ACCOUNT + i + "." + key, ""));
			}
			if(accountName == null)
				accountName = m.simpleFormat(fields);

			accounts.add(new MarketAccount(accountMarket, accountName, fields));

			i++;
		}
	}

	private void saveAccounts() {
		int i = 1;
		for (; i <= accounts.size(); i++) {
			MarketAccount ac = accounts.get(i-1);
			conf.setProperty(Constants.PROP_ACCOUNT + i, ac.getMarket());
			conf.setProperty(Constants.PROP_ACCOUNT + i + ".name", ac.getName());

			HashMap<String, String> fields = ac.getFields();
			for(String key : fields.keySet()) {
				conf.setProperty(Constants.PROP_ACCOUNT + i + "." + key, fields.get(key));
			}
		}
		// null terminated list
		conf.setProperty(Constants.PROP_ACCOUNT + i, "");

		try {
			saveConfs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void loadUserMarkets() {
		int userToken = 1;
		while(true) {
			String userTokenID = conf.getProperty(Constants.PROP_USER_TOKEN_ID + userToken);
			if(userTokenID == null || userTokenID.length() == 0)
				break;
			
			try {
				Market userTokenMarket = new MarketBurstToken(userTokenID, NS);
				addUserMarket(userTokenMarket, false);
			}
			catch (Exception e) {
			}
			userToken++;
		}
	}
	
	private void saveUserMarkets() {
		int i = 1;
		for (; i <= Markets.getUserMarkets().size(); i++) {
			Market ac = Markets.getUserMarkets().get(i-1);
			conf.setProperty(Constants.PROP_USER_TOKEN_ID + i, ac.getTokenID().getID());
		}
		// null terminated list
		conf.setProperty(Constants.PROP_USER_TOKEN_ID + i, "");

		try {
			saveConfs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeUserMarket(Market m, boolean save) {
		Markets.removeUserMarket(m);
		if(save)
			saveUserMarkets();
	}
	
	public void addUserMarket(Market m, boolean save) {
		Markets.addUserMarket(m);
		if(save)
			saveUserMarkets();
	}

	public void addAccount(MarketAccount ac) {
		accounts.add(ac);
		saveAccounts();
	}

	public void removeAccount(int index) {
		accounts.remove(index);
		saveAccounts();
	}

	public BurstAddress getAddress() {
		return address;
	}
	
	public byte[] getPubKey() {
		return BC.parseHexString(conf.getProperty(Constants.PROP_PUBKEY));
	}

	public BurstNodeService getNS() {
		return NS;
	}

	public void setLanguage(String lang) {
		conf.setProperty(Constants.PROP_LANG, lang);
	}
	
	public String getLanguage() {
		return conf.getProperty(Constants.PROP_LANG);
	}

	public void setNode(String node) {
		conf.setProperty(Constants.PROP_NODE, node);

		NS = BurstNodeService.getInstance(node);
	}
	
	public String getExplorer() {
		return conf.getProperty(Constants.PROP_EXPLORER, ExplorerWrapper.BURST_DEVTRUE);
	}
	
	public void setExplorer(String value) {
		conf.setProperty(Constants.PROP_EXPLORER, value);
	}
	
	public Response activate() throws IOException {
		OkHttpClient client = new OkHttpClient();

		JsonObject params = new JsonObject();
		params.addProperty("account", getAddress().getID());
		params.addProperty("publickey", BC.toHexString(getPubKey()));

		RequestBody body = RequestBody.create(Constants.JSON, params.toString());

		String faucet = isTestnet() ? Constants.FAUCET_TESTNET : Constants.FAUCET;
		Request request = new Request.Builder()
		        .url(faucet)
		        .post(body)
		        .build();

		return client.newCall(request).execute();
	}

	public boolean isTestnet() {
		return testnet;
	}
}
