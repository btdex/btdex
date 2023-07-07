package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bt.BT;
import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.NumberFormatting;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.Account;
import signumj.entity.response.Block;
import signumj.entity.response.MiningInfo;
import signumj.entity.response.Transaction;
import dorkbox.util.OS;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MiningPanel extends JPanel implements ActionListener, ChangeListener {
	
	private static final int N_PATHS_MIN = 3;
	private static final long ONE_GIB = 1073741824L;
	private static final long BYTES_OF_A_NONCE = 262144L;
	
	private static String TMP_DIR = System.getProperty("java.io.tmpdir");
	
	private static final String PROP_PLOT_PATH = "plotPath";
	private static final String PROP_PLOT_CACHE = "plotPathCache";
	private static final String PROP_PLOT_LOW_PRIO = "plotLowPrio";
	private static final String PROP_PLOT_CPU_CORES = "plotCpuCores";
	private static final String PROP_MINE_CPU_CORES = "mineCpuCores";
	private static final String PROP_MINE_ONLY_BEST = "mineOnlyBest";
	private static final String PROP_MINER_POOL = "minerPool";
	private static final String PROP_MINER_AUTO_START = "minerAutoStart";
        private static final String PROP_MINER_USE_DIRECT_IO = "minerUseDirectIO";
	
	private static final String PLOT_APP = "[PLOTTER]";
	private static final String MINER_APP = "[MINER]";
	
	private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private static final FileFilter PLOT_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && Globals.getInstance().getAddress() != null &&
					pathname.getName().startsWith(Globals.getInstance().getAddress().getID()) &&
					pathname.getName().split("_").length == 3;
		}
	};
	
	private static final FileFilter PLOT_FILE_FILTER_ANY = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && Globals.getInstance().getAddress() != null &&
					pathname.getName().split("_").length == 3;
		}
	};
	
	private static final String[] POOL_LIST = {
			"https://pool.signumcoin.ro",
			"https://signapool.notallmine.net",
			"http://pool.btfg.space",
			"http://signa.voiplanparty.com:8124",
			"http://signumpool.de:8080",
			"http://burst.e4pool.com",
			"http://opensignumpool.ddns.net:8126",
			"https://pool.ravefusions.com",
	};
	
	private static final String[] POOL_LIST_TESTNET = {
			"https://t-pool.notallmine.net",
			"http://localhost:8000"
	};
	
	private LinkedHashMap<String, SignumAddress> poolAddresses = new LinkedHashMap<>();
	private HashMap<String, String> poolMaxDeadlines = new HashMap<>();
	
	private ArrayList<JButton> pathCancelButtons = new ArrayList<>();
	private ArrayList<JButton> openFolderButtons = new ArrayList<>();
	private ArrayList<JButton> selectFolderButtons = new ArrayList<>();
	private ArrayList<JSlider> fractionToPlotSliders = new ArrayList<>();

	private ArrayList<Desc> sliderListDesc = new ArrayList<>();

	private ArrayList<File> pathList = new ArrayList<>();
	
	private JTextArea console;

	private JButton ssdSelectFolderButton;
	
	private File ssdPath;

	private JButton ssdCancelButton;

	private JButton startPlotButton, stopPlotButton;

	private JComboBox<String> cpusToPlotComboBox;

	private JComboBox<String> cpusToMineComboBox;

	private JComboBox<String> poolComboBox;

	private JCheckBox mineSubmitOnlyBest;

	private JCheckBox mineUseDirectIO;

	private JButton joinPoolButton, openPoolButton;

	private JTextField committedAmountField;

	private JButton addCommitmentButton;

	private JButton removeCommitmentButton;

	private JCheckBox minerAutoStartCheck;

	private JButton startMiningButton, stopMiningButton;
	
	private Process minerProcess, plotterProcess;

	private JCheckBox lowPriorityCheck;

	private JTextArea rewardsEstimationArea;

	private ArrayList<File> newPlotFiles = new ArrayList<>();
	private ArrayList<File> resumePlotFiles = new ArrayList<>();

	private boolean plotting, mining;

	private Logger logger;

	private File minerFile, plotterFile;

	private RotatingIcon pendingIconRotating;

	private Icon folderIcon;

	private long totalToPlot;
	private long totalCapacity;
	
	private AtomicReference<Long> noncesPlotted = new AtomicReference<>();

	private Icon iconPlot;

	private Icons icons;

	private JPanel disksPanel;

	private String soloNode;

	public MiningPanel() {
		super(new BorderLayout());
		
		logger = LogManager.getLogger();
		
		icons = new Icons(this.getForeground(), Constants.ICON_SIZE_MED);
		
		Icon pendingIcon = icons.get(Icons.SPINNER);
		pendingIconRotating = new RotatingIcon(pendingIcon);
		folderIcon = icons.get(Icons.FOLDER);
		
		disksPanel = new JPanel(new GridLayout(0, 1));
		JPanel plottingPanel = new JPanel(new BorderLayout());
		JPanel plottingBottomPanel = new JPanel(new GridLayout(0, 1));
		plottingPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_plot_disks")));
		plottingPanel.add(plottingBottomPanel, BorderLayout.PAGE_END);
		JScrollPane disksScrollPane = new JScrollPane(disksPanel);
		plottingPanel.setPreferredSize(new Dimension(200, 300));
		plottingPanel.add(disksScrollPane, BorderLayout.CENTER);
		
		Globals g = Globals.getInstance();
		
		int nPaths = N_PATHS_MIN;
		for (int i = 0; i < nPaths; i++) {			
			addDiskPath(i);
			
			if(pathList.get(i) != null && i == nPaths -1) {
				// add more as needed
				nPaths++;
			}
		}
		
		JPanel ssdPanel = new JPanel(new BorderLayout(2, 0));
		ssdSelectFolderButton = new JButton(tr("mine_ssd_cache_select"), icons.get(Icons.FOLDER));
		ssdSelectFolderButton.addActionListener(this);
		ssdPanel.add(ssdSelectFolderButton, BorderLayout.CENTER);
		
		ssdCancelButton = new JButton(icons.get(Icons.CANCEL));
		ssdCancelButton.setToolTipText(tr("mine_ssd_remove"));
		ssdCancelButton.addActionListener(this);
		ssdCancelButton.setEnabled(false);
		
		String path = g.getProperty(PROP_PLOT_CACHE);
		if(path != null && path.length() > 0) {
			ssdPath = new File(path);
			ssdSelectFolderButton.setText(tr("mine_ssd_cache", path));
			ssdCancelButton.setEnabled(true);
		}
		
		ssdPanel.add(ssdCancelButton, BorderLayout.LINE_START);
		plottingBottomPanel.add(ssdPanel);
		
		// CPU cores
		int coresAvailable = Runtime.getRuntime().availableProcessors();
		int selectedCores = Math.max(1, coresAvailable-2);
		if(g.getProperty(PROP_PLOT_CPU_CORES) != null && g.getProperty(PROP_PLOT_CPU_CORES).length() > 0) {
			selectedCores = Integer.parseInt(g.getProperty(PROP_PLOT_CPU_CORES));
		}
		selectedCores = Math.min(selectedCores, coresAvailable);
		selectedCores = Math.max(selectedCores, 1);
		cpusToPlotComboBox = new JComboBox<String>();
		for (int i = 1; i <= coresAvailable; i++) {
			cpusToPlotComboBox.addItem(Integer.toString(i));			
		}
		cpusToPlotComboBox.setSelectedIndex(selectedCores-1);
		cpusToPlotComboBox.addActionListener(this);
		
		/* TODO: add GPU support
		int numPlatforms[] = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms[0]];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        // Collect all devices of all platforms
        List<cl_device_id> devices = new ArrayList<cl_device_id>();
        for (int i=0; i<platforms.length; i++) {
            String platformName = getString(platforms[i], CL.CL_PLATFORM_NAME);

            // Obtain the number of devices for the current platform
            int numDevices[] = new int[1];
            CL.clGetDeviceIDs(platforms[i], CL.CL_DEVICE_TYPE_ALL, 0, null, numDevices);

            System.out.println("Number of devices in platform "+platformName+": "+numDevices[0]);
            
            // CL_PLATFORM_VERSION
            String platformVersion = getString(platforms[i], CL.CL_PLATFORM_VERSION);
            System.out.printf("CL_PLATFORM_VERSION: \t\t\t%s\n", platformVersion);

            cl_device_id devicesArray[] = new cl_device_id[numDevices[0]];
            CL.clGetDeviceIDs(platforms[i], CL.CL_DEVICE_TYPE_ALL, numDevices[0], devicesArray, null);

            devices.addAll(Arrays.asList(devicesArray));
        }
        */
				
		JPanel plotButtonsPanelLine1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel plotButtonsPanelLine2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		iconPlot = icons.get(Icons.PLOTTING);
		startPlotButton = new JButton(tr("mine_plot"), iconPlot);
		startPlotButton.addActionListener(this);
		startPlotButton.setEnabled(resumePlotFiles.size() > 0);
		
		stopPlotButton = new JButton(tr("mine_plot_stop"), icons.get(Icons.CANCEL));
		stopPlotButton.addActionListener(this);
		stopPlotButton.setEnabled(false);
		
		plotButtonsPanelLine2.add(new JLabel(tr("mine_cpus")));
		plotButtonsPanelLine2.add(cpusToPlotComboBox);
		plotButtonsPanelLine2.add(lowPriorityCheck = new JCheckBox(tr("mine_run_low_prio")));
		plotButtonsPanelLine1.add(stopPlotButton);
		plotButtonsPanelLine1.add(startPlotButton);
		
		plottingBottomPanel.add(plotButtonsPanelLine1);
		plottingBottomPanel.add(plotButtonsPanelLine2);
		
		lowPriorityCheck.setSelected(Boolean.parseBoolean(g.getProperty(PROP_PLOT_LOW_PRIO)));
		lowPriorityCheck.addActionListener(this);
		
		
		JPanel poolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		poolPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_join_pool")));
		poolPanel.add(new JLabel(tr("mine_select_pool")));
		
		poolComboBox = new JComboBox<String>();
		poolPanel.add(poolComboBox);
		poolComboBox.addActionListener(this);
		
		openPoolButton = new JButton(icons.get(Icons.EXPLORER));
		openPoolButton.addActionListener(this);
		poolPanel.add(openPoolButton);

		joinPoolButton = new JButton(tr("send_join_pool"), icons.get(Icons.PLUG));
		joinPoolButton.addActionListener(this);
		poolPanel.add(joinPoolButton);

		JPanel rewardsPanel = new JPanel();
		rewardsPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_rewards_and_commitment")));
		rewardsPanel.setLayout(new BoxLayout(rewardsPanel, BoxLayout.Y_AXIS));
		
		rewardsEstimationArea = new JTextArea(3, 20);
		rewardsEstimationArea.setFont(rewardsEstimationArea.getFont().deriveFont(rewardsEstimationArea.getFont().getSize2D()*1.2f));
		rewardsEstimationArea.setEditable(false);
		rewardsPanel.add(rewardsEstimationArea);

		JPanel commitmentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rewardsPanel.add(commitmentPanel);
		commitmentPanel.add(new JLabel(tr("mine_your_amount")));

		committedAmountField = new JTextField(9);
		committedAmountField.setEditable(false);
		commitmentPanel.add(committedAmountField);
		
		addCommitmentButton = new JButton(icons.get(Icons.PLUS));
		addCommitmentButton.setToolTipText(tr("send_add_commitment"));
		commitmentPanel.add(addCommitmentButton);
		addCommitmentButton.addActionListener(this);
		removeCommitmentButton = new JButton(icons.get(Icons.MINUS));
		removeCommitmentButton.setToolTipText(tr("send_remove_commitment"));
		commitmentPanel.add(removeCommitmentButton);
		removeCommitmentButton.addActionListener(this);
		

		JPanel minerPanelMain = new JPanel(new GridLayout(0, 1));
		minerPanelMain.setBorder(BorderFactory.createTitledBorder(tr("mine_run_miner")));

		JPanel minerPanel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel minerPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		minerPanelMain.add(minerPanel1);
		minerPanelMain.add(minerPanel2);
		
		mineSubmitOnlyBest = new JCheckBox(tr("mine_only_best"));
		mineSubmitOnlyBest.setToolTipText(tr("mine_only_best_details"));
		mineSubmitOnlyBest.setSelected(!"false".equals(g.getProperty(PROP_MINE_ONLY_BEST)));
		mineSubmitOnlyBest.addActionListener(this);
		
                mineUseDirectIO = new JCheckBox(tr("mine_use_direct_io"));
                mineUseDirectIO.setToolTipText(tr("mine_use_direct_io_details"));
		mineUseDirectIO.setSelected(!"false".equals(g.getProperty(PROP_MINER_USE_DIRECT_IO)));
                mineUseDirectIO.addActionListener(this);

                cpusToMineComboBox = new JComboBox<String>();
		for (int i = 1; i <= coresAvailable; i++) {
			cpusToMineComboBox.addItem(Integer.toString(i));			
		}
		cpusToMineComboBox.setSelectedIndex(0);
		int coresToMine = 0;
		if(g.getProperty(PROP_MINE_CPU_CORES) != null && g.getProperty(PROP_MINE_CPU_CORES).length() > 0) {
			coresToMine = Integer.parseInt(g.getProperty(PROP_MINE_CPU_CORES))- 1;
		}
		if(coresToMine > 0 && coresToMine < cpusToMineComboBox.getItemCount())
			cpusToMineComboBox.setSelectedIndex(coresToMine);
		
		cpusToMineComboBox.addActionListener(this);
		
		minerPanel2.add(new JLabel(tr("mine_cpus")));
		minerPanel2.add(cpusToMineComboBox);
		minerPanel2.add(mineSubmitOnlyBest);
                minerPanel2.add(mineUseDirectIO);
		
		minerPanel1.add(minerAutoStartCheck = new JCheckBox(tr("mine_start_auto")));
		minerAutoStartCheck.setSelected(Boolean.parseBoolean(g.getProperty(PROP_MINER_AUTO_START)));
		minerAutoStartCheck.addActionListener(this);

		stopMiningButton = new JButton(tr("mine_stop_mining"), icons.get(Icons.CANCEL));
		stopMiningButton.addActionListener(this);
		stopMiningButton.setEnabled(false);
		minerPanel1.add(stopMiningButton);
		startMiningButton = new JButton(tr("mine_start_mining"), icons.get(Icons.MINING));
		startMiningButton.addActionListener(this);
		minerPanel1.add(startMiningButton);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(poolPanel);
		rightPanel.add(minerPanelMain);
		rightPanel.add(rewardsPanel);
		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(plottingPanel, BorderLayout.CENTER);
		topPanel.add(rightPanel, BorderLayout.LINE_END);
		
		console = new JTextArea(10, 40);
		console.setEditable(false);
        JScrollPane consoleScrollPane = new JScrollPane(console);        
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        consolePanel.setBorder(BorderFactory.createTitledBorder(tr("mine_console")));
		
		add(topPanel, BorderLayout.PAGE_START);
		add(consolePanel, BorderLayout.CENTER);
		
		String []poolList = Globals.getInstance().isTestnet() ? POOL_LIST_TESTNET : POOL_LIST;
		String selectedPool = Globals.getInstance().getProperty(PROP_MINER_POOL);
		boolean userHasPool = false;
		for(String poolURL : poolList) {
			poolComboBox.addItem(poolURL);
			if(poolURL.equals(selectedPool)) {
				poolComboBox.setSelectedIndex(poolComboBox.getItemCount()-1);
				userHasPool = true;
			}
		}
		if(!userHasPool) {
			Random r = new Random();
			poolComboBox.setSelectedIndex(r.nextInt(poolComboBox.getItemCount()));
		}
		soloNode = g.isTestnet() ? BT.NODE_LOCAL_TESTNET : BT.NODE_LOCAL;
		poolComboBox.addItem(soloNode);
		if(soloNode.equals(selectedPool))
			poolComboBox.setSelectedIndex(poolComboBox.getItemCount()-1);
		// Solo mining should have its own address
		poolAddresses.put(soloNode, g.getAddress());
		poolMaxDeadlines.put(soloNode, "100000000");
		actionPerformed(new ActionEvent(poolComboBox, 0, ""));
		
		checkForPlotProblems();
		stateChanged(null);
		
		// Query pool API for their info
		Thread thread = new Thread(){
			public void run(){
				updatePoolInfo(poolList);
			}
		};
		thread.start();
		
		if(minerAutoStartCheck.isSelected()) {
			new java.util.Timer().schedule( 
			        new java.util.TimerTask() {
			            public void run() {
			                startMining();
			            }
			        }, 5000);
		}
	}
	
	private void addDiskPath(int i) {
		Globals g = Globals.getInstance();
		
		JButton cancelButton = new JButton(icons.get(Icons.CANCEL));
		cancelButton.setToolTipText(tr("mine_remove_path"));
		cancelButton.addActionListener(this);
		cancelButton.setEnabled(false);
		
		JButton openFolderButton = new JButton(icons.get(Icons.EXPLORER));
		openFolderButton.addActionListener(this);
		openFolderButton.setEnabled(false);
		
		JButton selectFolderButton = new JButton(tr("mine_select_disk"), folderIcon);
		selectFolderButton.addActionListener(this);
		
		JSlider fractionToPlotSlider = new JSlider(0, 100, 0);
		fractionToPlotSlider.addChangeListener(this);
		fractionToPlotSlider.setEnabled(false);
		
		Desc sliderDesc = new Desc(" ", fractionToPlotSlider);
		
		pathCancelButtons.add(cancelButton);
		openFolderButtons.add(openFolderButton);
		selectFolderButtons.add(selectFolderButton);
		fractionToPlotSliders.add(fractionToPlotSlider);
		sliderListDesc.add(sliderDesc);
		
		String path = g.getProperty(PROP_PLOT_PATH + (i+1));
		if(path == null || path.length() == 0) {
			pathList.add(null);
		}
		else {
			selectFolderButton.setText(path);
			File pathFile = new File(path);
			pathList.add(pathFile);
			cancelButton.setEnabled(true);
			openFolderButton.setEnabled(true);
			fractionToPlotSlider.setEnabled(true);
		}
		
		JPanel folderBorderPanel = new JPanel(new BorderLayout(2, 0));
		folderBorderPanel.add(selectFolderButton, BorderLayout.CENTER);
		folderBorderPanel.add(cancelButton, BorderLayout.LINE_START);
		JPanel folderPanel = new JPanel(new BorderLayout());
		folderPanel.add(openFolderButton, BorderLayout.LINE_START);
		folderPanel.add(sliderDesc, BorderLayout.CENTER);
		folderBorderPanel.add(folderPanel, BorderLayout.LINE_END);
		
		disksPanel.add(folderBorderPanel);
	}
	
	private static final OkHttpClient CLIENT = new OkHttpClient();
	
	private void updatePoolInfo(String []urlList) {
		
		for (int i = 0; i < urlList.length; i++) {
			String poolURL = urlList[i];
			String poolURLgetConfig = poolURL + "/api/getConfig";
			try {
				Request request = new Request.Builder().url(poolURLgetConfig).build();
				Response responses = CLIENT.newCall(request).execute();
				String jsonData = responses.body().string();
				JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

				SignumAddress poolAddress = SignumAddress.fromEither(json.get("poolAccount").getAsString());
				poolAddresses.put(poolURL, poolAddress);
				poolMaxDeadlines.put(poolURL, "100000000");
				
				JsonElement jsonMaxDeadline = json.get("maxDeadline");
				if(jsonMaxDeadline != null) {
					poolMaxDeadlines.put(poolURL, jsonMaxDeadline.getAsString());
				}
			}
			catch (Exception e) {
				logger.debug("Pool incompatible or down: " + poolURL);
			}
		}
	}
	
