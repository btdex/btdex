package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.MiningInfo;
import burst.kit.entity.response.Transaction;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MiningPanel extends JPanel implements ActionListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	
	private static final NumberFormat NF = NumberFormat.getNumberInstance();
	
	private static final int N_PATHS = 4;
	private static final long ONE_GIB = 1073741824L;
	private static final long BYTES_OF_A_NONCE = 262144L;
	
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static String TMP_DIR = System.getProperty("java.io.tmpdir");
	
	private static final String PROP_PLOT_PATH = "plotPath";
	private static final String PROP_PLOT_LOW_PRIO = "plotLowPrio";
	private static final String PROP_PLOT_CPU_CORES = "plotCpuCores";
	private static final String PROP_MINER_AUTO_START = "minerAutoStart";
	
	private static final String PLOT_APP = "[PLOTTER]";
	private static final String MINER_APP = "[MINER]";
	
	private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private static final FileFilter PLOT_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() &&
					pathname.getName().startsWith(Globals.getInstance().getAddress().getID()) &&
					pathname.getName().split("_").length == 3;
		}
	};
	
	private static final String[] POOL_LIST = {
			"https://bmf100pool.burstcoin.ro:443",
			"http://pool.burstcoin.ro:8080",
			"http://openburstpool.ddns.net:8126"
	};
	
	private static final String[] POOL_LIST_TESTNET = {
			"http://nivbox.co.uk:9000",
	};
	
	private ArrayList<BurstAddress> poolAddresses = new ArrayList<>();
	
	private ArrayList<JButton> pathCancelButtons = new ArrayList<>();
	private ArrayList<JButton> openFolderButtons = new ArrayList<>();
	private ArrayList<JButton> selectFolderButtons = new ArrayList<>();
	private ArrayList<JSlider> fractionToPlotSliders = new ArrayList<>();

	private ArrayList<Desc> sliderListDesc = new ArrayList<>();

	private ArrayList<File> pathList = new ArrayList<>();
	
	private JTextArea console;

	private JButton ssdSelectFolderButton;

	private JButton ssdCancelButton;

	private JButton startPlotButton, stopPlotButton;

	private JComboBox<String> cpusToUseComboBox;

	private JComboBox<String> poolComboBox;

	private JButton joinPoolButton, openPoolButton;

	private JTextField committedAmountField;

	private JButton addCommitmentButton;

	private JButton removeCommitmentButton;

	private JCheckBox minerAutoStartCheck;

	private JButton startMiningButton, stopMiningButton;
	
	private Process minerProcess;

	private JCheckBox lowPriorityCheck;

	private JTextArea rewardsEstimationArea;

	private ArrayList<File> newPlotFiles = new ArrayList<>();

	private boolean plotting, mining;

	private Logger logger;

	private File minerFile, plotterFile;

	private RotatingIcon pendingIconRotating;

	private Icon folderIcon;

	private long totalToPlot;
	private long totalCapacity;
	
	private AtomicReference<Long> noncesPlotted = new AtomicReference<>();

	private Icon iconPlot;

	public MiningPanel() {
		super(new BorderLayout());
		
		logger = LogManager.getLogger();
		
		Icons icons = new Icons(this.getForeground(), Constants.ICON_SIZE_MED);
		
		Icon pendingIcon = icons.get(Icons.SPINNER);
		pendingIconRotating = new RotatingIcon(pendingIcon);
		folderIcon = icons.get(Icons.FOLDER);
		
		JPanel plottingPanel = new JPanel(new GridLayout(0, 1));
		plottingPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_plot_disks")));
		
		Globals g = Globals.getInstance();
		
		for (int i = 0; i < N_PATHS; i++) {
			
			JButton cancelButton = new JButton(icons.get(Icons.CANCEL));
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
			
			plottingPanel.add(folderBorderPanel);
		}
		
		JPanel ssdPanel = new JPanel(new BorderLayout(2, 0));
		ssdSelectFolderButton = new JButton(tr("mine_ssd_cache"), icons.get(Icons.FOLDER));
		ssdSelectFolderButton.addActionListener(this);
		ssdPanel.add(ssdSelectFolderButton, BorderLayout.CENTER);
		
		ssdCancelButton = new JButton(icons.get(Icons.CANCEL));
		ssdCancelButton.addActionListener(this);
		ssdCancelButton.setEnabled(false);
		
		ssdPanel.add(ssdCancelButton, BorderLayout.LINE_START);
		plottingPanel.add(ssdPanel);
		
		// CPU cores
		int coresAvailable = Runtime.getRuntime().availableProcessors();
		int selectedCores = Math.max(1, coresAvailable-2);
		if(g.getProperty(PROP_PLOT_CPU_CORES) != null && g.getProperty(PROP_PLOT_CPU_CORES).length() > 0) {
			selectedCores = Integer.parseInt(g.getProperty(PROP_PLOT_CPU_CORES));
		}
		selectedCores = Math.min(selectedCores, coresAvailable);
		selectedCores = Math.max(selectedCores, 1);
		cpusToUseComboBox = new JComboBox<String>();
		for (int i = 1; i <= coresAvailable; i++) {
			cpusToUseComboBox.addItem(Integer.toString(i));			
		}
		cpusToUseComboBox.setSelectedIndex(selectedCores-1);
		cpusToUseComboBox.addActionListener(this);
		
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
				
		JPanel plotButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		iconPlot = icons.get(Icons.PLOTTING);
		startPlotButton = new JButton(tr("mine_plot"), iconPlot);
		startPlotButton.addActionListener(this);
		startPlotButton.setEnabled(false);
		
		stopPlotButton = new JButton(tr("mine_plot_stop"), icons.get(Icons.CANCEL));
		stopPlotButton.addActionListener(this);
		stopPlotButton.setEnabled(false);
		
		plotButtonsPanel.add(new JLabel("CPU Cores"));
		plotButtonsPanel.add(cpusToUseComboBox);
		plotButtonsPanel.add(lowPriorityCheck = new JCheckBox(tr("mine_run_low_prio")));
		plotButtonsPanel.add(stopPlotButton);
		plotButtonsPanel.add(startPlotButton);
		plottingPanel.add(plotButtonsPanel);
		
		lowPriorityCheck.setSelected(Boolean.parseBoolean(g.getProperty(PROP_PLOT_LOW_PRIO)));
		lowPriorityCheck.addActionListener(this);
		
		
		JPanel poolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		poolPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_join_pool")));
		poolPanel.add(new JLabel(tr("mine_select_pool")));
		
		poolComboBox = new JComboBox<String>();
		OkHttpClient client = new OkHttpClient();
		for (String poolURL : (g.isTestnet() ? POOL_LIST_TESTNET : POOL_LIST)) {
			String poolURLgetConfig = poolURL + "/api/getConfig";
			
			try {
				Request request = new Request.Builder().url(poolURLgetConfig).build();
				Response responses = client.newCall(request).execute();
				String jsonData = responses.body().string();
				JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

				BurstAddress address = BurstAddress.fromEither(json.get("poolAccount").getAsString());
				if(address != null) {
					poolComboBox.addItem(poolURL);
					poolAddresses.add(address);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		poolPanel.add(poolComboBox);
		
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
		
		
		JPanel minerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		minerPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_run_miner")));
		minerPanel.add(minerAutoStartCheck = new JCheckBox(tr("mine_start_auto")));
		minerAutoStartCheck.setSelected(Boolean.parseBoolean(g.getProperty(PROP_MINER_AUTO_START)));
		minerAutoStartCheck.addActionListener(this);

		stopMiningButton = new JButton(tr("mine_stop_mining"), icons.get(Icons.CANCEL));
		stopMiningButton.addActionListener(this);
		stopMiningButton.setEnabled(false);
		minerPanel.add(stopMiningButton);
		startMiningButton = new JButton(tr("mine_start_mining"), icons.get(Icons.MINING));
		startMiningButton.addActionListener(this);
		minerPanel.add(startMiningButton);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(rewardsPanel);
		rightPanel.add(poolPanel);
		rightPanel.add(minerPanel);
		
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
		
		if(minerAutoStartCheck.isSelected()) {
			new java.util.Timer().schedule( 
			        new java.util.TimerTask() {
			            public void run() {
			                startMining();
			            }
			        }, 3000);
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
				for(File plot : plotFiles) {
					amountInPlots += plot.length();
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
			}

			stopPlotButton.setEnabled(false);
			startPlotButton.setIcon(iconPlot);
			pendingIconRotating.removeComponent(startPlotButton);
			startPlotButton.setText(tr("mine_plot"));
		}
		
		MiningInfo miningInfo = BN.getMiningInfo();
		Block latestBlock = BN.getLatestBlock();
		if(miningInfo != null && latestBlock != null) {
			double networkTbs = 18325193796.0/miningInfo.getBaseTarget()/1.83;
			BurstValue burstPerTbPerDay = BurstValue.fromBurst(360.0/networkTbs * latestBlock.getBlockReward().doubleValue());

			String rewards = burstPerTbPerDay.toFormattedString();
			BurstValue avgCommitment = null;
			if(miningInfo.getAverageCommitmentNQT() > 0) {
				avgCommitment = BurstValue.fromPlanck(miningInfo.getAverageCommitmentNQT());
				rewards = tr("mine_reward_estimation", burstPerTbPerDay.multiply(8).toFormattedString(), avgCommitment.multiply(100).toFormattedString());
				rewards += "\n" + tr("mine_reward_estimation", burstPerTbPerDay.toFormattedString(), avgCommitment.toFormattedString());
				rewards += "\n" + tr("mine_reward_estimation_nothing", burstPerTbPerDay.divide(8).toFormattedString());
			}
			
			if(totalCapacity > 0) {
				double capacityTib = (double)totalCapacity/ ONE_GIB /1024.0;
				
				String capacity = formatSpace(totalCapacity);
				if(avgCommitment != null && account.getCommitment() != null) {
					capacity += "+" + account.getCommitment().toFormattedString() + "/TiB";
					
					double ratio = account.getCommitment().doubleValue()/avgCommitment.doubleValue();
			        double commitmentFactor = Math.pow(ratio, 0.4515449935);
			        commitmentFactor = Math.min(8.0, commitmentFactor);
			        commitmentFactor = Math.max(0.125, commitmentFactor);
			        
			        capacityTib *= commitmentFactor;
				}
				
				rewards += "\n\n" + tr("mine_your_rewards", capacity, burstPerTbPerDay.multiply(capacityTib).toFormattedString());
			}
			
			rewardsEstimationArea.setText(rewards);
		}
		
		if(!mining) {
			BurstAddress poolAddress = poolAddresses.get(poolComboBox.getSelectedIndex());
			startMiningButton.setEnabled(poolAddress.equals(BN.getRewardRecipient()) && totalCapacity > 0);
		}
		
		lowPriorityCheck.setEnabled(!plotting);
		cpusToUseComboBox.setEnabled(!plotting);
	}
	
	private void saveConfs(Globals g) {
		try {
			g.saveConfs();
		} catch (Exception ex) {
			ex.printStackTrace();
			Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
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
			    fractionToPlotSliders.get(pos).setValue(80);
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
		if(cpusToUseComboBox == e.getSource()) {
			g.setProperty(PROP_PLOT_CPU_CORES, Integer.toString(cpusToUseComboBox.getSelectedIndex()+1));
			saveConfs(g);
			return;
		}
		
		if(addCommitmentButton == e.getSource() || removeCommitmentButton == e.getSource()) {
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
			
			BurstAddress poolAddress = poolAddresses.get(poolComboBox.getSelectedIndex());
			String info = null;
			if(poolAddress.equals(BN.getRewardRecipient())) {
				info = tr("mine_already_joined");
			}
			for(Transaction tx : BN.getAccountTransactions()) {
				if(tx.getType() == 20 && tx.getSubtype() == 0 && tx.getConfirmations() < 4) {
					info = tr("mine_wait_join");
					break;
				}
			}
			
			if(info != null) {
				JOptionPane.showMessageDialog(getParent(), info, tr("send_join_pool"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			SendDialog dlg = new SendDialog((JFrame) SwingUtilities.getWindowAncestor(this),
					null, SendDialog.TYPE_JOIN_POOL, poolAddress);

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
			
			long freeSpace = path.getFreeSpace();
			
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
		
		if(!plotting)
			startPlotButton.setEnabled(totalToPlot > 0);
	}
	
	public String formatSpace(double bytes) {
		bytes /= ONE_GIB;
		if(bytes < 500) {
		    return NF.format(bytes) + " GiB";
		}
		bytes /= 1024;
	    return NF.format(bytes) + " TiB";
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
		
		long startNonce = 0;
		for(File path : pathList) {
			if(path == null)
				continue;
			
			File[] plotFiles = path.listFiles(PLOT_FILE_FILTER);
			for(File plot : plotFiles) {
				String pieces[] = plot.getName().split("_");
				long start = Long.parseLong(pieces[1]);
				long nonces = Long.parseLong(pieces[2]);
				startNonce = Math.max(startNonce, start+nonces+1);
			}
		}
		logger.info("Start nonce is {}", startNonce);
		
		
		Globals g = Globals.getInstance();
		newPlotFiles.clear();
		for (int i = 0; i < pathList.size(); i++) {
			File path = pathList.get(i);
			if(path == null)
				continue;
			
			long freeSpace = path.getFreeSpace();
			
			long toUseWithPlots = freeSpace/100 * fractionToPlotSliders.get(i).getValue();
			
			long noncesToAdd = toUseWithPlots/BYTES_OF_A_NONCE;
			if(noncesToAdd == 0)
				continue;
			
			String newPlot = g.getAddress().getID() + "_" + startNonce + "_" + noncesToAdd;
			newPlotFiles.add(new File(path, newPlot));
			logger.info("Added file to plot {}", newPlot);
			
			startNonce += noncesToAdd + 1;
		}
		
		if(newPlotFiles.size() == 0) {
			plotting = false;
			return;
		}
		
		
		String engraverName = "engraver_cpu";
		if(OS.contains("win"))
			engraverName += ".exe";
		plotterFile = new File(TMP_DIR, engraverName);
		if (!plotterFile.exists() || plotterFile.length() == 0) {
		     InputStream link = (getClass().getResourceAsStream("/engraver/" + engraverName));
		     try {
				Files.copy(link, plotterFile.getAbsoluteFile().toPath());
				if(!OS.contains("win"))
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
			
			for (File plot : newPlotFiles) {
				String[] sections = plot.getName().split("_");
				String cmd = plotterFile.getAbsolutePath() + " -i " + sections[0];
				cmd += " -s " + sections[1]; 
				cmd += " -n " + sections[2];
				cmd += " -c " + (cpusToUseComboBox.getSelectedIndex()+1);
				cmd += " -q";
				cmd += " -d"; // FIXME: enable back direct io, but we need to find out the sector size then and adjust no of nonces
				if(lowPriorityCheck.isSelected())
					cmd += " -l";

				addToConsole(PLOT_APP, "Plotting file " + plot.getAbsolutePath());
				
				long noncesInThisPlot = Long.parseLong(sections[2]);
				try {
					Process process = Runtime.getRuntime().exec(cmd, null, plot.getParentFile());
					
					long counter = 0;
					while (process.isAlive()) {
						if(!plotting) {
							// stop requested
							process.destroyForcibly();
							break;
						}
						counter++;
						Thread.sleep(100);
						if(counter % 100 == 0) {
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
									int partial = buff.getInt();

									noncesPlotted.set(noncesFinished + partial);
								}
							}
							raf.close();
						}
					}
					noncesFinished += noncesInThisPlot;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			addToConsole(PLOT_APP, "Plotting successfully finished!");

			plotting = false;
			plotterFile.delete();
			
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
	
	private static final String MINER_CONFIG_FILE = "btdex-miner.yaml";
	
	private void startMining() {
		if(mining)
			return;
		
		logger.info("Starded mining");
		mining = true;
		
		startMiningButton.setEnabled(false);
		stopMiningButton.setEnabled(true);
				
		String minerName = "scavenger";
		if(OS.contains("win"))
			minerName += ".exe";
		minerFile = new File(TMP_DIR, minerName);
		InputStream minerStream = (getClass().getResourceAsStream("/scavenger/" + minerName));
		InputStream minerConfigStream = (getClass().getResourceAsStream("/scavenger/config.yaml"));
		try {
			if(minerFile.exists() && minerFile.isFile())
				minerFile.delete();
				
			Files.copy(minerStream, minerFile.getAbsoluteFile().toPath());
			if(!OS.contains("win"))
				minerFile.setExecutable(true);

			FileWriter minerConfig = new FileWriter(minerFile.getParent() + "/" + MINER_CONFIG_FILE);
			minerConfig.append("plot_dirs:\n");
			for (File path : pathList) {
				if(path == null)
					continue;
				minerConfig.append(" - '" + path.getAbsolutePath() + "'\n");
			}
			minerConfig.append("url: '" + poolComboBox.getSelectedItem().toString() + "'\n");
			IOUtils.copy(minerConfigStream, minerConfig);
			minerConfig.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this), ex.getMessage(), Toast.Style.ERROR).display();
			plotting = false;
			return;
		}
		MineThread mineThread = new MineThread();
		mineThread.start();
	}
	
	class MineThread extends Thread {

		public void run() {
			try {
				String cmd = minerFile.getAbsolutePath() + " -c " + MINER_CONFIG_FILE;
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
