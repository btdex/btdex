
package btdex.miner;


import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import btdex.ui.MiningPanel;
import signumj.crypto.plot.impl.MiningPlot;

/**
 * The type Plot drive.
 */
public class PlotDrive {
	private static Logger logger = LogManager.getLogger();

	private Collection<PlotFile> plotFiles;
	private String directory;

	PlotDrive(String directory, Long chunkPartNonces) {
		this.directory = directory;

		plotFiles = new HashSet<>();

		// check if there are pending files to resume
		File[] plotFilePaths = new File(directory).listFiles(MiningPanel.PLOT_FILE_FILTER);
		for(File path : plotFilePaths) {
			PlotFile plotFile = new PlotFile(path.toPath(), chunkPartNonces);

			long expectedFileSize = MiningPlot.SCOOP_SIZE * MiningPlot.SCOOPS_PER_PLOT * plotFile.getPlots();

			if(expectedFileSize != plotFile.getFilePath().toFile().length()) {
				logger.error("invalid file size for plot : " + plotFile.getFilePath());        
			}
			else if(plotFile.getStaggeramt() % plotFile.getNumberOfParts() != 0) {
				logger.error("could not calculate valid numberOfParts: " + plotFile.getFilePath());
			}
			else {
				plotFiles.add(plotFile);
			}
		}
	}

	public Collection<PlotFile> getPlotFiles() {
		return plotFiles;
	}

	public String getDirectory() {
		return directory;
	}

	/* Collects chunk part start nonces.*/
	Map<BigInteger, Long> collectChunkPartStartNonces() {
		Map<BigInteger, Long> chunkPartStartNonces = new HashMap<>();
		for(PlotFile plotFile : plotFiles) {
			int expectedSize = chunkPartStartNonces.size() + plotFile.getChunkPartStartNonces().size();
			chunkPartStartNonces.putAll(plotFile.getChunkPartStartNonces());
			if(expectedSize != chunkPartStartNonces.size()) {
				logger.warn("possible overlapping plot-file '" + plotFile.getFilePath() + "', please check your plots.");
			}
		}
		return chunkPartStartNonces;
	}

	/* returns total number of bytes of all plotFiles */
	public long getSize() {
		long size = 0;
		for(PlotFile plotFile : plotFiles) {
			size += plotFile.getSize();
		}
		return size;
	}
}
