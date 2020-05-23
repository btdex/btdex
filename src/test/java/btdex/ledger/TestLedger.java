package btdex.ledger;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import bt.BT;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.Account;
import burst.kit.service.BurstNodeService;

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
		
		byte index = 0;
		
		BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_TESTNET);
		BurstCrypto BC = BurstCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		BurstAddress yourAddress = BC.getBurstAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > BurstValue.fromBurst(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		BurstAddress rec = BurstAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransaction(rec, pubKey, BurstValue.fromBurst(0.1), BurstValue.fromBurst(0.01), 1000).blockingGet();
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
		byte index = 0;
		
		BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_TESTNET);
		BurstCrypto BC = BurstCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		BurstAddress yourAddress = BC.getBurstAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > BurstValue.fromBurst(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		BurstAddress rec = BurstAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransactionWithMessage(rec, pubKey, BurstValue.fromBurst(0.1), 1000, 
				"test message").blockingGet();
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
		byte index = 0;
		
		BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_TESTNET);
		BurstCrypto BC = BurstCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		BurstAddress yourAddress = BC.getBurstAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > BurstValue.fromBurst(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		BurstAddress rec = BurstAddress.fromRs("BURST-JJQS-MMA4-GHB4-4ZNZU");
		
		byte []utx = NS.generateTransactionWithMessage(rec, pubKey, BurstValue.fromBurst(0.1), 1000, 
				"test message with a lot of byyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyytttttttttttttttttttttttttttttttttttttteeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeessssssssssssssssssssssssssssssssssssssss")
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
		byte index = 0;
		
		BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_TESTNET);
		BurstCrypto BC = BurstCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		BurstAddress yourAddress = BC.getBurstAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > BurstValue.fromBurst(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		HashMap<BurstAddress, BurstValue> recipients = new HashMap<BurstAddress, BurstValue>();
		recipients.put(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE), BurstValue.fromBurst(0.01));
		recipients.put(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE2), BurstValue.fromBurst(0.01));
		recipients.put(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE3), BurstValue.fromBurst(0.01));
		recipients.put(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE4), BurstValue.fromBurst(0.01));
		
		byte []utx = NS.generateMultiOutTransaction(pubKey, BurstValue.fromBurst(0.01), 1000, recipients).blockingGet();
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
		byte index = 0;
		
		BurstNodeService NS = BurstNodeService.getInstance(BT.NODE_TESTNET);
		BurstCrypto BC = BurstCrypto.getInstance();
		
		byte[] pubKey = BurstLedger.getPublicKey(index);
		
		BurstAddress yourAddress = BC.getBurstAddressFromPublic(pubKey);
		Account account = NS.getAccount(yourAddress).blockingGet();
		
		assertTrue(account.getBalance().longValue() > BurstValue.fromBurst(1).longValue(),
				"Account " + yourAddress.getFullAddress() + " has no balace for testing");

		HashSet<BurstAddress> recipients = new HashSet<>();
		recipients.add(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE));
		recipients.add(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE2));
		recipients.add(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE3));
		recipients.add(BC.getBurstAddressFromPassphrase(BT.PASSPHRASE4));
		
		byte []utx = NS.generateMultiOutSameTransaction(pubKey, BurstValue.fromBurst(0.1), BurstValue.fromBurst(0.01), 1000, recipients).blockingGet();
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
