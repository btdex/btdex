package btdex.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.binance.dex.api.client.BinanceDexEnvironment;
import com.binance.dex.api.client.Wallet;
import com.binance.dex.api.client.encoding.Crypto;
import com.google.gson.JsonObject;

import bt.BT;
import btdex.api.Server;
import btdex.markets.MarketBurstToken;
import btdex.ui.ExplorerWrapper;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.service.BurstNodeService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.bitcoinj.core.ECKey;

public class Globals {

	private BurstNodeService NS;
	public static final BurstCrypto BC = BurstCrypto.getInstance();

	static String confFile = Constants.DEF_CONF_FILE;
	private Properties conf = new Properties();

	private ArrayList<MarketAccount> accounts = new ArrayList<>();

	private Logger logger;

	private boolean ledgerEnabled = false;
	private boolean testnet = false;
	private BurstAddress address;
	private String bnbAddress;
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

			configLogger(conf.getProperty(Constants.PROP_LOGGER, "OFF"));
			logger = LogManager.getLogger();
			logger.debug("Logger configured");
			logger.debug("System properties: " + System.getProperties());
			
			logger.info("Using properties file {}", confFile);
			testnet = Boolean.parseBoolean(conf.getProperty(Constants.PROP_TESTNET, "false"));
			setNode(conf.getProperty(Constants.PROP_NODE, isTestnet() ? Constants.NODE_TESTNET : BT.NODE_BURSTCOIN_RO));
			BT.activateCIP20(true);

			// possible ledger account index
			ledgerEnabled = Boolean.parseBoolean(conf.getProperty(Constants.PROP_LEDGER_ENABLED, "false"));
			logger.debug("Conf. ledger enabled: {}", ledgerEnabled);
			ledgerIndex = Integer.parseInt(conf.getProperty(Constants.PROP_LEDGER, "-1"));
			logger.debug("Conf. ledger index: {}", ledgerIndex);

			// load the markets
			Markets.loadStandardMarkets(testnet, NS);
			loadUserMarkets();

			mediators = new Mediators(testnet);

			checkPublicKey();

			loadAccounts();
			
