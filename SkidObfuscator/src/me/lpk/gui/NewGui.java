package me.lpk.gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import me.lpk.Skidfuscate;
import me.lpk.gui.drop.IDropUser;
import me.lpk.gui.drop.JarDropHandler;
import me.lpk.gui.panel.ObfuscationPanel;
import me.lpk.gui.panel.OptimizationPanel;
import me.lpk.gui.panel.SettingsPanel;
import me.lpk.options.Options;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class NewGui implements IDropUser {
	private final static int ID_LOAD_LIBRARY = 0;
	private final static int ID_LOAD_TARGET = 1;

	private JFrame frmSkidfuscator;
	private final JPanel  pnlInputs = new JPanel();
	private final SettingsPanel 
			pnlObfuscation = new ObfuscationPanel(), 
			pnlOptimization = new OptimizationPanel();
	private final List<SettingsPanel> settingsPanels = new ArrayList<SettingsPanel>();
	private final Skidfuscate instance = Skidfuscate.get();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		 try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    }  catch (Exception e) {
	    	
	    }
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					NewGui window = new NewGui();
					window.frmSkidfuscator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public NewGui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSkidfuscator = new JFrame();
		frmSkidfuscator.setTitle("Skidfuscator");
		frmSkidfuscator.setBounds(100, 100, 704, 475);
		frmSkidfuscator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		frmSkidfuscator.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		pnlInputs.setLayout(new BorderLayout(0, 0));

		addDropZones(pnlInputs);
		settingsPanels.add(pnlObfuscation);
		settingsPanels.add(pnlOptimization);
		
		tabbedPane.addTab("Inputs", null, pnlInputs, "Where the inputs are chosen");
		tabbedPane.addTab("Obfuscation", null, pnlObfuscation, "Options for obfuscation");
		tabbedPane.addTab("Optimization", null, pnlOptimization, "Options for optimization");
		tabbedPane.setSelectedIndex(1);
	}

	private void addDropZones(JPanel pnl) {
		TransferHandler dropHandler_TargetJar = new JarDropHandler(this, ID_LOAD_TARGET);
		TransferHandler dropHandler_LibraryJar = new JarDropHandler(this, ID_LOAD_LIBRARY);
		JSplitPane splitDrag = new JSplitPane();
		JLabel lblDragAJar = new JLabel("Drag program here");
		JLabel lblDragALibrary = new JLabel("Drag libraries here");
		splitDrag.setBackground(SystemColor.controlShadow);
		lblDragALibrary.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblDragALibrary.setBackground(SystemColor.controlShadow);
		lblDragALibrary.setHorizontalAlignment(SwingConstants.CENTER);
		lblDragAJar.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblDragAJar.setBackground(new Color(240, 240, 240));
		lblDragAJar.setHorizontalAlignment(SwingConstants.CENTER);
		lblDragALibrary.setTransferHandler(dropHandler_LibraryJar);
		lblDragAJar.setTransferHandler(dropHandler_TargetJar);
		splitDrag.setLeftComponent(lblDragAJar);
		splitDrag.setRightComponent(lblDragALibrary);
		splitDrag.setDividerLocation(290);
		pnl.add(splitDrag, BorderLayout.CENTER);
	}

	@Override
	public void preLoadJars(int id) {
		if (id == ID_LOAD_TARGET) {

		} else if (id == ID_LOAD_LIBRARY) {

		}
	}

	@Override
	public void onJarLoad(int id, File jar) {
		if (id == ID_LOAD_TARGET) {
			instance.parse(jar, Options.boolsFromGui(this), Options.stringsFromGui(this));
		} else if (id == ID_LOAD_LIBRARY) {
			instance.addLibrary(jar);
		}
	}

	public List<SettingsPanel> getSettingPanels() {
		return settingsPanels;
	}
}
