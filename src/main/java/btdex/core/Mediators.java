package btdex.core;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstID;

public class Mediators {
    private BurstID[] mediators;

    public Mediators(Boolean testnet) {
        String[] mediators = (testnet) ? Constants.MEDIATORS_TESTNET : Constants.MEDIATORS;

        this.mediators = convertStringToBurstID(mediators);
    }

    private BurstID[] convertStringToBurstID (String[] md) {
        BurstCrypto BC = BurstCrypto.getInstance();
        int mediatorsCount = md.length;
        BurstID[] converted = new BurstID[mediatorsCount];
        for (int i = 0; i < mediatorsCount; i++) {
            converted[i] = BC.rsDecode(md[i]);
        }
        return converted;
    }

    public BurstID[] getMediators() {
        return mediators;
    }

    public boolean isMediatorAccepted(long mediator) {
        for (BurstID m : mediators) {
            if(m.getSignedLongId() == mediator)
                return true;
        }
        return false;
    }
}