			int apiPort = Integer.parseInt(conf.getProperty(Constants.PROP_API_PORT, "-1"));
			if(apiPort > 0) {
				new Server(apiPort);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isLedgerEnabled() {
		return ledgerEnabled;
	}

	public boolean usingLedger() {
		return ledgerEnabled && ledgerIndex >= 0;
	}

	public int getLedgerIndex() {
		return ledgerIndex;
	}

	public long getFeeContract() {
		return isTestnet() ? Constants.FEE_CONTRACT_TESTNET : Constants.FEE_CONTRACT;
	}

	public Mediators getMediators() {
		return mediators;
	}

	private void checkPublicKey() {
		String publicKeyStr = conf.getProperty(Constants.PROP_PUBKEY);

		if(publicKeyStr == null || publicKeyStr.length()!=64) {
			// no public key or invalid, show the welcome screen
			logger.debug("No public key detected or its length not 64 char");
			conf.remove(Constants.PROP_PUBKEY);
		}
		else {
			byte[] publicKey = BC.parseHexString(publicKeyStr);
			address = BC.getBurstAddressFromPublic(publicKey);
			logger.debug("checkPublicKey() sets address to {}", address.getFullAddress());
			
			String ecKeyPubStr = conf.getProperty(Constants.PROP_ECKEY_PUB);
			if(ecKeyPubStr != null) {
				ECKey ecKey = ECKey.fromPublicOnly(BC.parseHexString(ecKeyPubStr));
				bnbAddress = Crypto.getAddressFromECKey(ecKey, isTestnet() ? BinanceDexEnvironment.TEST_NET.getHrp() :
					BinanceDexEnvironment.PROD.getHrp());
			}
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
		logger.debug("Config saved");
	}

	public void clearConfs() throws Exception {
		File f = new File(confFile);
		f.delete();
		logger.debug("Config cleared");
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
		logger.debug("Ledger keys set. Address from pubKey {}", address.getFullAddress());
	}

	public void setKeys(byte []pubKey, byte []privKey, char []pin) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		byte[] encPrivKey = Globals.BC.aesEncrypt(privKey, pinKey);

		conf.setProperty(Constants.PROP_PUBKEY, Globals.BC.toHexString(pubKey));
		conf.setProperty(Constants.PROP_ENC_PRIVKEY, Globals.BC.toHexString(encPrivKey));
		
		address = BC.getBurstAddressFromPublic(pubKey);
		logger.debug("Keys set. for address {}", address.getFullAddress());
		
		Wallet bnbWallet = new Wallet(BC.toHexString(privKey), isTestnet() ?
				BinanceDexEnvironment.TEST_NET : BinanceDexEnvironment.PROD);
		conf.setProperty(Constants.PROP_ECKEY_PUB, bnbWallet.getEcKey().getPublicKeyAsHex());
		bnbAddress = bnbWallet.getAddress();
		logger.debug("Keys set. for address {}", bnbAddress);
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
			Boolean result = pubKey.equals(conf.getProperty(Constants.PROP_PUBKEY));
			logger.debug("PIN checked, result: {}", result);
			return result;
		} catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
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

			if(accountMarket == null || accountMarket.length()==0) {
				logger.debug("No markets specified in the config file");
				break;
			}

			String accountName = conf.getProperty(Constants.PROP_ACCOUNT + i + ".name", null);

			Market m = null;
			ArrayList<Market> markets = Markets.getMarkets();
			for (int j = 0; j < markets.size(); j++) {
				if(markets.get(j).toString().equals(accountMarket)) {
					m = markets.get(j);
					logger.debug("Specified market {}", m.getID());
					break;
				}
			}
			if(m == null) {
				logger.warn("Specified market invalid or not in Markets");
				break;
			}

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
			logger.debug("Accounts saved");
		} catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private void loadUserMarkets() {
		int userToken = 1;
		while(true) {
			String userTokenID = conf.getProperty(Constants.PROP_USER_TOKEN_ID + userToken);
			if(userTokenID == null || userTokenID.length() == 0) {
				logger.debug("User tokens not set in config file");
				break;
			}

			try {
				Market userTokenMarket = new MarketBurstToken(userTokenID, NS);
				addUserMarket(userTokenMarket, false);
			}
			catch (Exception e) {
				logger.error("Error: {}", e.getLocalizedMessage());
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
			logger.error("Error: {}", e.getLocalizedMessage());
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
	
	public String getBinanceAddress() {
		return bnbAddress;
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
		return conf.getProperty(Constants.PROP_EXPLORER, ExplorerWrapper.BURSTCOIN_NETWORK);
	}

	public void setExplorer(String value) {
		conf.setProperty(Constants.PROP_EXPLORER, value);
	}

	public Response activate() throws IOException {
		OkHttpClient client = new OkHttpClient();

		JsonObject params = new JsonObject();
		params.addProperty("account", getAddress().getID());
		params.addProperty("publickey", BC.toHexString(getPubKey()));
		
		RequestBody body = RequestBody.create(params.toString(), Constants.JSON);

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

	private static void configLogger(String level) {
		level = level.trim().toUpperCase();
		Level l = Level.getLevel(level);
		if(l == null) {
			l = Level.OFF;
			System.out.println("Incorrect logging level, defaulting to OFF");
		}
		if(!l.equals(Level.OFF)){
			ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
			//These 2 lines for log4j config builder logging
			builder.setStatusLevel(Level.ERROR);
			builder.setConfigurationName("BTDEX");
			//Create a console appender
			AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
				ConsoleAppender.Target.SYSTEM_OUT);
			appenderBuilder.add(builder.newLayout("PatternLayout")
				.addAttribute("pattern", "%d [%t] %-5level: %logger %msg%n%throwable"));
			builder.add(appenderBuilder);
			// create a rolling file appender
			LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
				.addAttribute("pattern", "%d [%t] %-5level: %logger %msg%n");

			ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
				.addComponent(builder.newComponent("TimeBasedTriggeringPolicy"))
				.addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "1M"));
			appenderBuilder = builder.newAppender("rolling", "RollingFile")
				.addAttribute("fileName", "log/btdex.log")
				.addAttribute("filePattern", "log/archive/btdex-%d{yyyy-MM-dd}-%i.log")
				.add(layoutBuilder)
				.addComponent(triggeringPolicy);
			builder.add(appenderBuilder);

			builder.add(builder.newLogger("btdex", l)
				.add(builder.newAppenderRef("Stdout"))
				.add( builder.newAppenderRef("rolling"))
				.addAttribute("additivity", false));
			builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));

			Configurator.initialize(builder.build());
		}

	}

}
