package btdex.core;

import okhttp3.MediaType;

public class Constants {
    public static final String PROP_NODE = "node";
    public static final String PROP_TESTNET = "testnet";
    public static final String PROP_ACCOUNT = "account";
    public static final String PROP_ENC_PRIVKEY = "encPrivKey";
    public static final String PROP_PUBKEY = "pubKey";

    public static final String PROP_EXPLORER = "explorer";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final String FAUCET_TESTNET =
            "https://burst-account-activator-testnet.ohager.now.sh/api/activate";
    public static final String FAUCET = "https://burst-account-activator.now.sh/api/activate";

    public static final String DEF_CONF_FILE = "config.properties";

    public static final String[] MEDIATORS = {"TLYF-7EBX-FBLY-DFX86", "P3D9-QX3S-7YHZ-BYLZD"};
    public static final String[] MEDIATORS_TESTNET = {"TMSU-YBH5-RVC7-6J6WJ", "GFP4-TVNR-S7TY-E5KAY"};

    // FIXME: set the fee contract
    public static final long FEE_CONTRACT = 222222L;
    
    public static final String BURST_SYMBOL = "\u0243";

    public static final String BURST_TICKER = "BURST";

}
