package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Transaction;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MiningPanel extends JPanel implements ActionListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	
	private static final NumberFormat NF = NumberFormat.getNumberInstance();
	
	private static final int N_PATHS = 4;
	private static final long ONE_GIB = 1073741824L;
	
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
	private ArrayList<JButton> selectFolderButtons = new ArrayList<>();
	private ArrayList<JSlider> fractionToPlotSliders = new ArrayList<>();

	private ArrayList<Desc> sliderListDesc = new ArrayList<>();

	private ArrayList<File> pathList = new ArrayList<>();
	
	private JTextArea console;

	private JButton ssdSelectFolderButton;

	private JButton ssdCancelButton;

	private JButton plotButton;

	private JComboBox<String> cpusToUse;

	private JComboBox<String> poolComboBox;

	private JButton joinPoolButton;

	private JTextField committedAmountField;

	private JButton addCommitmentButton;

	private JButton removeCommitmentButton;

	private JCheckBox minerAutoStart;

	private JButton startMiningButton;

	private JCheckBox lowPriorityCheck;

	private JTextArea rewardsEstimationArea;

	public MiningPanel() {
		super(new BorderLayout());
		
		Icons icons = new Icons(this.getForeground(), Constants.ICON_SIZE_MED);
		
		JPanel plottingPanel = new JPanel(new GridLayout(0, 1));
		plottingPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_plot_disks")));
		
		for (int i = 0; i < N_PATHS; i++) {
			
			JButton cancelButton = new JButton(icons.get(Icons.CANCEL));
			cancelButton.addActionListener(this);
			
			JButton selectFolderButton = new JButton("Select a disk folder to use", icons.get(Icons.FOLDER));
			selectFolderButton.addActionListener(this);
			
			JSlider fractionToPlotSlider = new JSlider(0, 100, 0);
			fractionToPlotSlider.addChangeListener(this);
			fractionToPlotSlider.setEnabled(false);
			
			Desc sliderDesc = new Desc(" ", fractionToPlotSlider);
			
			pathCancelButtons.add(cancelButton);
			selectFolderButtons.add(selectFolderButton);
			fractionToPlotSliders.add(fractionToPlotSlider);
			sliderListDesc.add(sliderDesc);
			
			pathList.add(null);
			
			JPanel folderBorderPanel = new JPanel(new BorderLayout(2, 0));
			folderBorderPanel.add(selectFolderButton, BorderLayout.CENTER);
			folderBorderPanel.add(cancelButton, BorderLayout.LINE_START);
			JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			folderPanel.add(sliderDesc);
			folderBorderPanel.add(folderPanel, BorderLayout.LINE_END);
			
			plottingPanel.add(folderBorderPanel);
		}
		
		JPanel ssdPanel = new JPanel(new BorderLayout(2, 0));
		ssdSelectFolderButton = new JButton("SSD cache (optional, needed for external SMRs)", icons.get(Icons.FOLDER));
		ssdSelectFolderButton.addActionListener(this);
		ssdPanel.add(ssdSelectFolderButton, BorderLayout.CENTER);
		
		ssdCancelButton = new JButton(icons.get(Icons.CANCEL));
		ssdCancelButton.addActionListener(this);
		
		ssdPanel.add(ssdCancelButton, BorderLayout.LINE_START);
		plottingPanel.add(ssdPanel);
		
		// CPU cores
		int cores = Runtime.getRuntime().availableProcessors();
		int selectedCores = Math.max(0, cores-3);
		cpusToUse = new JComboBox<String>();
		for (int i = 1; i <= cores; i++) {
			cpusToUse.addItem(Integer.toString(i));			
		}
		cpusToUse.setSelectedIndex(selectedCores);
		
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

		plotButton = new JButton(tr("mine_plot"), icons.get(Icons.PLOTTING));
		plotButton.addActionListener(this);
		
		plotButtonsPanel.add(new JLabel("CPU Cores"));
		plotButtonsPanel.add(cpusToUse);
		plotButtonsPanel.add(lowPriorityCheck = new JCheckBox("Run with low priority"));
		plotButtonsPanel.add(plotButton);
		plottingPanel.add(plotButtonsPanel);
		
		
		JPanel poolPanel = new JPanel(new FlowLayout());
		poolPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_join_pool")));
		poolPanel.add(new JLabel("Select Pool:"));
		
		Globals g = Globals.getInstance();
		
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
		
		joinPoolButton = new JButton("Join pool", icons.get(Icons.PLUG));
		joinPoolButton.addActionListener(this);
		poolPanel.add(joinPoolButton);
		

		JPanel rewardsPanel = new JPanel();
		rewardsPanel.setBorder(BorderFactory.createTitledBorder(tr("mine_rewards_and_commitment")));
		rewardsPanel.setLayout(new BoxLayout(rewardsPanel, BoxLayout.Y_AXIS));
		
		rewardsEstimationArea = new JTextArea(3, 20);
		rewardsEstimationArea.setText("Waiting for network data...");
		rewardsEstimationArea.setEditable(false);
		rewardsPanel.add(rewardsEstimationArea);

		JPanel commitmentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rewardsPanel.add(commitmentPanel);
		commitmentPanel.add(new JLabel("Your Committed Amount (BURST):"));

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
		minerPanel.add(minerAutoStart = new JCheckBox("Start automatically"));

		startMiningButton = new JButton("Start Mining", icons.get(Icons.MINING));
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
        consolePanel.setBorder(BorderFactory.createTitledBorder("Console"));
		
		add(topPanel, BorderLayout.PAGE_START);
		add(consolePanel, BorderLayout.CENTER);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		int pos = 0;
		
		pos = selectFolderButtons.indexOf(e.getSource());
		if(pos >=0) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
			    File selectedPath = fileChooser.getSelectedFile();
			    
			    pathList.set(pos, selectedPath);
			    
			    selectFolderButtons.get(pos).setText(selectedPath.getAbsolutePath());
			    
			    fractionToPlotSliders.get(pos).setEnabled(true);
			    fractionToPlotSliders.get(pos).setValue(80);
			}
		}
		
		pos = pathCancelButtons.indexOf(e.getSource());
		if(pos >=0) {
			
		}
		
		if(addCommitmentButton == e.getSource() || removeCommitmentButton == e.getSource()) {
			SendDialog dlg = new SendDialog((JFrame) SwingUtilities.getWindowAncestor(this),
					null, addCommitmentButton == e.getSource() ? SendDialog.TYPE_ADD_COMMITMENT :
						SendDialog.TYPE_REMOVE_COMMITMENT, null);

			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
			
			return;
		}
		
		if(joinPoolButton == e.getSource()) {
			if(poolComboBox.getSelectedIndex() < 0)
				return;
			
			BurstNode BN = BurstNode.getInstance();
			
			BurstAddress poolAddress = poolAddresses.get(poolComboBox.getSelectedIndex());
			String info = null;
			if(BN.getRewardRecipient().equals(poolAddress)) {
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
		for (int i = 0; i < pathList.size(); i++) {
			File path = pathList.get(i);
			if(path == null)
				continue;
			
			double usableSpace = path.getFreeSpace();
			
			double usedSpace = usableSpace/ONE_GIB;
			
			usedSpace *= fractionToPlotSliders.get(i).getValue()/100d;
			
			if(usedSpace < 500) {
			    sliderListDesc.get(i).setDesc(NF.format(usedSpace) + " GiB");
			}
			else {
				usedSpace /= 1024;
			    sliderListDesc.get(i).setDesc(NF.format(usedSpace) + " TiB");
			}			
		}
	}
}
