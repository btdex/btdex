package btdex.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;

import bt.BT;
import bt.compiler.Compiler;
import btdex.sm.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.service.BurstNodeService;

public class Globals {

	BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_BURST_TEAM);
	public static final BurstCrypto BC = BurstCrypto.getInstance();

	static final String CONF_FILE = "config.properties";

	public static final String PROP_NODE = "node";
	public static final String PROP_ENC_PRIVKEY = "encPrivKey";
	public static final String PROP_PUBKEY = "pubKey";
	
	public static final NumberFormat NF = NumberFormat.getInstance(Locale.ENGLISH);
	public static final NumberFormat NF_FULL = NumberFormat.getInstance(Locale.ENGLISH);
	static {
		NF.setMinimumFractionDigits(4);
		NF.setMaximumFractionDigits(4);
		
		NF_FULL.setMinimumFractionDigits(4);
		NF_FULL.setMaximumFractionDigits(8);
		
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ENGLISH);
		s.setGroupingSeparator('\'');
		((DecimalFormat)NF).setDecimalFormatSymbols(s);
		((DecimalFormat)NF_FULL).setDecimalFormatSymbols(s);
	}

	Properties conf = new Properties();

	boolean IS_TESTNET = false;
	private BurstAddress address;

	public static Compiler contract;

	/** Back-up arbitrator */
	public static final BurstID ARBITRATOR_BAKCUP = BC.rsDecode("TMSU-YBH5-RVC7-6J6WJ");

	/** Arbitrator list to choose randomly from */
	public static final BurstID[] ARBITRATORS = {
			BC.rsDecode("GFP4-TVNR-S7TY-E5KAY"),
			// TODO: add other arbitrators here

			ARBITRATOR_BAKCUP,
	};

	static final Globals INSTANCE = new Globals();

	public static Globals getInstance() {
		return INSTANCE;
	}

	public static Properties getConf() {
		return getInstance().conf;
	}

	public Globals() {
		try {
			// Read properties from file
			FileInputStream input = new FileInputStream(CONF_FILE);
			conf.load(input);

			setNode(conf.getProperty(PROP_NODE, BT.NODE_BURST_TEAM));

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
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		try {

			contract = new Compiler(SellContract.class);
			contract.compile();
			contract.link();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveConfs() throws Exception {
		FileOutputStream f = new FileOutputStream(CONF_FILE);
		getInstance().conf.store(f, "BTDEX configuration file, only edit if you know what you're doing");
	}

	public void setKeys(byte []pubKey, byte []privKey, char []pin) throws Exception {
		byte[] pinKey = BC.getSha256().digest(new String(pin).getBytes("UTF-8"));
		byte[] encPrivKey = Globals.BC.aesEncrypt(privKey, pinKey);

		Globals.getConf().setProperty(Globals.PROP_PUBKEY, Globals.BC.toHexString(pubKey));
		Globals.getConf().setProperty(Globals.PROP_ENC_PRIVKEY, Globals.BC.toHexString(encPrivKey));

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

	public BurstAddress getAddress() {
		return address;
	}

	public byte[] getPubKey() {
		return BC.parseHexString(conf.getProperty(PROP_PUBKEY));
	}

	public Compiler getContract() {
		return contract;
	}

	public BurstNodeService getNS() {
		return NS;
	}

	public void setNode(String node) {
		conf.setProperty("node", node);
		if(node.contains("6876"))
			IS_TESTNET = true;

		NS = BurstNodeService.getInstance(node);
	}

	public boolean isArbitratorAccepted(long arb) {
		for (BurstID arbi : ARBITRATORS) {
			if(arbi.getSignedLongId() == arb)
				return true;
		}
		return false;
	}

}