//    /**
//     * Returns the value of the device info parameter with the given name
//     *
//     * @param device The device
//     * @param paramName The parameter name
//     * @return The value
//     */
//    private static String getString(cl_device_id device, int paramName)
//    {
//        // Obtain the length of the string that will be queried
//        long size[] = new long[1];
//        CL.clGetDeviceInfo(device, paramName, 0, null, size);
//
//        // Create a buffer of the appropriate size and fill it with the info
//        byte buffer[] = new byte[(int)size[0]];
//        CL.clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);
//
//        // Create a string from the buffer (excluding the trailing \0 byte)
//        return new String(buffer, 0, buffer.length-1);
//    }
//    
//    /**
//     * Returns the value of the platform info parameter with the given name
//     *
//     * @param platform The platform
//     * @param paramName The parameter name
//     * @return The value
//     */
//    private static String getString(cl_platform_id platform, int paramName)
//    {
//        // Obtain the length of the string that will be queried
//        long size[] = new long[1];
//        CL.clGetPlatformInfo(platform, paramName, 0, null, size);
//
//        // Create a buffer of the appropriate size and fill it with the info
//        byte buffer[] = new byte[(int)size[0]];
//        CL.clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);
//
//        // Create a string from the buffer (excluding the trailing \0 byte)
//        return new String(buffer, 0, buffer.length-1);
//    }
	
	public void update() {
		BurstNode BN = BurstNode.getInstance();

		Account account = BN.getAccount();
		if(account != null && account.getCommittedBalance() != null) {
			committedAmountField.setText(NumberFormatting.BURST.format(account.getCommittedBalance().longValue()));
		}
		
		if(plotting) {
			for (int i = 0; i < pathList.size(); i++) {
				File path = pathList.get(i);
				pathCancelButtons.get(i).setEnabled(false);
				openFolderButtons.get(i).setEnabled(false);
				selectFolderButtons.get(i).setEnabled(false);
				fractionToPlotSliders.get(i).setEnabled(false);
				
				if(path == null)
					continue;
				
				if(fractionToPlotSliders.get(i).getValue() > 0) {
					selectFolderButtons.get(i).setIcon(pendingIconRotating);
					pendingIconRotating.addComponent(selectFolderButtons.get(i));
				}
			}
			
			long percentPlotted = (100*BYTES_OF_A_NONCE*noncesPlotted.get())/totalToPlot;
			startPlotButton.setText(tr("mine_plotting", percentPlotted));
			startPlotButton.setIcon(pendingIconRotating);
			pendingIconRotating.addComponent(startPlotButton);
			if(ssdPath != null) {
				ssdSelectFolderButton.setIcon(pendingIconRotating);
				pendingIconRotating.addComponent(ssdSelectFolderButton);
			}
			
			ssdCancelButton.setEnabled(false);
			ssdSelectFolderButton.setEnabled(false);
		}
		else {
			totalCapacity = 0;
			for (int i = 0; i < pathList.size(); i++) {
				File path = pathList.get(i);
				selectFolderButtons.get(i).setEnabled(true);
				
				if(path == null)
					continue;
				
				File[] plotFiles = path.listFiles(PLOT_FILE_FILTER);
				long amountInPlots = 0;
				if(plotFiles != null) {
					for(File plot : plotFiles) {
						amountInPlots += plot.length();
					}
				}
				if(amountInPlots > 0) {
					selectFolderButtons.get(i).setText(path.getAbsolutePath() + " " + formatSpace(amountInPlots));
				}
				totalCapacity += amountInPlots;
				
				pendingIconRotating.removeComponent(selectFolderButtons.get(i));
				selectFolderButtons.get(i).setIcon(folderIcon);
				pathCancelButtons.get(i).setEnabled(true);
				openFolderButtons.get(i).setEnabled(true);
				fractionToPlotSliders.get(i).setEnabled(true);
				
				if(ssdPath != null) {
					pendingIconRotating.removeComponent(ssdSelectFolderButton);
					ssdSelectFolderButton.setIcon(folderIcon);
				}
			}

			stopPlotButton.setEnabled(false);
			startPlotButton.setIcon(iconPlot);
			pendingIconRotating.removeComponent(startPlotButton);
			startPlotButton.setText(tr("mine_plot"));
			
			ssdCancelButton.setEnabled(ssdPath != null);
			ssdSelectFolderButton.setEnabled(true);
		}
		
		MiningInfo miningInfo = BN.getMiningInfo();
		Block latestBlock = BN.getLatestBlock();
		if(miningInfo != null && latestBlock != null) {
			double networkTbs = 18325193796.0/(miningInfo.getBaseTarget()*1.83);
			SignumValue burstPerTbPerDay = SignumValue.fromSigna(360.0/networkTbs * latestBlock.getBlockReward().doubleValue());

			String rewards = tr("mine_reward_estimation_old", NumberFormatting.BURST_2.format(burstPerTbPerDay.longValue()),
					formatSpace(networkTbs*1024L*ONE_GIB));
			rewards += "\n" + tr("mine_reward_poc_plus_activation", NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(8).longValue()));
			SignumValue avgCommitment = null;
			int pocPlusBlock = Globals.getInstance().isTestnet() ? 269_700 : 878_000;
			if(miningInfo.getAverageCommitmentNQT() > 0 && miningInfo.getHeight() > pocPlusBlock) {
				avgCommitment = SignumValue.fromNQT(miningInfo.getAverageCommitmentNQT());
//				rewards = tr("mine_reward_estimation", NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(8).longValue()),
//						NumberFormatting.BURST_2.format(avgCommitment.multiply(100*8).longValue()));
				rewards = tr("mine_reward_estimation", NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(4.18).longValue()),
						NumberFormatting.BURST_2.format(avgCommitment.multiply(100).longValue()));
				rewards += "\n" + tr("mine_reward_estimation", NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(2.04).longValue()),
						NumberFormatting.BURST_2.format(avgCommitment.multiply(10).longValue()));
				rewards += "\n" + tr("mine_reward_estimation", NumberFormatting.BURST_2.format(burstPerTbPerDay.longValue()),
						NumberFormatting.BURST_2.format(avgCommitment.longValue()));
				rewards += "\n" + tr("mine_reward_estimation_nothing", NumberFormatting.BURST_2.format(burstPerTbPerDay.divide(8).longValue()));
			}
			
			// TODO: leaving this out for now
