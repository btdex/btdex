package btdex.core;

import com.google.gson.Gson;

import burst.kit.crypto.BurstCrypto;
import okhttp3.MediaType;

public class Constants {
    public static final String NODE_LOCALHOST = "http://localhost:8125";
    public static final String NODE_TESTNET2 = "https://testnet-2.burst-alliance.org:6876";

    public static final String PROP_LANG = "lang";
    public static final String PROP_NODE = "node";
    public static final String PROP_TESTNET = "testnet";
    public static final String PROP_LEDGER_ENABLED = "ledgerEnabled";
    public static final String PROP_ACCOUNT = "account";
    public static final String PROP_ENC_PRIVKEY = "encPrivKey";
    public static final String PROP_PUBKEY = "pubKey";
    public static final String PROP_LEDGER = "ledger";

    public static final String PROP_USER_TOKEN_ID = "userTokenID";
    
    public static final String PROP_EXPLORER = "explorer";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    public static final Gson GSON = new Gson();

    public static final String FAUCET_TESTNET =
            "https://burst-account-activator-testnet.ohager.now.sh/api/activate";
    public static final String FAUCET = "https://burst-account-activator.now.sh/api/activate";
    
    public static final String CHECK_BLOCK_TESTNET = "12645549025663958301";
    public static final String CHECK_BLOCK = "8868708821622932189";
    public static final int CHECK_HEIGHT = 150_000;    

    public static final String DEF_CONF_FILE = "config.properties";

    public static final String[] MEDIATORS = {"TLYF-7EBX-FBLY-DFX86", "P3D9-QX3S-7YHZ-BYLZD"};
    public static final String[] MEDIATORS_TESTNET = {"6ET8-WUKM-3HS8-CN4KM", "D3S9-8L56-UMLL-6EDFX", "E9UA-FX37-CHPE-568RD"};

    // FIXME: create a new secure address
    public static final long FEE_CONTRACT = BurstCrypto.getInstance().rsDecode("G4XE-MB8T-WWZC-E4GFU").getSignedLongId();
    
    public static final long FEE_CONTRACT_TESTNET = BurstCrypto.getInstance().rsDecode("G4XE-MB8T-WWZC-E4GFU").getSignedLongId();
    
    public static final String BURST_SYMBOL = "\u0243";

    public static final String BURST_TICKER = "BURST";
    
    // Number of confirmations needed for SC update to be taken into account
    public static final int PRICE_NCONF = 1;
    
	public static final int FEE_QUANT = 735000;
	
	// Deadline in minutes to be used on transactions
	public static final int BURST_DEADLINE = 1440;

    public static final String RELEASES_LINK = "https://github.com/btdex/btdex/releases";
    public static final String WEBSITE_LINK = "https://btdex.trade";
    public static final String DISCORD_LINK = "https://discord.gg/VQ6sFAY";
    public static final String REDDIT_LINK = "https://www.reddit.com/r/BTDEX/";
    public static final String GITHUB_LINK = "https://github.com/btdex/btdex";

    public static final int ICON_SIZE = 24;
    public static final int ICON_SIZE_SMALL = 18;

    public static final int UI_UPDATE_INTERVAL = 10000;

}
