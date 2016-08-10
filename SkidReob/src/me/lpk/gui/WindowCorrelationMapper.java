package me.lpk.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.CorrelationMapper;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.impl.ModeNone;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;

public class WindowCorrelationMapper {
	private JFileChooser chooser;
	private JFrame frmCorrelationMapper;
	private JTextField txtTargetJar;
	private JTextField txtCleanJar;
	private JTextArea txtrCleanNames;
	private JTextArea txtrTargetNames;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowCorrelationMapper window = new WindowCorrelationMapper();
					window.frmCorrelationMapper.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WindowCorrelationMapper() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmCorrelationMapper = new JFrame();
		frmCorrelationMapper.setResizable(false);
		frmCorrelationMapper.setTitle("Correlation Mapper");
		frmCorrelationMapper.setBounds(100, 100, 787, 450);
		frmCorrelationMapper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCorrelationMapper.getContentPane().setLayout(null);
		JButton btnLoadTarget = new JButton("Load Target");
		JButton btnLoadClean = new JButton("Load Clean");
		JButton btnCorrelate = new JButton("Correlate");
		JSplitPane splitPane = new JSplitPane();
		txtrCleanNames = new JTextArea();
		txtrTargetNames = new JTextArea();
		txtCleanJar = new JTextField();
		txtTargetJar = new JTextField();
		txtCleanJar.setBounds(122, 45, 649, 20);
		txtTargetJar.setBounds(122, 11, 649, 20);
		btnCorrelate.setBounds(10, 75, 102, 23);
		btnLoadTarget.setBounds(10, 10, 102, 23);
		btnLoadClean.setBounds(10, 44, 102, 23);
		splitPane.setBounds(10, 109, 761, 301);

		btnLoadTarget.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					File jar = fc.getSelectedFile();
					txtTargetJar.setText(jar.getAbsolutePath());
				}
			}
		});
		btnLoadClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					File jar = fc.getSelectedFile();
					txtCleanJar.setText(jar.getAbsolutePath());
				}
			}
		});
		btnCorrelate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				go(txtTargetJar.getText(), txtCleanJar.getText());
			}
		});
		txtCleanJar.setColumns(10);
		txtTargetJar.setColumns(10);
		splitPane.setDividerLocation(frmCorrelationMapper.getWidth() / 2);
		splitPane.setLeftComponent(txtrCleanNames);
		splitPane.setRightComponent(txtrTargetNames);
		txtrCleanNames.setText("CleanNames");
		txtrTargetNames.setText("TargetNames");
		frmCorrelationMapper.getContentPane().add(btnLoadTarget);
		frmCorrelationMapper.getContentPane().add(btnLoadClean);
		frmCorrelationMapper.getContentPane().add(txtTargetJar);
		frmCorrelationMapper.getContentPane().add(txtCleanJar);
		frmCorrelationMapper.getContentPane().add(btnCorrelate);
		frmCorrelationMapper.getContentPane().add(splitPane);
	}

	public void go(String pathTarget, String pathClean) {
		// Loading
		File targetJar = new File(pathTarget);
		File cleanJar = new File(pathClean);
		LazySetupMaker targ = LazySetupMaker.get(targetJar.getAbsolutePath(), false);
		LazySetupMaker clen = LazySetupMaker.get(cleanJar.getAbsolutePath(), false);
		//Classpather.addFile(targetJar);

		System.out.println("Loading classes...");
		Map<String, ClassNode> targetNodes = targ.getNodes();
		Map<String, ClassNode> baseNodes = clen.getNodes();
		// Making maps
		System.out.println("Generating mappings");
		Map<String, MappedClass> targetMappings = MappingGen.mappingsFromNodes(targetNodes);
		for (MappedClass mappedClass : targetMappings.values()) {
			targetMappings = MappingGen.linkMappings(mappedClass, targetMappings);
		}
		Map<String, MappedClass> cleanMappings = MappingGen.mappingsFromNodes(baseNodes);
		// Linking
		System.out.println("Linking correlating sources...");
		targetMappings = resetRemapped(targetMappings);
		correlate(targetMappings, cleanMappings);
		// Filling in the gaps
		System.out.println("Filling in missing classes...");
		targetMappings = CorrelationMapper.fillInTheGaps(targetMappings, new ModeNone());
		// Processing
		System.out.println("Processing output jar...");
		saveJar(targetJar, targetNodes, targetMappings);
		saveMappings(targetMappings, targ.getName() + ".enigma.map");
		System.out.println("Done!");
	}

	private void correlate(Map<String, MappedClass> mappedClasses, Map<String, MappedClass> baseClasses) {
		HashMap<String, String> h = new HashMap<String, String>();
		String[] clean = txtrCleanNames.getText().split("\n");
		String[] target = txtrTargetNames.getText().split("\n");
		for (int i = 0; i < Math.min(clean.length, target.length); i++) {
			h.put(target[i], clean[i]);
		}
		for (String obfu : h.keySet()) {
			MappedClass targetClass = mappedClasses.get(obfu);
			MappedClass cleanClass = baseClasses.get(h.get(obfu));
			if (targetClass == null) {
				System.err.println("NULL 1: " + obfu + ":" + h.get(obfu));

				if (cleanClass == null) {
					System.err.println("NULL 2: " + obfu + ":" + h.get(obfu));
					continue;
				}
			}
			mappedClasses = CorrelationMapper.correlate(targetClass, cleanClass, mappedClasses, baseClasses);
		}
	}

	private static void saveMappings(Map<String, MappedClass> mappedClasses, String string) {
		StringBuilder sb = new StringBuilder();
		for (MappedClass clazz : mappedClasses.values()) {
			sb.append("CLASS " + clazz.getOriginalName() + " " + clazz.getNewName() + "\n");
			for (MappedMember f : clazz.getFields()) {
				sb.append("\tFIELD " + f.getOriginalName() + " " + f.getNewName() + " " + f.getDesc() + "\n");
			}
			for (MappedMember m : clazz.getMethods()) {
				if (m.getOriginalName().contains("<")) {
					continue;
				}
				sb.append("\tMETHOD " + m.getOriginalName() + " " + m.getNewName() + " " + m.getDesc() + "\n");
			}
		}
		try {
			FileUtils.write(new File(string), sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, MappedClass> resetRemapped(Map<String, MappedClass> mappedClasses) {
		for (String name : mappedClasses.keySet()) {
			MappedClass mc = mappedClasses.get(name);
			mc.setRenamedOverride(false);
			mappedClasses.put(name, mc);
		}
		return mappedClasses;
	}

	private static void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses, true);
		try {
			out.putAll(JarUtils.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int renamed = 0;
		for (MappedClass mc : mappedClasses.values()) {
			if (mc.isTruelyRenamed()) {
				renamed++;
			}
		}
		System.out.println("Saving...  [Ranemed " + renamed + " classes]");
		JarUtils.saveAsJar(out, nonEntriesJar.getName() + "_correlated.jar");
	}

	private JFileChooser getFileChooser() {
		if (chooser == null) {
			chooser = new JFileChooser();
			final String dir = System.getProperty("user.dir");
			final File fileDir = new File(dir);
			chooser.setCurrentDirectory(fileDir);
		}
		return chooser;
	}
}
