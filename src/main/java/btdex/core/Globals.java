package btdex.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import com.google.gson.JsonObject;

import bt.BT;
import bt.compiler.Compiler;
import btdex.markets.MarketBRL;
import btdex.markets.MarketBTC;
import btdex.markets.MarketETH;
import btdex.markets.MarketEUR;
import btdex.markets.MarketLTC;
import btdex.markets.MarketNDST;
import btdex.markets.MarketTRT;
import btdex.markets.MarketXMR;
import btdex.sc.SellContract;
import btdex.sc.SellNoDepositContract;
import btdex.ui.ExplorerWrapper;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.service.BurstNodeService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Globals {

	BurstNodeService NS;
	public static final BurstCrypto BC = BurstCrypto.getInstance();
	
	static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	static final String FAUCET_TESTNET =
			"https://burst-account-activator-testnet.ohager.now.sh/api/activate";
	static final String FAUCET = "https://burst-account-activator.now.sh/api/activate";

	static final String DEF_CONF_FILE = "config.properties";

	public static final String PROP_NODE = "node";
	public static final String PROP_TESTNET = "testnet";
	public static final String PROP_ACCOUNT = "account";
	public static final String PROP_ENC_PRIVKEY = "encPrivKey";
	public static final String PROP_PUBKEY = "pubKey";
	
	public static final String PROP_EXPLORER = "explorer";
	
	ArrayList<Market> markets = new ArrayList<>();
	Market token;
	
	// FIXME: set the fee contract
	public final long feeContract = 222222L;
	
	public static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	public static final NumberFormat NF_FULL = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(2);
		NF.setMaximumFractionDigits(5);
		
		NF_FULL.setMinimumFractionDigits(5);
		NF_FULL.setMaximumFractionDigits(8);
		
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
		s.setGroupingSeparator('\'');
		((DecimalFormat)NF).setDecimalFormatSymbols(s);
		((DecimalFormat)NF_FULL).setDecimalFormatSymbols(s);
	}

	static String confFile = DEF_CONF_FILE;
	Properties conf = new Properties();
	
	ArrayList<Account> accounts = new ArrayList<>();

	boolean testnet = false;
	private BurstAddress address;
	private FeeSuggestion suggestedFee;

	static Compiler contract, contractNoDeposit;
	
	static byte[] contractCode;
	static byte[] contractNoDepositCode;

	/** Mediator list to choose randomly from */
	public static final BurstID[] MEDIATORS = {
			BC.rsDecode("TMSU-YBH5-RVC7-6J6WJ"),
			BC.rsDecode("GFP4-TVNR-S7TY-E5KAY"),
			// TODO: add other mediators here

	};

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
			
			testnet = Boolean.parseBoolean(conf.getProperty(PROP_TESTNET, "false"));
			setNode(conf.getProperty(PROP_NODE, isTestnet() ? BT.NODE_TESTNET : BT.NODE_BURSTCOIN_RO));
			
			markets.add(token = new MarketTRT());
			if(testnet) {
				markets.add(new MarketNDST());
			}
			markets.add(new MarketEUR());
			markets.add(new MarketBRL());
			markets.add(new MarketBTC());
			markets.add(new MarketETH());
			markets.add(new MarketLTC());
			markets.add(new MarketXMR());


			String publicKeyStr = conf.getProperty(Globals.PROP_PUBKEY);
			if(publicKeyStr == null || publicKeyStr.length()!=64) {
				// no public key or invalid, show the welcome screen
				conf.remove(PROP_PUBKEY);
			}
			else {
				// get the updated public key and continue
				publicKeyStr = conf.getProperty(Globals.PROP_PUBKEY);
				byte []publicKey = BC.parseHexString(publicKeyStr);
				address = BC.getBurstAddressFromPublic(publicKey);
			}
			loadAccounts();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		try {
			contract = new Compiler(SellContract.class);
			contract.compile();
			contract.link();
			
			contractNoDeposit = new Compiler(SellNoDepositContract.class);
			contractNoDeposit.compile();
			contractNoDeposit.link();
			
			contractCode = contract.getCode();
			contractNoDepositCode = contractNoDeposit.getCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Market> getMarkets(){
		return markets;
	}
	
	public Market getToken() {
		return token;
	}
	
	public String getNode() {
		return conf.getProperty(Globals.PROP_NODE);
	}
	
	public void updateSuggestedFee() {
		suggestedFee = getNS().suggestFee().blockingGet();
	}
	
	public FeeSuggestion getSuggestedFee() {
		return suggestedFee;
	}

	public void saveConfs() throws Exception {
		File f = new File(confFile);
		if(f.getParentFile()!=null)
			f.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(f);
		getInstance().conf.store(fos, "BTDEX configuration file, private key is encrypted, only edit if you know what you're doing");
	}

	public void clearConfs() throws Exception {
		File f = new File(confFile);
		f.delete();
	}

	public void setKeys(byte []pubKey, byte []privKey, char []pin) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		byte[] encPrivKey = Globals.BC.aesEncrypt(privKey, pinKey);

		conf.setProperty(Globals.PROP_PUBKEY, Globals.BC.toHexString(pubKey));
		conf.setProperty(Globals.PROP_ENC_PRIVKEY, Globals.BC.toHexString(encPrivKey));

		address = BC.getBurstAddressFromPublic(pubKey);
	}

	public boolean checkPIN(char []pin) {
		try {
			byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
			String encPrivKey = conf.getProperty(Globals.PROP_ENC_PRIVKEY);
			byte []privKey = BC.aesDecrypt(BC.parseHexString(encPrivKey), pinKey);
			String pubKey = BC.toHexString(BC.getPublicKey(privKey));

			return pubKey.equals(conf.getProperty(PROP_PUBKEY));
		} catch (Exception e) {
			return false;
		}
	}

	public byte[] signTransaction(char []pin, byte[]unsigned) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		String encPrivKey = conf.getProperty(Globals.PROP_ENC_PRIVKEY);
		byte []privKey = BC.aesDecrypt(BC.parseHexString(encPrivKey), pinKey);

		return BC.signTransaction(privKey, unsigned);
	}
	
	public ArrayList<Account> getAccounts() {
		return accounts;
	}
	
	private void loadAccounts() {
		// load the accounts
		int i = 1;
		while(true) {
			String accountMarket = conf.getProperty(PROP_ACCOUNT + i, null);
			if(accountMarket == null || accountMarket.length()==0)
				break;

			String accountName = conf.getProperty(PROP_ACCOUNT + i + ".name", null);

			Market m = null;
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
				fields.put(key, conf.getProperty(PROP_ACCOUNT + i + "." + key, ""));
			}
			if(accountName == null)
				accountName = m.simpleFormat(fields);
			
			accounts.add(new Account(accountMarket, accountName, fields));
			
			i++;
		}
	}
	
	private void saveAccounts() {
		int i = 1;
		for (; i <= accounts.size(); i++) {
			Account ac = accounts.get(i-1);
			conf.setProperty(PROP_ACCOUNT + i, ac.getMarket());
			conf.setProperty(PROP_ACCOUNT + i + ".name", ac.getName());
			
			HashMap<String, String> fields = ac.getFields();
			for(String key : fields.keySet()) {
				conf.setProperty(PROP_ACCOUNT + i + "." + key, fields.get(key));
			}
		}
		// null terminated list
		conf.setProperty(PROP_ACCOUNT + i, "");
		
		try {
			saveConfs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addAccount(Account ac) {
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
		return BC.parseHexString(conf.getProperty(PROP_PUBKEY));
	}

	public Compiler getContract() {
		return contract;
	}
	
	public Compiler getContractNoDeposit() {
		return contractNoDeposit;
	}
	
	public byte[] getContractCode() {
		return contractCode;
	}
	
	public byte[] getContractNoDepositCode() {
		return contractNoDepositCode;
	}

	public BurstNodeService getNS() {
		return NS;
	}

	public void setNode(String node) {
		conf.setProperty(PROP_NODE, node);

		NS = BurstNodeService.getInstance(node);
	}
	
	public String getExplorer() {
		return conf.getProperty(PROP_EXPLORER, ExplorerWrapper.BURST_DEVTRUE);
	}
	
	public void setExplorer(String value) {
		conf.setProperty(PROP_EXPLORER, value);
	}
	
	public Response activate() throws IOException {
		OkHttpClient client = new OkHttpClient();
		
		JsonObject params = new JsonObject();
		params.addProperty("account", getAddress().getID());
		params.addProperty("publickey", BC.toHexString(getPubKey()));
		
		RequestBody body = RequestBody.create(JSON, params.toString());
		
		String faucet = isTestnet() ? FAUCET_TESTNET : FAUCET;
		Request request = new Request.Builder()
		        .url(faucet)
		        .post(body)
		        .build();
		
		return client.newCall(request).execute();
	}
	
	public boolean isTestnet() {
		return testnet;
	}
	
	public long getFeeContract() {
		return feeContract;
	}

	public boolean isArbitratorAccepted(long arb) {
		for (BurstID arbi : MEDIATORS) {
			if(arbi.getSignedLongId() == arb)
				return true;
		}
		return false;
	}

}
