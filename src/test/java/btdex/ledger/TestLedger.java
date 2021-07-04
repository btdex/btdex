package btdex.ledger;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import bt.BT;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.Account;
import signumj.service.NodeService;

/**
 * Check the Ledger implementation
 * 
 * Requires a connected Ledger device, with the Burstcoin app, available at
 * https://github.com/jjos2372/app-ledger-burst
 * 
 * Also that your Ledger account index 0 has some balance on TESTNET.
 * 
 * @author jjos
 *
 */
public class TestLedger {
	
	@Test
	public void testPubKeys() throws Exception {
		byte index1 = 0;
		byte index2 = 1;
		
		byte[] pubKey1 = BurstLedger.getPublicKey(index1);
		byte[] pubKey2 = BurstLedger.getPublicKey(index2);
		
		assertTrue(!Arrays.equals(pubKey1, pubKey2), "keys should be different");
	}

	@Test
	public void testOrdinary() throws Exception {
		
		byte index = 1;
		
		NodeService NS = NodeService.getInstance("https://testnet-2.burst-alliance.org:6876/");
		SignumCrypto BC = SignumCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		SignumAddress yourAddress = BC.getAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		SignumAddress rec = SignumAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransaction(rec, pubKey, SignumValue.fromSigna(0.1), SignumValue.fromSigna(0.01), 1000, null).blockingGet();
		byte [] signed = BurstLedger.sign(utx, index);
				
		byte[] messageSha256 = BC.getSha256().digest(utx);
		System.out.println("msgSha256: " + Hex.toHexString(messageSha256));		
		
		System.out.println("sig: " + Hex.toHexString(signed));
		
		byte [] sig = new byte[64];
		System.arraycopy(signed, 96, sig, 0, sig.length);
		
        boolean verified = BC.verify(sig, utx, pubKey, true);
        System.out.println("Verified: " + verified);
        assertTrue(verified);
	}
	
	@Test
	public void testMessage() throws Exception {
		byte index = 2;
		
		NodeService NS = NodeService.getInstance(BT.NODE_TESTNET);
		SignumCrypto BC = SignumCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		SignumAddress yourAddress = BC.getAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		SignumAddress rec = SignumAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransactionWithMessage(rec, pubKey, SignumValue.fromSigna(0.1), 1000, 
				"test message", null).blockingGet();
		byte [] signed = BurstLedger.sign(utx, index);
				
		byte[] messageSha256 = BC.getSha256().digest(utx);
		System.out.println("msgSha256: " + Hex.toHexString(messageSha256));		
		
		System.out.println("sig: " + Hex.toHexString(signed));
		
		byte [] sig = new byte[64];
		System.arraycopy(signed, 96, sig, 0, sig.length);
		
        boolean verified = BC.verify(sig, utx, pubKey, true);
        System.out.println("Verified: " + verified);
        assertTrue(verified);
	}

	@Test
	public void testLongMessage() throws Exception {
		byte index = 1;
		
		NodeService NS = NodeService.getInstance(BT.NODE_TESTNET);
		SignumCrypto BC = SignumCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		SignumAddress yourAddress = BC.getAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		SignumAddress rec = SignumAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransactionWithMessage(rec, pubKey, SignumValue.fromSigna(0.1), 1000, 
				"test message with a lot of byyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyytttttttttttttttttttttttttttttttttttttteeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeessssssssssssssssssssssssssssssssssssssss", null)
				.blockingGet();
		byte [] signed = BurstLedger.sign(utx, index);
				
		byte[] messageSha256 = BC.getSha256().digest(utx);
		System.out.println("msgSha256: " + Hex.toHexString(messageSha256));		
		
		System.out.println("sig: " + Hex.toHexString(signed));
		
		byte [] sig = new byte[64];
		System.arraycopy(signed, 96, sig, 0, sig.length);
		
        boolean verified = BC.verify(sig, utx, pubKey, true);
        System.out.println("Verified: " + verified);
        assertTrue(verified);
	}
	
	
	@Test
	public void testMultiOut() throws Exception {
		byte index = 2;
		
		NodeService NS = NodeService.getInstance(BT.NODE_TESTNET);
		SignumCrypto BC = SignumCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		SignumAddress yourAddress = BC.getAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		HashMap<SignumAddress, SignumValue> recipients = new HashMap<SignumAddress, SignumValue>();
		recipients.put(BC.getAddressFromPassphrase(BT.PASSPHRASE), SignumValue.fromSigna(0.01));
		recipients.put(BC.getAddressFromPassphrase(BT.PASSPHRASE2), SignumValue.fromSigna(0.01));
		recipients.put(BC.getAddressFromPassphrase(BT.PASSPHRASE3), SignumValue.fromSigna(0.01));
		recipients.put(BC.getAddressFromPassphrase(BT.PASSPHRASE4), SignumValue.fromSigna(0.01));
		
		byte []utx = NS.generateMultiOutTransaction(pubKey, SignumValue.fromSigna(0.01), 1000, recipients).blockingGet();
		byte [] signed = BurstLedger.sign(utx, index);
				
		byte[] messageSha256 = BC.getSha256().digest(utx);
		System.out.println("msgSha256: " + Hex.toHexString(messageSha256));		
		
		System.out.println("sig: " + Hex.toHexString(signed));
		
		byte [] sig = new byte[64];
		System.arraycopy(signed, 96, sig, 0, sig.length);
		
        boolean verified = BC.verify(sig, utx, pubKey, true);
        System.out.println("Verified: " + verified);
        assertTrue(verified);
	}

	@Test
	public void testMultiOutSame() throws Exception {
		byte index = 1;
		
		NodeService NS = NodeService.getInstance(BT.NODE_TESTNET);
		SignumCrypto BC = SignumCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		SignumAddress yourAddress = BC.getAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		HashSet<SignumAddress> recipients = new HashSet<>();
		recipients.add(BC.getAddressFromPassphrase(BT.PASSPHRASE));
		recipients.add(BC.getAddressFromPassphrase(BT.PASSPHRASE2));
		recipients.add(BC.getAddressFromPassphrase(BT.PASSPHRASE3));
		recipients.add(BC.getAddressFromPassphrase(BT.PASSPHRASE4));
		
		byte []utx = NS.generateMultiOutSameTransaction(pubKey, SignumValue.fromSigna(0.1), SignumValue.fromSigna(0.01), 1000, recipients).blockingGet();
		byte [] signed = BurstLedger.sign(utx, index);
				
		byte[] messageSha256 = BC.getSha256().digest(utx);
		System.out.println("msgSha256: " + Hex.toHexString(messageSha256));		
		
		System.out.println("sig: " + Hex.toHexString(signed));
		
		byte [] sig = new byte[64];
		System.arraycopy(signed, 96, sig, 0, sig.length);
		
        boolean verified = BC.verify(sig, utx, pubKey, true);
        System.out.println("Verified: " + verified);
        assertTrue(verified);
	}

}
