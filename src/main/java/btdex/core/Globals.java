package btdex.core;

import java.io.IOException;

import bt.BT;
import bt.compiler.Compiler;
import btdex.sm.SellContract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstID;
import burst.kit.service.BurstNodeService;

public class Globals {
	
	public static BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_LOCAL_TESTNET);
	public static BurstCrypto BC = BurstCrypto.getInstance();
	
	public static boolean IS_TESTNET = true;

	public static Compiler contract;

	/** Back-up arbitrator */
	public static final BurstID ARBITRATOR_BAKCUP = BC.rsDecode("TMSU-YBH5-RVC7-6J6WJ");

	/** Arbitrator list to choose randomly from */
	public static final BurstID[] ARBITRATORS = {
			BC.rsDecode("GFP4-TVNR-S7TY-E5KAY"),
			// TODO: add other arbitrators here
			
			ARBITRATOR_BAKCUP,
	};

	static {
		try {
			contract = new Compiler(SellContract.class);
			contract.compile();
			contract.link();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void setNode(String node) {
		if(node.contains("6876"))
			IS_TESTNET = true;
		
		NS = BurstNodeService.getInstance(node);
	}
	
	public static boolean isArbitratorAccepted(long arb) {
		for (BurstID arbi : ARBITRATORS) {
			if(arbi.getSignedLongId() == arb)
				return true;
		}
		return false;
	}

	public static long MARKET_BTC            = 0x000000001;
	public static long MARKET_LTC            = 0x000000002;
	public static long MARKET_ETH            = 0x000000003;
	// TODO: fill with other cryptos here
	
	public static long MARKET_USD            = 0x000001000;
	public static long MARKET_EUR            = 0x000002000;
	public static long MARKET_BRL            = 0x000003000;
	// TODO: fill with other fiat currencies here
	
	public static long MARKET_MASK           = 0x0000fffff;
	
	public static long TRANSFER_SAME_BANK    = 0x000100000;
	public static long TRANSFER_NATIONAL_BANK= 0x000200000;
	public static long TRANSFER_SEPA         = 0x000300000;
	public static long TRANSFER_SEPA_INST    = 0x000400000;
	public static long TRANSFER_ZELLE        = 0x000500000;
	// TODO: fill with other FIAT transfer methods
	public static long TRANSFER_MASK         = 0x00ff00000;
}
