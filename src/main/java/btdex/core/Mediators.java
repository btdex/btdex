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
    	Globals g = Globals.getInstance();
        Random rand = new Random();
        BurstID[] randomMediators = new BurstID[2];
        
        randomMediators[0] = mediators[rand.nextInt(mediators.length)];
        while(randomMediators[0].getSignedLongId() == g.getAddress().getSignedLongId()) {
            // make sure we don't mediate our own contract
            randomMediators[1] = mediators[rand.nextInt(mediators.length)];
        }
        randomMediators[1] = mediators[rand.nextInt(mediators.length)];
        while(randomMediators[1] == randomMediators[0] ||
        		randomMediators[1].getSignedLongId() == g.getAddress().getSignedLongId()) {
            // make sure we have 2 different mediators and that we do not mediate our own contract
            randomMediators[1] = mediators[rand.nextInt(mediators.length)];
        }
        return randomMediators;
    }

    private boolean isMediatorAccepted(ContractState contract, long mediator) {
    	if(contract.getCreator().getSignedLongId() == mediator)
    		return false;
    	
        for (BurstID m : mediators) {
            if(m.getSignedLongId() == mediator)
                return true;
        }
        return false;
    }
    
    public boolean areMediatorsAccepted(ContractState contract) {
        return isMediatorAccepted(contract, contract.getMediator1())
        		&& isMediatorAccepted(contract, contract.getMediator2());
    }
}
