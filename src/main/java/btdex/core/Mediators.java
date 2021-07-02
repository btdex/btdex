package btdex.core;

import signumj.crypto.SignumCrypto;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class Mediators {
    private SignumID[] mediators;
    private SignumValue[] mediatorBalances;

    private static Logger logger = LogManager.getLogger();

    private static final SignumValue MIN_TRT = SignumValue.fromNQT(10_000L * 1_000_000L);

    public Mediators(Boolean testnet) {
        String[] mediators = (testnet) ? Constants.MEDIATORS_TESTNET : Constants.MEDIATORS;

        this.mediators = convertStringToSignumID(mediators);
        mediatorBalances = new SignumValue[mediators.length];
    }

    private SignumID[] convertStringToSignumID (String[] md) {
        SignumCrypto BC = SignumCrypto.getInstance();
        int mediatorsCount = md.length;
        SignumID[] converted = new SignumID[mediatorsCount];
        for (int i = 0; i < mediatorsCount; i++) {
            converted[i] = BC.rsDecode(md[i]);
        }

        return converted;
    }

    public SignumID[] getMediators() {
        return mediators;
    }

    public SignumID[] getTwoRandomMediators() {
    	Globals g = Globals.getInstance();
        Random rand = new Random();
        SignumID[] randomMediators = new SignumID[2];

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
        logger.debug("Two random mediators: {} and {}", randomMediators[0], randomMediators[0]);
        return randomMediators;
    }

    private boolean isMediatorAccepted(ContractState contract, long mediator) {
    	for (int i = 0; i < mediators.length; i++) {
    		if(mediators[i].getSignedLongId() == mediator && mediatorBalances[i]!=null && mediatorBalances[i].compareTo(MIN_TRT) >= 0){
				logger.trace("Mediator {} accepted for contract {}", mediator, contract.getAddress().toString());
				return true;
			}
		}
		logger.debug("Mediator {} not accepted for contract {}", mediator, contract.getAddress().toString());
        return false;
    }

    public void setMediatorBalance(int i, SignumValue value) {
    	mediatorBalances[i] = value;
    }

    public boolean isMediator(long id) {
    	for (SignumID m : mediators) {
            if(m.getSignedLongId() == id){
				logger.trace("isMediator {} true", id);
				return true;
			}
        }
		logger.trace("isMediator {} false", id);
        return false;
    }

    public boolean areMediatorsAccepted(ContractState contract) {
        return isMediatorAccepted(contract, contract.getMediator1())
        		&& isMediatorAccepted(contract, contract.getMediator2());
    }
}
