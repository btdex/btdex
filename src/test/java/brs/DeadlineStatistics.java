package brs;

import bt.BT;
import burst.kit.entity.response.Block;

/**
 * Check the deadlines of a series of blocks and computes alternative deadlines.
 * 
 * @author jjos
 *
 */
public class DeadlineStatistics {

	public static void main(String[] args) {

		// Select the node: mainnet or testnet
		BT.setNodeAddress(BT.NODE_BURSTCOIN_RO);
//		BT.setNodeAddress(BT.NODE_TESTNET);
		
		// How many blocks in the past should the analysis start:
		int start = 10000;
		
		int SQRT_FACTOR = 16;
		int LN_FACTOR = 45;

		int PAGE_SIZE = 90;
		
		int smallestDeadline = Integer.MAX_VALUE;
		int largestDeadline = 0;
		
		long avDeadline = 0;
		int counter = 0;
		
		System.out.println("height\tdeadline\tdeadline sqrt\tdeadline ln\ttarget");
		
		while(counter < 360) {
			
			Block[] blocks = BT.getNode().getBlocks(start, start+PAGE_SIZE).blockingGet();
			start += PAGE_SIZE;

			for(int i = 1; i<blocks.length; i++) {
				Block next = blocks[i];
				Block b = blocks[i-1];

				int deadline = b.getTimestamp().getTimestamp() - next.getTimestamp().getTimestamp();
				
				int deadlineSqrt = (int)(Math.sqrt(deadline) * SQRT_FACTOR);
				int deadlineLn = (int)(Math.log(deadline) * LN_FACTOR);
				
				smallestDeadline = Math.min(smallestDeadline, deadline);
				largestDeadline = Math.max(largestDeadline, deadline);

				avDeadline += deadline;
				counter ++;

				System.out.println(b.getHeight() + "\t" + deadline + "\t" +
						deadlineSqrt + "\t" + deadlineLn + "\t" + b.getBaseTarget());
			}
		}
		avDeadline /= counter;
		System.out.println("average deadline: " + avDeadline);
		System.out.println("smallest deadline: " + smallestDeadline);
		System.out.println("largest deadline: " + largestDeadline);

		System.out.println("average deadline sqrt: " + (int)(Math.sqrt(avDeadline) * SQRT_FACTOR));
		System.out.println("smallest deadline sqrt: " + (int)(Math.sqrt(smallestDeadline) * SQRT_FACTOR));
		System.out.println("largest deadline sqrt: " + (int)(Math.sqrt(largestDeadline) * SQRT_FACTOR));

		System.out.println("average deadline ln: " + (int)(Math.log(avDeadline) * LN_FACTOR));
		System.out.println("smallest deadline ln: " + (int)(Math.log(smallestDeadline) * LN_FACTOR));
		System.out.println("largest deadline ln: " + (int)(Math.log(largestDeadline) * LN_FACTOR));

		System.exit(0);
	}
}
