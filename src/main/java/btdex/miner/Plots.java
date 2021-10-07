
package btdex.miner;


import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import btdex.core.Constants;
import btdex.core.Globals;

public class Plots {
	private static final Logger LOG = LoggerFactory.getLogger(Plots.class);

	private Collection<PlotDrive> plotDrives;
	private Map<BigInteger, Long> chunkPartStartNonces;

	public Plots() {
		plotDrives = new HashSet<>();
		chunkPartStartNonces = new HashMap<>();
		Globals g = Globals.getInstance();

		for(int i=0; i<1000; i++) {
			String path = g.getProperty(Constants.PROP_PLOT_PATH + (i+1));
			if(path == null || path.length() == 0) {
				break;
			}

			PlotDrive plotDrive = new PlotDrive(path, 960000L);
			if(!plotDrive.getPlotFiles().isEmpty()) {
				plotDrives.add(plotDrive);

				int expectedSize = chunkPartStartNonces.size() + plotDrive.collectChunkPartStartNonces().size();
				chunkPartStartNonces.putAll(plotDrive.collectChunkPartStartNonces());
				if(expectedSize != chunkPartStartNonces.size()) {
					LOG.error("possible duplicate/overlapping plot-file on drive '" + plotDrive.getDirectory() + "' please check your plots.");
				}
			}
			else {
				LOG.info("no plotfiles found at '" + plotDrive.getDirectory() + "' ... will be ignored.");
			}
		}
	}

	public Collection<PlotDrive> getPlotDrives() {
		return plotDrives;
	}

	/* total number of bytes of all plotFiles */
	public long getSize() {
		long size = 0;
		for(PlotDrive plotDrive : plotDrives) {
			size += plotDrive.getSize();
		}
		return size;
	}

	public void printPlotFiles() {
		for(PlotDrive plotDrive : getPlotDrives()) {
			for(PlotFile plotFile : plotDrive.getPlotFiles()) {
				System.out.println(plotFile.getFilePath());
			}
		}
	}

	public void checkPlotFiles() {
		for(PlotDrive plotDrive : getPlotDrives()) {
			for(PlotFile plotFile : plotDrive.getPlotFiles()) {
				plotFile.check();
			}
		}
	}

	/* gets plot file by plot file start nonce. */
	public PlotFile getPlotFileByPlotFileStartNonce(long plotFileStartNonce) {
		for(PlotDrive plotDrive : getPlotDrives()) {
			for(PlotFile plotFile : plotDrive.getPlotFiles()) {
				if(plotFile.getFilename().contains(String.valueOf(plotFileStartNonce))) {
					return plotFile;
				}
			}
		}
		return null;
	}

	/* gets chunk part start nonces. */
	public Map<BigInteger, Long> getChunkPartStartNonces() {
		return chunkPartStartNonces;
	}

	/* gets plot file by chunk part start nonce. */
	public PlotFile getPlotFileByChunkPartStartNonce(BigInteger chunkPartStartNonce) {
		for(PlotDrive plotDrive : getPlotDrives()) {
			for(PlotFile plotFile : plotDrive.getPlotFiles()) {
				if(plotFile.getChunkPartStartNonces().containsKey(chunkPartStartNonce)) {
					return plotFile;
				}
			}
		}
		return null;
	}
}