//			if(totalCapacity > 0) {
//				double capacityTib = (double)totalCapacity/ ONE_GIB /1024.0;
//				
//				String capacity = formatSpace(totalCapacity);
//				double commitmentFactor = 1;
//				if(avgCommitment != null && account.getCommitment() != null) {
//					capacity += "+" + NumberFormatting.BURST_2.format(account.getCommitment().longValue()) + " BURST/TiB";
//					
//					double ratio = account.getCommitment().doubleValue()/avgCommitment.doubleValue();
//			        commitmentFactor = Math.pow(ratio, 0.4515449935);
//			        commitmentFactor = Math.min(8.0, commitmentFactor);
//			        commitmentFactor = Math.max(0.125, commitmentFactor);
//				}
//				
//				rewards += "\n\n" + tr("mine_your_rewards", capacity, NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(capacityTib*commitmentFactor).longValue()));
//				if(avgCommitment == null || account.getCommitment() == null) {
//					rewards += "\n" + tr("mine_reward_poc_plus_activation", NumberFormatting.BURST_2.format(burstPerTbPerDay.multiply(capacityTib*8).longValue()));
//				}
//			}
			
			rewardsEstimationArea.setText(rewards);
		}
		
		if(!mining && poolComboBox.getSelectedItem() != null) {
			if(poolAddresses.get(soloNode) == null)
				poolAddresses.put(soloNode, Globals.getInstance().getAddress());
			
			SignumAddress poolAddress = poolAddresses.get(poolComboBox.getSelectedItem().toString());
			startMiningButton.setEnabled(poolAddress != null
					&& (poolAddress.equals(BN.getRewardRecipient()) || 
							(poolComboBox.getSelectedIndex() == poolComboBox.getItemCount()-1 && BN.getRewardRecipient()==null))
					&& totalCapacity > 0);
		}
		
		lowPriorityCheck.setEnabled(!plotting);
		cpusToPlotComboBox.setEnabled(!plotting);
	}
	
	public void stop() {
		if(mining && minerProcess!=null && minerProcess.isAlive()) {
			mining = false;
	    	logger.info("destroying miner process");
			minerProcess.destroyForcibly();
		}
		if(plotting && plotterProcess!=null && plotterProcess.isAlive()) {
			plotting = false;
	    	logger.info("destroying plotter process");
			plotterProcess.destroyForcibly();
		}
	}
	
	private void saveConfs(Globals g) {
		try {
			g.saveConfs();
		} catch (Exception ex) {
			ex.printStackTrace();
			Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
		}
	}
	
	private void checkForPlotProblems() {
		resumePlotFiles.clear();
		
		for (int i = 0; i < pathList.size(); i++) {
			File pathFile = pathList.get(i);

			if(pathFile == null)
				continue;

			// check if there are pending files to resume
			File[] plotFiles = pathFile.listFiles(PLOT_FILE_FILTER_ANY);
			if(plotFiles != null) {
				for(File plot : plotFiles) {
					
					if(!plot.getName().startsWith(Globals.getInstance().getAddress().getID())) {
						addToConsole(PLOT_APP, "Plot '" + plot.getAbsolutePath() + "' is for a different account and will generate mining errors");
						continue;
					}
					
					int progress = getPlotProgress(plot);
					if(progress >= 0) {
						resumePlotFiles.add(plot);
						addToConsole(PLOT_APP, "Plot '" + plot.getName() + "' is incomplete, start plotting again to resume it");
					}
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int pos = 0;
		
		Globals g = Globals.getInstance();
		
		pos = selectFolderButtons.indexOf(e.getSource());
		if(pos >=0) {
			JFileChooser fileChooser = new JFileChooser();
			if(pathList.get(pos) != null)
				fileChooser.setCurrentDirectory(pathList.get(pos));
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setAcceptAllFileFilterUsed(false);
			
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
			    File selectedPath = fileChooser.getSelectedFile();
			    
			    pathList.set(pos, selectedPath);
			    g.setProperty(PROP_PLOT_PATH + (pos+1), selectedPath.getAbsolutePath());
			    saveConfs(g);
			    selectFolderButtons.get(pos).setText(selectedPath.getAbsolutePath());
			    pathCancelButtons.get(pos).setEnabled(true);
			    openFolderButtons.get(pos).setEnabled(true);
			    
			    fractionToPlotSliders.get(pos).setEnabled(true);
				File[] plotFiles = selectedPath.listFiles(PLOT_FILE_FILTER);
			    fractionToPlotSliders.get(pos).setValue((plotFiles.length > 0) ? 0 : 80);
			    
			    if(pos == pathList.size() - 1) {
			    	// using the last one, add one more
			    	addDiskPath(pos+1);
			    	this.revalidate();
			    }
			    
				checkForPlotProblems();
			}
			return;
		}
		
		pos = pathCancelButtons.indexOf(e.getSource());
		if(pos >=0) {
		    pathList.set(pos, null);
		    selectFolderButtons.get(pos).setText(tr("mine_select_disk"));		    
		    pathCancelButtons.get(pos).setEnabled(false);
		    openFolderButtons.get(pos).setEnabled(false);
		    fractionToPlotSliders.get(pos).setEnabled(false);
		    fractionToPlotSliders.get(pos).setValue(0);
		    sliderListDesc.get(pos).setDesc(" ");

		    g.setProperty(PROP_PLOT_PATH + (pos+1), "");
		    saveConfs(g);
		    return;
		}
		
		if(ssdSelectFolderButton == e.getSource()) {
			JFileChooser fileChooser = new JFileChooser();
			if(ssdPath != null)
				fileChooser.setCurrentDirectory(ssdPath);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				ssdPath = fileChooser.getSelectedFile();
			    
			    g.setProperty(PROP_PLOT_CACHE, ssdPath.getAbsolutePath());
			    saveConfs(g);
			    ssdSelectFolderButton.setText(tr("mine_ssd_cache", ssdPath.getAbsolutePath()));
			    ssdCancelButton.setEnabled(true);		    
			}
			return;
		}
		if(ssdCancelButton == e.getSource()) {
		    ssdPath = null;
		    ssdSelectFolderButton.setText(tr("mine_ssd_cache_select"));		    
		    ssdCancelButton.setEnabled(false);

		    g.setProperty(PROP_PLOT_CACHE, "");
		    saveConfs(g);
		    return;
		}

		pos = openFolderButtons.indexOf(e.getSource());
		if(pos >=0) {
			DesktopApi.open(pathList.get(pos));
			return;
		}
		
		if(startPlotButton == e.getSource()) {
			startPlotting();
			return;
		}
		if(stopPlotButton == e.getSource()) {
			plotting = false;
			stopPlotButton.setEnabled(false);
			checkForPlotProblems();
			update();
			return;
		}

		if(startMiningButton == e.getSource()) {
			startMining();
			return;
		}
		if(stopMiningButton == e.getSource()) {
			addToConsole(MINER_APP, "Stopping miner");
			minerProcess.destroyForcibly();
			mining = false;

			stopMiningButton.setEnabled(false);
			return;
		}

		if(lowPriorityCheck == e.getSource()) {
			g.setProperty(PROP_PLOT_LOW_PRIO, Boolean.toString(lowPriorityCheck.isSelected()));
			saveConfs(g);
			return;
		}
		if(minerAutoStartCheck == e.getSource()) {
			g.setProperty(PROP_MINER_AUTO_START, Boolean.toString(minerAutoStartCheck.isSelected()));
			saveConfs(g);
			return;
		}
		if(mineSubmitOnlyBest == e.getSource()) {
			g.setProperty(PROP_MINE_ONLY_BEST, Boolean.toString(mineSubmitOnlyBest.isSelected()));
			saveConfs(g);
			return;
		}
                if(mineUseDirectIO == e.getSource()) {
			g.setProperty(PROP_MINER_USE_DIRECT_IO, Boolean.toString(mineUseDirectIO.isSelected()));
			saveConfs(g);
			return;
		}
		if(cpusToPlotComboBox == e.getSource()) {
			g.setProperty(PROP_PLOT_CPU_CORES, Integer.toString(cpusToPlotComboBox.getSelectedIndex()+1));
			saveConfs(g);
			return;
		}
		if(cpusToMineComboBox == e.getSource()) {
			g.setProperty(PROP_MINE_CPU_CORES, Integer.toString(cpusToMineComboBox.getSelectedIndex()+1));
			saveConfs(g);
			return;
		}
		
		if(poolComboBox == e.getSource()) {
			joinPoolButton.setText(poolComboBox.getSelectedIndex()==poolComboBox.getItemCount()-1 ?
					tr("send_go_solo") : tr("send_join_pool"));
			
			g.setProperty(PROP_MINER_POOL, poolComboBox.getSelectedItem().toString());
			
			saveConfs(g);
		}
		
		if(addCommitmentButton == e.getSource() || removeCommitmentButton == e.getSource()) {
			
			MiningInfo miningInfo = BurstNode.getInstance().getMiningInfo();
			if(miningInfo != null && miningInfo.getAverageCommitmentNQT() == 0) {
				JOptionPane.showMessageDialog(getParent(),
						tr("mine_commitment_not_available"),
						tr(addCommitmentButton == e.getSource() ? "send_add_commitment" : "send_remove_commitment"),
						JOptionPane.INFORMATION_MESSAGE);

				return;
			}
			
			SendDialog dlg = new SendDialog((JFrame) SwingUtilities.getWindowAncestor(this),
					null, addCommitmentButton == e.getSource() ? SendDialog.TYPE_ADD_COMMITMENT :
						SendDialog.TYPE_REMOVE_COMMITMENT, null);

			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
			
			return;
		}
		
		if(openPoolButton == e.getSource()) {
			try {
				DesktopApi.browse(new URI(poolComboBox.getSelectedItem().toString()));
			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
			return;
		}
		
		if(joinPoolButton == e.getSource()) {
			if(poolComboBox.getSelectedIndex() < 0)
				return;
			
			BurstNode BN = BurstNode.getInstance();
			
			String info = null;
			SignumAddress poolAddress = poolAddresses.get(poolComboBox.getSelectedItem().toString());
			Transaction[] txs = BN.getAccountTransactions();
			Transaction[] utx = BN.getUnconfirmedTransactions();
			ArrayList<Transaction> allTxs = new ArrayList<>();
			if(txs != null)
				Collections.addAll(allTxs, txs);
			if(utx != null)
				Collections.addAll(allTxs, utx);
			
			for(Transaction tx : allTxs) {
				if(tx.getSender().equals(g.getAddress()) && tx.getType() == 20 && tx.getSubtype() == 0 && tx.getConfirmations() < 4) {
					info = tr("mine_wait_join");
					break;
				}
			}
			if(info == null && poolAddress == null) {
				info = tr("mine_invalid_pool");				
			}
			boolean isSolo = poolComboBox.getSelectedIndex() == poolComboBox.getItemCount() -1;
			if(info == null && !isSolo && poolAddress.equals(BN.getRewardRecipient())) {
				info = tr("mine_already_joined");
			}
			if(info == null && isSolo && (BN.getRewardRecipient()==null || poolAddress.equals(BN.getRewardRecipient()))) {
				info = tr("mine_already_solo");
			}
			
			if(info != null) {
				JOptionPane.showMessageDialog(getParent(), info, tr(isSolo ? "send_go_solo" : "send_join_pool"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			SendDialog dlg = new SendDialog((JFrame) SwingUtilities.getWindowAncestor(this),
					null, isSolo ? SendDialog.TYPE_GO_SOLO : SendDialog.TYPE_JOIN_POOL, poolAddress);

			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
			
			return;
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		totalToPlot = 0;
		
		for (int i = 0; i < pathList.size(); i++) {
			File path = pathList.get(i);
			if(path == null)
				continue;
			
			long freeSpace = path.getUsableSpace();
			if(ssdPath != null) {
				// Leave some free space to avoid problems with the sector alignment when moving multiple files.
				// TODO we can remove this if we force to use exact sector sizes
				freeSpace -= 10*BYTES_OF_A_NONCE;
			}
			
			long toUseWithPlots = (freeSpace/100 * fractionToPlotSliders.get(i).getValue());
			totalToPlot += toUseWithPlots;
			double amountToPlot = toUseWithPlots;
			
			String desc = "+" + formatSpace(amountToPlot);
			
			long freeSpaceGb = freeSpace/ONE_GIB;
			if(freeSpaceGb < 1) {
				desc = tr("mine_disk_full");
				fractionToPlotSliders.get(i).setValue(100);
				fractionToPlotSliders.get(i).setEnabled(false);
			}
			
		    sliderListDesc.get(i).setDesc(desc);
		}
		// Add the files to be resumed to the total
		for(File fr : resumePlotFiles) {
			totalToPlot += fr.length();
		}
		
		if(!plotting)
			startPlotButton.setEnabled(totalToPlot > 0 || resumePlotFiles.size() > 0);
	}
	
	public String formatSpace(double bytes) {
		bytes /= ONE_GIB;
		if(bytes < 500) {
		    return NumberFormatting.BURST_2.format(bytes) + " GiB";
		}
		bytes /= 1024;
	    return NumberFormatting.BURST_2.format(bytes) + " TiB";
	}
	
	private void startPlotting() {
		if(plotting)
			return;
		
		logger.info("Starded plotting");
		noncesPlotted.set(0L);
		plotting = true;
		
		startPlotButton.setEnabled(false);
		stopPlotButton.setEnabled(true);
		
		update();
		
		// Start nonce is random and we leave still enough bits for many PiB of unique nonces.
		// This way he user can disconnect disks and plot later or can use it on multiple machines.
		byte[] entropy = new byte[Short.BYTES];
		new SecureRandom().nextBytes(entropy);
		ByteBuffer bb = ByteBuffer.wrap(entropy);
		long startNonce = (bb.getShort() & 0x0FFF) * 100000000000000L;
		
		for(File path : pathList) {
			if(path == null)
				continue;
			
			File[] plotFiles = path.listFiles(PLOT_FILE_FILTER);
			for(File plot : plotFiles) {
				String pieces[] = plot.getName().split("_");
				long start = Long.parseUnsignedLong(pieces[1]);
				long nonces = Long.parseUnsignedLong(pieces[2]);
				startNonce = Math.max(startNonce, start+nonces);
			}
		}
		logger.info("Start nonce is {}", startNonce);
		
		
		Globals g = Globals.getInstance();
		newPlotFiles.clear();
		for (int i = 0; i < pathList.size(); i++) {
			File path = pathList.get(i);
			if(path == null)
				continue;
			
			long freeSpace = path.getUsableSpace();
			
			long toUseWithPlots = freeSpace/100 * fractionToPlotSliders.get(i).getValue();
			
			long noncesToAdd = toUseWithPlots/BYTES_OF_A_NONCE;
			if(noncesToAdd == 0)
				continue;
			
			String newPlot = g.getAddress().getID() + "_" + startNonce + "_" + noncesToAdd;
			newPlotFiles.add(new File(path, newPlot));
			logger.info("Added file to plot {}", newPlot);
			
			startNonce += noncesToAdd + 1;
		}
		
		if(newPlotFiles.size() == 0 && resumePlotFiles.size()==0) {
			plotting = false;
			return;
		}
		
		
		String plotterName = "signum-plotter";
		if(OS.isWindows())
			plotterName += ".exe";
		else if(OS.isMacOsX())
			plotterName += ".app";
		
		plotterFile = new File(TMP_DIR, plotterName);
		if (!plotterFile.exists() || plotterFile.length() == 0) {
		     InputStream link = (getClass().getResourceAsStream("/plotter/" + plotterName));
		     try {
		    	logger.info("Copying ploter to {}", plotterFile.getAbsolutePath());
				Files.copy(link, plotterFile.getAbsoluteFile().toPath());
				if(!OS.isWindows())
					plotterFile.setExecutable(true);
			} catch (IOException ex) {
				ex.printStackTrace();
				Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
				plotting = false;
				return;
			}
		}
		
		PlotThread plotThread = new PlotThread();
		plotThread.start();
	}
	
	class PlotThread extends Thread {

		public void run() {
			long noncesFinished = 0;
			addToConsole(PLOT_APP, "Plotting started for a total of " + formatSpace(totalToPlot) + ", this can be a long process...");
			
			// Cache will use 45% of the free space, so we can have 2 (one moving and one plotting) and do not get a disk full
			long noncesCache = 0;
			if(ssdPath != null) {
				// clear possibly forgotten or killed plots on the cache folder
				File[] lostCacheFiles = ssdPath.listFiles(PLOT_FILE_FILTER);
				if(lostCacheFiles != null) {
					for(File plot : lostCacheFiles) {
						addToConsole(PLOT_APP, "Deleting plot on cache '" + plot.getName() + "'");
						plot.delete();
					}
				}
				
				noncesCache = ssdPath.getUsableSpace() * 45 / (100 * BYTES_OF_A_NONCE);
			}
			
			ArrayList<File> filesToPlot = new ArrayList<>();
			filesToPlot.addAll(resumePlotFiles);
			filesToPlot.addAll(newPlotFiles);
			
			for (File plot : filesToPlot) {
				if(resumePlotFiles.contains(plot)) {
					addToConsole(PLOT_APP, "Resuming plot file '" + plot.getName() + "'");
				}
				String[] sections = plot.getName().split("_");
				
				long noncesInThisPlot = Long.parseLong(sections[2]);
				long nonceStart = Long.parseLong(sections[1]);
				
				long noncesAlreadyPlotted = 0;
				long noncesBeingPlot = noncesInThisPlot;
				
				File fileBeingPlot = plot;
				
				if(ssdPath != null && !resumePlotFiles.contains(plot)) {
					noncesBeingPlot = Math.min(noncesCache, noncesInThisPlot);
				}
				
				while(noncesAlreadyPlotted < noncesInThisPlot) {
					noncesBeingPlot = Math.min(noncesInThisPlot - noncesAlreadyPlotted, noncesBeingPlot);
					if(ssdPath != null && !resumePlotFiles.contains(plot)) {
						fileBeingPlot = new File(ssdPath, sections[0] + "_" + nonceStart + "_" + noncesBeingPlot);
						
						long freeCacheSpaceNow = ssdPath.getUsableSpace()/BYTES_OF_A_NONCE;
						while(freeCacheSpaceNow < noncesCache) {
							addToConsole(PLOT_APP, "Waiting for enough space on your cache disk...");
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								e.printStackTrace();
								plotting = false;
							}
							if(!plotting) {
								addToConsole(PLOT_APP, "Stopped");
								return;
							}
							freeCacheSpaceNow = ssdPath.getUsableSpace()/BYTES_OF_A_NONCE;
						}
					}

					String cmd = plotterFile.getAbsolutePath() + " -i " + sections[0];
					cmd += " -s " + nonceStart;
					cmd += " -n " + noncesBeingPlot;
					cmd += " -c " + (cpusToPlotComboBox.getSelectedIndex()+1);
					cmd += " -q";
					cmd += " -d"; // FIXME: enable back direct io, but we need to find out the sector size then and adjust no of nonces
					if(lowPriorityCheck.isSelected())
						cmd += " -l";

					addToConsole(PLOT_APP, "Plotting file '" + fileBeingPlot.getAbsolutePath() + "'");

					try {
						plotterProcess = Runtime.getRuntime().exec(cmd, null, fileBeingPlot.getParentFile());

						long counter = 0;
						while (plotterProcess.isAlive()) {
							if(!plotting) {
								addToConsole(PLOT_APP, "Stopped");
								plotterProcess.destroyForcibly();
								if(getPlotProgress(fileBeingPlot) < 0) {
									// delete the file, because we will not be able to resume it
									fileBeingPlot.delete();
								}
								break;
							}
							counter++;
							Thread.sleep(100);
							if(counter % 300 == 0) {
								int partial = getPlotProgress(fileBeingPlot);
								noncesPlotted.set(noncesFinished + partial);
							}
						}
						// TODO: apparently for some systems this returns garbage, visit again later
//						if(plotting && plotterProcess.exitValue()!=0) {
//							addToConsole(PLOT_APP, "Error, plotter exit code: " + plotterProcess.exitValue());
//							plotting = false;
//							break;
//						}
						if(!plotting) {
							break;
						}
						nonceStart += noncesBeingPlot;
						noncesAlreadyPlotted += noncesBeingPlot;
						noncesFinished += noncesBeingPlot;
						
						if(ssdPath != null) {
							addToConsole(PLOT_APP, "Moving '" + fileBeingPlot.getName() + "' to '" + plot.getParent() + "'");
							moveFile(fileBeingPlot.toPath(), new File(plot.getParent(), fileBeingPlot.getName()).toPath());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			if(plotting)
				addToConsole(PLOT_APP, "Plotting successfully finished! Be sure to stop and start the miner.");
			if(ssdPath != null)
				addToConsole(PLOT_APP, "But your system might still be moving files from cache");

			plotting = false;
			plotterFile.delete();
			resumePlotFiles.clear();
			
			// Finished, so we reset all sliders
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					for (int i = 0; i < fractionToPlotSliders.size(); i++) {
						fractionToPlotSliders.get(i).setValue(0);
					}
					update();
				}
			});
		};
	};
	
	private void moveFile(Path source, Path target) {
		Thread copyThread = new Thread(() -> {
            try{
            	Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
		copyThread.start();
	}
	
	private int getPlotProgress(File plot) {
		int progress = -1;
		try {
			RandomAccessFile raf = new RandomAccessFile(plot, "r");

			if(raf.length() > 8) {
				// Seek to the end of file
				raf.seek(raf.length() - 8);
				byte []array = new byte[8];
				raf.read(array, 0, 8);

				// Check for the magic bytes at the end of the file
				if(array[4] == -81 && array[5] == -2 && array[6] == -81 && array[7] == -2) {
					ByteBuffer buff = ByteBuffer.wrap(array, 0, 4);
					buff.order(ByteOrder.LITTLE_ENDIAN);
					progress = buff.getInt();
				}
			}
			raf.close();
		}
		catch (Exception e) {
			progress = -1;
			e.printStackTrace();
			logger.info(e.getMessage());
		}
		
		return progress;
	}
	
	private static final String MINER_CONFIG_FILE = "btdex-miner.yaml";
	
	private void startMining() {
		if(mining)
			return;
		
		logger.info("Starded mining");
		mining = true;
		
		startMiningButton.setEnabled(false);
		stopMiningButton.setEnabled(true);
				
		String minerName = "signum-miner";
		if(OS.isWindows())
			minerName += ".exe";
		else if(OS.isMacOsX())
			minerName += ".app";
		
		minerFile = new File(TMP_DIR, minerName);
		InputStream minerStream = (getClass().getResourceAsStream("/miner/" + minerName));
		InputStream minerConfigStream = (getClass().getResourceAsStream("/miner/config.yaml"));
		try {
			if(minerFile.exists() && minerFile.isFile())
				minerFile.delete();
				
			logger.info("Copying miner to {}", minerFile.getAbsolutePath());
			Files.copy(minerStream, minerFile.getAbsoluteFile().toPath());
			if(!OS.isWindows())
				minerFile.setExecutable(true);

			File minerConfigFile = new File(minerFile.getParent(), MINER_CONFIG_FILE);
			FileWriter minerConfig = new FileWriter(minerConfigFile);
			minerConfig.append("plot_dirs:\n");
			for (File path : pathList) {
				if(path == null)
					continue;
				minerConfig.append(" - '" + path.getAbsolutePath() + "'\n");
			}
			minerConfig.append("url: '" + poolComboBox.getSelectedItem().toString() + "'\n");
			
			minerConfig.append("target_deadline: " + poolMaxDeadlines.get(poolComboBox.getSelectedItem().toString()) + "\n");
			if(!mineSubmitOnlyBest.isSelected())
				minerConfig.append("submit_only_best: false\n");
			
			minerConfig.append("cpu_threads: " + (cpusToMineComboBox.getSelectedIndex()+1) + "\n");
			minerConfig.append("cpu_worker_task_count: " + (cpusToMineComboBox.getSelectedIndex()+1) + "\n");

			minerConfig.append("additional_headers: \n");
			minerConfig.append("  \"x-miner\" : \"btdex-" + Globals.getInstance().getVersion() + "\" \n");

                        if(mineUseDirectIO.isSelected()) {
                            minerConfig.append("hdd_use_direct_io: true               # default true\n");
                        } else {
                            minerConfig.append("hdd_use_direct_io: false              # default true\n");
                        }

			logger.info("Copying miner config to {}", minerConfigFile.getAbsolutePath());

			IOUtils.copy(minerConfigStream, minerConfig);
			minerConfig.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
			mining = false;
			return;
		}
		MineThread mineThread = new MineThread();
		mineThread.start();
	}
	
	class MineThread extends Thread {

		public void run() {
			try {
				String cmd = minerFile.getAbsolutePath() + " -c " + MINER_CONFIG_FILE;
				logger.info("Running miner with '{}'", cmd);
				minerProcess = Runtime.getRuntime().exec(cmd, null, minerFile.getParentFile());
				
				InputStream stdIn = minerProcess.getInputStream();
				InputStreamReader isr = new InputStreamReader(stdIn);
				BufferedReader br = new BufferedReader(isr);

				while (minerProcess.isAlive()) {
					String line = br.readLine();
					if(line != null)
						addToConsole(MINER_APP, line);
				}
				mining = false;
				minerFile.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private void addToConsole(String app, String line) {
    	logger.info("{} {}", app, line);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// lets have a limit of 400 lines
			    int numLinesToTrunk = console.getLineCount() - 400;
			    if(numLinesToTrunk > 0) {
			    	try {
			    		int posOfLastLineToTrunk = console.getLineEndOffset(numLinesToTrunk - 1);
			    		console.replaceRange("",0,posOfLastLineToTrunk);
			    	}
			    	catch (BadLocationException ex) {
			    		ex.printStackTrace();
			    	}
			    }
			    
				Date now = new Date(System.currentTimeMillis());
				
				console.append(app);
				console.append(" ");
				console.append(TIME_FORMAT.format(now));
				console.append(" ");
				console.append(line);
				console.append("\n");
				console.setCaretPosition(console.getDocument().getLength());
			}
		});
	}
	
}
