package btdex.core;

import com.google.gson.Gson;

import bt.Contract;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstID;
import okhttp3.MediaType;

public class Constants {
    public static final String NODE_LOCALHOST = "http://localhost:8125";
    public static final String NODE_TESTNET2 = "https://testnet-2.burst-alliance.org:6876";

    public static final String PROP_LANG = "lang";
    public static final String PROP_NODE = "node";
    public static final String PROP_LOGGER = "logger";
    public static final String PROP_TESTNET = "testnet";
    public static final String PROP_LEDGER_ENABLED = "ledgerEnabled";
    public static final String PROP_ACCOUNT = "account";
    public static final String PROP_ENC_PRIVKEY = "encPrivKey";
    public static final String PROP_PUBKEY = "pubKey";
    public static final String PROP_LEDGER = "ledger";
    public static final String PROP_API_PORT = "apiPort";    

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

    public static final String[] MEDIATORS = {"TLYF-7EBX-FBLY-DFX86", "P3D9-QX3S-7YHZ-BYLZD", "K9DB-72JS-2PLL-9U9JF", "T7MP-XCSN-RAPA-6VFBC"};
    public static final String[] MEDIATORS_TESTNET = {"6ET8-WUKM-3HS8-CN4KM", "D3S9-8L56-UMLL-6EDFX", "E9UA-FX37-CHPE-568RD"};

    public static final long FEE_CONTRACT = BurstCrypto.getInstance().rsDecode("BNR6-GMFS-S6CF-8XFGU").getSignedLongId();

    public static final long FEE_CONTRACT_TESTNET = BurstCrypto.getInstance().rsDecode("G4XE-MB8T-WWZC-E4GFU").getSignedLongId();
    
    public static final long TRT_DIVIDENDS = BurstID.fromLong("14893248166511032525").getSignedLongId();

    public static final String BURST_SYMBOL = "\u0243";

    public static final String BURST_TICKER = "BURST";

    // Number of confirmations needed for SC update to be taken into account
    public static final int PRICE_NCONF = 1;

	public static final int FEE_QUANT = 735000;

	// Deadline in minutes to be used on transactions
	public static final int BURST_SEND_DEADLINE = 1440;
	
	// Deadline in minutes to be used on exchange transactions
	public static final int BURST_EXCHANGE_DEADLINE = 20;

    public static final String RELEASES_LINK = "https://github.com/btdex/btdex/releases";
    public static final String WEBSITE_LINK = "https://btdex.trade";
    public static final String DISCORD_LINK = "https://discord.gg/VQ6sFAY";
    public static final String REDDIT_LINK = "https://www.reddit.com/r/BTDEX/";
    public static final String GITHUB_LINK = "https://github.com/btdex/btdex";

    public static final int ICON_SIZE = 24;
    public static final int ICON_SIZE_MED = 18;
    public static final int ICON_SIZE_SMALL = 12;

    public static final int UI_UPDATE_INTERVAL = 10000;
    
    public static final long MIN_OFFER = 10 * Contract.ONE_BURST;
    public static final long MAX_OFFER = 50_000 * Contract.ONE_BURST;

}
