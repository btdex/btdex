
package btdex.miner.event;

public class ReaderDriveFinishEvent {
	private String directory;
	private long size;
	private long time;
	private long blockNumber;

	public ReaderDriveFinishEvent(String directory, long size, long time, long blockNumber) {
		this.directory = directory;
		this.size = size;
		this.time = time;
		this.blockNumber = blockNumber;
	}

	public long getTime()
	{
		return time;
	}

	public String getDirectory()
	{
		return directory;
	}

	public long getSize()
	{
		return size;
	}

	public long getBlockNumber()
	{
		return blockNumber;
	}
}
