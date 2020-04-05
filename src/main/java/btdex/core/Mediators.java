package btdex.core;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstID;

import java.util.Random;

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

    public BurstID[] getTwoRandomMediators() {
        Random rand = new Random();
        BurstID[] randomMediators = new BurstID[2];
        randomMediators[0] = mediators[rand.nextInt(mediators.length)];
        randomMediators[1] = mediators[rand.nextInt(mediators.length)];
        while(randomMediators[0] == randomMediators[1]) {
            // make sure we have 2 different mediators
            randomMediators[1] = mediators[rand.nextInt(mediators.length)];
        }
        return randomMediators;
    }

    public boolean isMediatorAccepted(long mediator) {
        for (BurstID m : mediators) {
            if(m.getSignedLongId() == mediator)
                return true;
        }
        return false;
    }
}
