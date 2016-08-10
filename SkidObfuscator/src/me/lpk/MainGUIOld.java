package me.lpk;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;

import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import me.lpk.gui.drop.IDropUser;
import me.lpk.gui.drop.JarDropHandler;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.SkidRemapper;
import me.lpk.optimization.Optimizer;
import me.lpk.optimization.SimpleRemover;
import me.lpk.optimization.Remover;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

import javax.swing.border.EtchedBorder;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public class MainGUIOld implements IDropUser {
	private JCheckBox chkUseMaxs, chkOptimization;
	private JCheckBox chkRemoveSource, chkRemoveInnerOuter, chkRemoveClassAnnotations, chkRemoveClassAttribs, chkRemoveMethods;
	private JCheckBox chkRemoveParameter, chkRemoveMethodsAnnotations, chkRemoveLocals, chkRemoveLines, chkRemoveFrames, chkRemoveMethodAttribs;
	private JFrame frmSkidshrink;
	private JLabel lblOpMainOptions;
	private JLabel lblClassOptions;
	private JLabel lblMethodOptions;
	private JLabel lblDragALibrary;
	private JSplitPane splitDrag;
	private JPanel pnlLibraries;
	private JList<String> lstLoadedLibs;
	private JTabbedPane tabbedPane;
	private JPanel pnlObfuscation;
	private final Optimizer optimizer = new Optimizer();
	private Set<File> libraries = new HashSet<File>();
	private JPanel pnlObMainOptions;
	private JLabel lblObMainOptions;
	private JCheckBox chkStringObfuscation;
	private JCheckBox chckbxFlowObfuscation;
	private JCheckBox chckbxAntidecompile;
	private JPanel pnlObStringOptions;
	private JLabel lblObStringOptions;
	private JCheckBox chckbxNewCheckBox;
	private JPanel pnlObAnti;
	private JLabel lblAntidecompileOptions;
	private JCheckBox chkMakeSynthetic;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		MainGUIOld window = new MainGUIOld();
		window.initialize();
		window.frmSkidshrink.setVisible(true);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSkidshrink = new JFrame();
		frmSkidshrink.setTitle("Skidfuscator");
		frmSkidshrink.getContentPane().setBackground(SystemColor.controlHighlight);
		frmSkidshrink.setBounds(100, 100, 880, 480);
		frmSkidshrink.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel lbloptimizedJarsWill = new JLabel("Optimized jars will be placed ajacent to this program. Optimization affects file size.");
		lbloptimizedJarsWill.setFont(new Font("Tahoma", Font.ITALIC, 11));
		lbloptimizedJarsWill.setBackground(SystemColor.desktop);
		frmSkidshrink.getContentPane().add(lbloptimizedJarsWill, BorderLayout.SOUTH);
		DefaultListModel<String> model = new DefaultListModel<String>();
		model.addElement("File: Percent Shrink ");

		chkRemoveInnerOuter = new JCheckBox("Remove Inner/Outers");
		chkRemoveInnerOuter.setSelected(true);
		chkRemoveInnerOuter.setBackground(SystemColor.controlHighlight);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmSkidshrink.getContentPane().add(tabbedPane, BorderLayout.WEST);

		pnlObfuscation = new JPanel();
		pnlObfuscation.setBackground(SystemColor.controlHighlight);
		tabbedPane.addTab("Obfuscation", null, pnlObfuscation, null);
		pnlObfuscation.setLayout(new BoxLayout(pnlObfuscation, BoxLayout.Y_AXIS));

		pnlObMainOptions = new JPanel();
		pnlObMainOptions.setBackground(SystemColor.controlHighlight);
		pnlObfuscation.add(pnlObMainOptions);
		pnlObMainOptions.setLayout(new BoxLayout(pnlObMainOptions, BoxLayout.Y_AXIS));

		lblObMainOptions = new JLabel("Main Options");
		lblObMainOptions.setFont(new Font("Tahoma", Font.BOLD, 13));
		pnlObMainOptions.add(lblObMainOptions);

		chkStringObfuscation = new JCheckBox("String Obfuscation");
		chkStringObfuscation.setBackground(SystemColor.controlHighlight);
		pnlObMainOptions.add(chkStringObfuscation);

		chckbxAntidecompile = new JCheckBox("Anti-Decompile");
		chckbxAntidecompile.setBackground(SystemColor.controlHighlight);
		pnlObMainOptions.add(chckbxAntidecompile);

		chckbxFlowObfuscation = new JCheckBox("Flow Obfuscation");
		chckbxFlowObfuscation.setBackground(SystemColor.controlHighlight);
		chckbxFlowObfuscation.setEnabled(false);
		pnlObMainOptions.add(chckbxFlowObfuscation);

		pnlObStringOptions = new JPanel();
		pnlObStringOptions.setBackground(SystemColor.controlHighlight);
		pnlObfuscation.add(pnlObStringOptions);
		pnlObStringOptions.setLayout(new BoxLayout(pnlObStringOptions, BoxLayout.Y_AXIS));

		lblObStringOptions = new JLabel("String Options");
		lblObStringOptions.setFont(new Font("Tahoma", Font.BOLD, 13));
		pnlObStringOptions.add(lblObStringOptions);

		chckbxNewCheckBox = new JCheckBox("Break into array");
		chckbxNewCheckBox.setBackground(SystemColor.controlHighlight);
		pnlObStringOptions.add(chckbxNewCheckBox);

		pnlObAnti = new JPanel();
		pnlObAnti.setBackground(SystemColor.controlHighlight);
		pnlObfuscation.add(pnlObAnti);
		pnlObAnti.setLayout(new BoxLayout(pnlObAnti, BoxLayout.Y_AXIS));

		lblAntidecompileOptions = new JLabel("Anti-Decompile Options");
		pnlObAnti.add(lblAntidecompileOptions);
		lblAntidecompileOptions.setFont(new Font("Tahoma", Font.BOLD, 13));

		chkMakeSynthetic = new JCheckBox("Declare Synthetic");
		chkMakeSynthetic.setBackground(SystemColor.controlHighlight);
		pnlObAnti.add(chkMakeSynthetic);

		JPanel pnlOptimization = new JPanel();
		tabbedPane.addTab("Optimization", null, pnlOptimization, null);

		pnlOptimization.setBackground(SystemColor.controlHighlight);
		pnlOptimization.setLayout(new BoxLayout(pnlOptimization, BoxLayout.Y_AXIS));

		JPanel pnlOpMainOptions = new JPanel();
		pnlOptimization.add(pnlOpMainOptions);
		pnlOpMainOptions.setBackground(SystemColor.controlHighlight);
		pnlOpMainOptions.setBorder(null);
		pnlOpMainOptions.setLayout(new BoxLayout(pnlOpMainOptions, BoxLayout.Y_AXIS));

		lblOpMainOptions = new JLabel("Main Options");
		lblOpMainOptions.setFont(new Font("Tahoma", Font.BOLD, 13));
		pnlOpMainOptions.add(lblOpMainOptions);
		chkOptimization = new JCheckBox("Optimize");
		chkOptimization.setSelected(true);
		chkOptimization.setBackground(SystemColor.controlHighlight);
		pnlOpMainOptions.add(chkOptimization);
		chkOptimization.setToolTipText("This may be needed when 'Use Maxs' is included");
		chkUseMaxs = new JCheckBox("Compile with Maxs");
		chkUseMaxs.setBackground(SystemColor.controlHighlight);
		pnlOpMainOptions.add(chkUseMaxs);
		chkUseMaxs.setToolTipText("This may require you to start shrunk programs with '-noverify'.");

		// Class optimizations
		JPanel pnlOpClassOptions = new JPanel();
		pnlOptimization.add(pnlOpClassOptions);
		pnlOpClassOptions.setBackground(SystemColor.controlHighlight);
		pnlOpClassOptions.setLayout(new BoxLayout(pnlOpClassOptions, BoxLayout.Y_AXIS));

		lblClassOptions = new JLabel("Class Options");
		lblClassOptions.setFont(new Font("Tahoma", Font.BOLD, 13));
		pnlOpClassOptions.add(lblClassOptions);
		chkRemoveSource = new JCheckBox("Remove Source Name");
		chkRemoveSource.setSelected(true);
		chkRemoveSource.setBackground(SystemColor.controlHighlight);
		pnlOpClassOptions.add(chkRemoveSource);
		pnlOpClassOptions.add(chkRemoveSource);
		chkRemoveClassAnnotations = new JCheckBox("Remove Annotations");
		chkRemoveClassAnnotations.setSelected(false);
		chkRemoveClassAnnotations.setBackground(SystemColor.controlHighlight);
		pnlOpClassOptions.add(chkRemoveClassAnnotations);
		chkRemoveClassAttribs = new JCheckBox("Remove Attributes");
		chkRemoveClassAttribs.setSelected(true);
		chkRemoveClassAttribs.setBackground(SystemColor.controlHighlight);
		pnlOpClassOptions.add(chkRemoveClassAttribs);
		chkRemoveMethods = new JCheckBox("Remove Unused Methods");
		chkRemoveMethods.setSelected(false);
		chkRemoveMethods.setEnabled(false);
		chkRemoveMethods.setBackground(SystemColor.controlHighlight);
		pnlOpClassOptions.add(chkRemoveMethods);

		// Method optimizations
		JPanel pnlOpMethodOptions = new JPanel();
		pnlOptimization.add(pnlOpMethodOptions);
		pnlOpMethodOptions.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.setLayout(new BoxLayout(pnlOpMethodOptions, BoxLayout.Y_AXIS));

		lblMethodOptions = new JLabel("Method Options");
		lblMethodOptions.setFont(new Font("Tahoma", Font.BOLD, 13));
		pnlOpMethodOptions.add(lblMethodOptions);
		chkRemoveParameter = new JCheckBox("Remove Parameter Names");
		chkRemoveParameter.setSelected(true);
		chkRemoveParameter.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveParameter);
		chkRemoveMethodsAnnotations = new JCheckBox("Remove Annotations");
		chkRemoveMethodsAnnotations.setSelected(false);
		chkRemoveMethodsAnnotations.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveMethodsAnnotations);
		chkRemoveLocals = new JCheckBox("Remove Local Variables");
		chkRemoveLocals.setSelected(true);
		chkRemoveLocals.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveLocals);
		chkRemoveLines = new JCheckBox("Remove Line Numbers");
		chkRemoveLines.setSelected(true);
		chkRemoveLines.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveLines);
		chkRemoveMethodAttribs = new JCheckBox("Remove Attributes");
		chkRemoveMethodAttribs.setSelected(true);
		chkRemoveMethodAttribs.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveMethodAttribs);
		chkRemoveFrames = new JCheckBox("Remove Frames");
		chkRemoveFrames.setEnabled(false);
		chkRemoveFrames.setSelected(false);
		chkRemoveFrames.setBackground(SystemColor.controlHighlight);
		pnlOpMethodOptions.add(chkRemoveFrames);
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(new Color(227, 227, 227));
		mainPanel.setBorder(null);
		frmSkidshrink.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));
		TransferHandler handler = new JarDropHandler(this, 0);
		JPanel pnlDragContainer = new JPanel();
		pnlDragContainer.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pnlDragContainer.setBackground(SystemColor.scrollbar);
		mainPanel.add(pnlDragContainer, BorderLayout.CENTER);
		pnlDragContainer.setLayout(new BorderLayout(0, 0));

		splitDrag = new JSplitPane();
		splitDrag.setBackground(SystemColor.controlShadow);
		pnlDragContainer.add(splitDrag, BorderLayout.CENTER);
		JLabel lblDragAJar = new JLabel("Drag program here");
		lblDragAJar.setFont(new Font("Tahoma", Font.PLAIN, 14));
		splitDrag.setLeftComponent(lblDragAJar);
		lblDragAJar.setBackground(new Color(240, 240, 240));
		lblDragAJar.setHorizontalAlignment(SwingConstants.CENTER);
		lblDragAJar.setTransferHandler(handler);

		pnlLibraries = new JPanel();
		pnlLibraries.setBackground(SystemColor.controlShadow);
		splitDrag.setRightComponent(pnlLibraries);
		pnlLibraries.setLayout(new BorderLayout(0, 0));

		lblDragALibrary = new JLabel("Drag libraries here");
		lblDragALibrary.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblDragALibrary.setBackground(SystemColor.controlShadow);
		pnlLibraries.add(lblDragALibrary);
		lblDragALibrary.setHorizontalAlignment(SwingConstants.CENTER);

		lstLoadedLibs = new JList<String>();
		lstLoadedLibs.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		lstLoadedLibs.setBackground(SystemColor.controlHighlight);
		DefaultListModel<String> model2 = new DefaultListModel<String>();
		model2.addElement("  Loaded Libraries  ");
		lstLoadedLibs.setModel(model2);
		pnlLibraries.add(lstLoadedLibs, BorderLayout.EAST);
		splitDrag.setDividerLocation(frmSkidshrink.getWidth() / 2);
		TransferHandler handler2 = new JarDropHandler(this, 1);
		lblDragALibrary.setTransferHandler(handler2);
		splitDrag.setDividerLocation(frmSkidshrink.getWidth() / 3);
	}

	public void addLibrary(File jar, JList<String> jlist) {
		libraries.add(jar);
		DefaultListModel<String> model = (DefaultListModel<String>) jlist.getModel();
		model.addElement("  " + jar.getName() + "  ");
	}

	@Override
	public void preLoadJars(int id) {
		if (id == 0){
		optimizer.update(chkUseMaxs.isSelected(), !chkOptimization.isSelected(), chkRemoveSource.isSelected(), chkRemoveInnerOuter.isSelected(),
				chkRemoveClassAnnotations.isSelected(), chkRemoveClassAttribs.isSelected(), chkRemoveMethods.isSelected(), chkRemoveParameter.isSelected(),
				chkRemoveMethodsAnnotations.isSelected(), chkRemoveLocals.isSelected(), chkRemoveLines.isSelected(), chkRemoveFrames.isSelected(),
				chkRemoveMethodAttribs.isSelected());
		}
	}

	@Override
	public void onJarLoad(int id, File jar) {
		if (id == 1){
			addLibrary(jar, lstLoadedLibs);
			return;
		}
		// Create the setup
		LazySetupMaker lsm = LazySetupMaker.get(jar.getAbsolutePath(), true, libraries);
		// Create the output
		Map<String, byte[]> out = new HashMap<String, byte[]>();

		// Add the program to the classpath if COMPUTE_FRAMES is used.
		try {
			if (!chkUseMaxs.isSelected()) {
				//Classpather.addFile(jar);
			}
			out.putAll(JarUtils.loadNonClassEntries(jar));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Removal
		if (chkOptimization.isSelected()) {
			// Find entry-point
			String mainClass = JarUtils.getManifestMainClass(jar);
			Logger.logLow("Compting classes to ignore...");
			// Find unused classes
			// TODO: Make remover that removes un-used methods
			Remover remover = new SimpleRemover();
			remover.getUsedClasses(mainClass, lsm.getNodes());
			Set<String> keep = remover.getKeptClasses();
			List<String> allClasses = new ArrayList<String>();
			for (String name : lsm.getNodes().keySet()) {
				allClasses.add(name);
			}
			// Remove unused ClassNodes
			Logger.logLow("Removing unused classes...");
			try {
				for (String name : allClasses) {
					if (!keep.contains(name)) {
						lsm.getNodes().remove(name);
						lsm.getMappings().remove(name);
						System.out.println("X: " + name);
					}
				}
			} catch (ConcurrentModificationException e) {
				e.printStackTrace();
			}
			// Optimize existing ClassNodes
			Logger.logLow("Optimizing classes...");
			for (ClassNode cn : lsm.getNodes().values()) {
				ClassWriter cw = new ClassWriter(chkUseMaxs.isSelected() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
				optimizer.shrink(lsm.getMappings().get(cn.name), remover, new SkidRemapper(new HashMap<String, MappedClass>()), cw);
			}
		}

		// TODO: Obfuscation

		// fucking do this

		// Finally export ClassNodes to bytes
		for (ClassNode cn : lsm.getNodes().values()) {
			ClassWriter cw = new ClassWriter(chkUseMaxs.isSelected() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
			cn.accept(cw);
			out.put(cn.name, cw.toByteArray());
		}

		Logger.logLow("Saving...");
		File newJar = new File(jar.getName().substring(0, jar.getName().indexOf(".")) + "-opti.jar");
		JarUtils.saveAsJar(out, newJar.getName());

		// Poop out message of file % change
		/*
		 * long origSize = jar.length(); try { Thread.sleep(25); } catch
		 * (InterruptedException e) { e.printStackTrace(); }
		 * 
		 * long newSize = newJar.length(); double l = (0.0 + newSize) / (0.0 +
		 * origSize); int i = Integer.parseInt((l + "").substring(2, 4));
		 * System.out.print((100 - i) + "%");
		 */
	}

}
