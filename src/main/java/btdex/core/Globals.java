package btdex.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import bt.BT;
import bt.compiler.Compiler;
import btdex.sm.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstID;
import burst.kit.service.BurstNodeService;

public class Globals {
	
	BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_BURST_TEAM);
	public static final BurstCrypto BC = BurstCrypto.getInstance();
	
	static final String CONF_FILE = "config.properties";
	
	public static final String PROP_NODE = "node";
	public static final String PROP_ENC_PASSPHRASE = "encryptedPassPhrase";
	public static final String PROP_PUBKEY = "pubkey";
		
	Properties conf = new Properties();
	
	boolean IS_TESTNET = false;

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
