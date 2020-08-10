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

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.Constants;

public class MiningPanel extends JPanel implements ActionListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	
	private static final NumberFormat NF = NumberFormat.getNumberInstance();
	
	private static final int N_PATHS = 6;
	private static final long ONE_GIB = 1073741824L;
	
	private ArrayList<JTextField> pathFields = new ArrayList<>();
	private ArrayList<JButton> trashButtons = new ArrayList<>();
	private ArrayList<JButton> selectFolderButtons = new ArrayList<>();
	private ArrayList<JSlider> fractionToPlotSliders = new ArrayList<>();

	private ArrayList<Desc> sliderListDesc = new ArrayList<>();

	private ArrayList<File> pathList = new ArrayList<>();
	
	private JButton cancelPlot;

	public MiningPanel() {
		super(new BorderLayout());
		
		Icons icons = new Icons(this.getForeground(), Constants.ICON_SIZE_MED);
		
		JPanel folderPanels = new JPanel(new GridLayout(0, 1));
		
		for (int i = 0; i < N_PATHS; i++) {
			
			JTextField pathField = new JTextField(20);
			pathField.setEditable(false);
			
			JButton trashButton = new JButton(icons.get(Icons.TRASH));
			trashButton.addActionListener(this);
			
			JButton selectFolderButton = new JButton(icons.get(Icons.FOLDER));
			selectFolderButton.addActionListener(this);
			
			JSlider fractionToPlotSlider = new JSlider(0, 100, 0);
			fractionToPlotSlider.addChangeListener(this);
			fractionToPlotSlider.setEnabled(false);
			
			Desc sliderDesc = new Desc(" ", fractionToPlotSlider);
			
			pathFields.add(pathField);
			trashButtons.add(trashButton);
			selectFolderButtons.add(selectFolderButton);
			fractionToPlotSliders.add(fractionToPlotSlider);
			sliderListDesc.add(sliderDesc);
			
			pathList.add(null);
			
			JPanel folderPanel = new JPanel(new FlowLayout());
			folderPanel.add(pathField);
			folderPanel.add(selectFolderButton);
			folderPanel.add(sliderDesc);
			folderPanel.add(trashButton);
			
			folderPanels.add(folderPanel);
		}
		
		JPanel plotButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		cancelPlot = new JButton(tr("dlg_cancel"));
		cancelPlot.setEnabled(false);
		
		JButton plotButton = new JButton(tr("mine_plot"), icons.get(Icons.HDD));
		plotButton.addActionListener(this);
		
		plotButtonsPanel.add(cancelPlot);
		plotButtonsPanel.add(plotButton);
		folderPanels.add(plotButtonsPanel);
		
		
		JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, folderPanels, new JLabel("test"));
		
		add(topPanel, BorderLayout.PAGE_START);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int pos = 0;
		
		pos = selectFolderButtons.indexOf(e.getSource());
		if(pos >=0) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
			
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
			    File selectedPath = fileChooser.getSelectedFile();
			    
			    pathList.set(pos, selectedPath);
			    
			    pathFields.get(pos).setText(selectedPath.getAbsolutePath());
			    
			    fractionToPlotSliders.get(pos).setEnabled(true);
			    fractionToPlotSliders.get(pos).setValue(50);
			}
		}
		
		pos = trashButtons.indexOf(e.getSource());
		if(pos >=0) {
			
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
