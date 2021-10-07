
package btdex.miner.event;


import java.math.BigInteger;

import btdex.miner.PlotFile;

public class ReaderLoadedPartEvent {

	private byte[] generationSignature;
	private BigInteger chunkPartStartNonce;
	private long blockNumber;

	private byte[] scoops;
	private int[] scoopArray;
	private PlotFile plotFile;

	public ReaderLoadedPartEvent(long blockNumber, byte[] generationSignature, byte[] scoops, int[] scoopArray, BigInteger chunkPartStartNonce, PlotFile plotFile) {
		this.generationSignature = generationSignature;
		this.chunkPartStartNonce = chunkPartStartNonce;
		this.blockNumber = blockNumber;
		this.scoops = scoops;
		this.scoopArray = scoopArray;
		this.plotFile = plotFile;
	}

	public PlotFile getPlotFile()
	{
		return plotFile;
	}

	public long getBlockNumber()
	{
		return blockNumber;
	}

	public byte[] getScoops()
	{
		return scoops;
	}

	public int[] getScoopArray()
	{
		return scoopArray;
	}

	public BigInteger getChunkPartStartNonce()
	{
		return chunkPartStartNonce;
	}

	public byte[] getGenerationSignature()
	{
		return generationSignature;
	}
}
