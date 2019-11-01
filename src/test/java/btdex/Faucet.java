package btdex;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import bt.BT;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import burst.kit.entity.response.http.BRSError;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

/**
 * An extremely simple Faucet for validating accounts.
 * 
 * @author jjos
 *
 */
public class Faucet extends Thread {

	public static final int PORT = 1978;
	
	public static final int TIMEOUT = 1000;
	
	static final BurstValue AMOUNT = BurstValue.fromPlanck(1);
	static final BurstNodeService NS = BurstNodeService.getCompositeInstance(BT.NODE_TESTNET);
	static final BurstCrypto BC = BurstCrypto.getInstance();
	static final String PASS = BT.PASSPHRASE;
	
	static final int MAX_REQUESTS_PER_BLOCK = 2;
	
	static BurstValue FEE = BurstValue.fromPlanck(735000);

	private Socket socket;
	private boolean exceeded;

	public Faucet(Socket s, boolean ex) {
		this.socket = s;
		this.exceeded = ex;
	}

	@Override
	public void run() {
		InputStream inp = null;
		BufferedReader brinp = null;
		DataOutputStream out = null;
		try {
			socket.setSoTimeout(TIMEOUT);
			inp = socket.getInputStream();
			brinp = new BufferedReader(new InputStreamReader(inp));
			out = new DataOutputStream(socket.getOutputStream());
			
			if(exceeded) {
				out.writeBytes("maximum number of requests reached, wait a few minutes and try again");
				out.flush();
				return;
			}
			
		} catch (IOException e) {
			return;
		}
		String pubkey;
		
		System.out.println("New client");

		try {
			pubkey = brinp.readLine();
			if (pubkey != null && pubkey.length() == 64) {
				byte []publicKey = BC.parseHexString(pubkey);
				BurstAddress address = BC.getBurstAddressFromPublic(publicKey);
				
				try {
					NS.getAccount(address).blockingGet();
					
					out.writeBytes("invalid request");
					out.flush();
				}
				catch (Exception e) {
					if(e.getCause() instanceof BRSError) {
						BRSError error = (BRSError) e.getCause();
						if(error.getCode() == 5) {							
							byte[] senderPublicKey = BC.getPublicKey(PASS);
							Single<byte[]> utx = NS.generateTransactionWithMessage(address, publicKey,
									senderPublicKey, AMOUNT, FEE,
									1000, "Congratulations, your new BTDEX account is now validated!");
							
							Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
								byte[] signedTransactionBytes = BC.signTransaction(BC.getPrivateKey(PASS), unsignedTransactionBytes);
								return NS.broadcastTransaction(signedTransactionBytes);
							});
							tx.blockingGet();
							out.writeBytes("success");
							out.flush();
							
							System.out.println("Pubkey served: " + pubkey);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String args[]) {
		ServerSocket serverSocket = null;
		
		long timer = System.currentTimeMillis();
		int nreq = 0;

		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Server running");
		while (true) {
			try {
				long now = System.currentTimeMillis();
				if(now - timer > TimeUnit.MINUTES.toMillis(20)) {
					// 5 blocks, clear the counters
					timer = now;
					nreq = 0;
					
					// update fee
					// We only serve unknown accounts
					FeeSuggestion feeSugest = NS.suggestFee().blockingGet();
					FEE = feeSugest.getCheapFee();
				}
				// another request
				nreq ++;
				
				Socket s = serverSocket.accept();
				// new thread for every client
				new Faucet(s, nreq > MAX_REQUESTS_PER_BLOCK*5).start();
			} catch (IOException e) {
				System.out.println("I/O error: " + e.getMessage());
			}
		}
	}
}

