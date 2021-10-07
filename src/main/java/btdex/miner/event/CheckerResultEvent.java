
package btdex.miner.event;

import java.math.BigInteger;

import btdex.miner.PlotFile;

/**
 * Fired when a chunk-part is checked.
 */
public class CheckerResultEvent {
	private byte[] generationSignature;
	private BigInteger chunkPartStartNonce;

	private long blockNumber;
	private BigInteger result;
	private PlotFile plotFile;
	private byte[] scoops;
	private int[] scoopArray;
	private int lowestNonce;

	public CheckerResultEvent(long blockNumber, byte[] generationSignature, BigInteger chunkPartStartNonce, int lowestNonce, int[] scoopArray,
			PlotFile plotFile, byte[] scoops) {
		this.generationSignature = generationSignature;
		this.chunkPartStartNonce = chunkPartStartNonce;
		this.blockNumber = blockNumber;
		this.lowestNonce = lowestNonce;
		this.plotFile = plotFile;
		this.scoops = scoops;
		this.scoopArray = scoopArray;
	}

	public PlotFile getPlotFile()
	{
		return plotFile;
	}

	public long getBlockNumber()
	{
		return blockNumber;
	}

	public BigInteger getResult()
	{
		return result;
	}

	public void setResult(BigInteger result)
	{
		this.result = result;
	}

	public byte[] getScoops()
	{
		return scoops;
	}

	public int[] getScoopArray()
	{
		return scoopArray;
	}

	public int getLowestNonce()
	{
		return lowestNonce;
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
