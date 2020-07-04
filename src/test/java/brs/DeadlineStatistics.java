package brs;

import java.util.ArrayList;

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
//		BT.setNodeAddress(BT.NODE_LOCAL_TESTNET);
		
		// How many blocks in the past should the analysis start:
		int start = 0;
		
		int PAGE_SIZE = 90;
		int NBLOCKS = 360;
		
		int smallestDeadline = Integer.MAX_VALUE;
		int largestDeadline = 0;
		
		float avDeadline = 0;
		float avEC = 0;
		int counter = 0;
		
		ArrayList<Block> blockList = new ArrayList<>(NBLOCKS);
		
		while(counter < NBLOCKS) {
			Block[] blocks = BT.getNode().getBlocks(start, start+PAGE_SIZE).blockingGet();
			start += PAGE_SIZE;
			
			for(int i = 1; i<blocks.length; i++) {
				blockList.add(blocks[i]);
				counter ++;
			}
		}
		
		System.out.println("height\tdeadline\ttarget\tEC");
		for(int i = 1; i<blockList.size(); i++) {
			Block next = blockList.get(i);
			Block b = blockList.get(i-1);

			int deadline = b.getTimestamp().getTimestamp() - next.getTimestamp().getTimestamp();

			smallestDeadline = Math.min(smallestDeadline, deadline);
			largestDeadline = Math.max(largestDeadline, deadline);

			avDeadline += deadline;
			float EC = 18325193796f/b.getBaseTarget();
			avEC += EC;
			System.out.println(b.getHeight() + "\t" + deadline + "\t" + b.getBaseTarget() + "\t" + EC);
		}
        avDeadline /= counter;
        avEC /= counter;
		
		double stdDev = 0;
		for(int i = 1; i<blockList.size(); i++) {
			Block next = blockList.get(i);
			Block b = blockList.get(i-1);

			int deadline = b.getTimestamp().getTimestamp() - next.getTimestamp().getTimestamp();
			
			double dev = deadline-avDeadline;
			stdDev += dev*dev;
		}
		stdDev = Math.sqrt(stdDev/counter);
		
        System.out.println("average deadline: " + avDeadline);
        System.out.println("average EC: " + avEC);
		System.out.println("smallest deadline: " + smallestDeadline);
		System.out.println("largest deadline: " + largestDeadline);
		System.out.println("deadline std dev: " + stdDev);

		System.exit(0);
	}
}
