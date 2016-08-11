package me.lpk.gui;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.JarUtils;

import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.awt.event.ActionEvent;

public class WindowMappingProcessor {
	private JFrame frame;
	private JTextField txtJarLoc;
	private JTextField txtMapLoc;
	private JTextPane txtLog;
	private JButton btnUndo;
	private JFileChooser chooser;
	private ActionListener currentAction;
	private File jar, map;

	public static void main(String[] args) {
		WindowMappingProcessor wmp = new WindowMappingProcessor();
		wmp.frame.setVisible(true);
	}
	
	/**
	 * Create the application.
	 */
	public WindowMappingProcessor() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Mapping Processor");
		frame.setResizable(false);
		frame.setBounds(100, 100, 481, 285);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		txtJarLoc = new JTextField();
		txtJarLoc.setBounds(140, 11, 325, 23);
		frame.getContentPane().add(txtJarLoc);
		txtJarLoc.setColumns(10);

		JButton btnLoadJar = new JButton("Load Jar");
		btnLoadJar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					jar = fc.getSelectedFile();
					txtJarLoc.setText(jar.getAbsolutePath());
					if (map != null) {
						btnUndo.setEnabled(true);
					}
				}
			}
		});
		btnLoadJar.setBounds(10, 11, 120, 23);
		frame.getContentPane().add(btnLoadJar);

		JButton btnLoadMappings = new JButton("Load Mappings");
		btnLoadMappings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					map = fc.getSelectedFile();
					txtMapLoc.setText(map.getAbsolutePath());
					if (jar != null) {
						btnUndo.setEnabled(true);
					}
				}
			}
		});
		btnLoadMappings.setBounds(10, 45, 120, 23);
		frame.getContentPane().add(btnLoadMappings);

		txtMapLoc = new JTextField();
		txtMapLoc.setColumns(10);
		txtMapLoc.setBounds(140, 45, 325, 23);
		frame.getContentPane().add(txtMapLoc);

		btnUndo = new JButton("Reverse");
		currentAction = new ProguardAction();
		btnUndo.addActionListener(currentAction);
		btnUndo.setBounds(10, 79, 120, 23);
		btnUndo.setEnabled(false);
		frame.getContentPane().add(btnUndo);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setBounds(10, 116, 455, 131);
		frame.getContentPane().add(scrollPane);
		txtLog = new JTextPane();
		scrollPane.setViewportView(txtLog);

		JComboBox<String> combo = new JComboBox<String>(new String[] { "Proguard", "Enigma", "SRG" });
		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = combo.getSelectedItem().toString();
				btnUndo.removeActionListener(currentAction);
				if (s.equals("Proguard")) {
					currentAction = new ProguardAction();
					log("Switching to Proguard mapping processor.");
				} else if (s.equals("Enigma")) {
					currentAction = new EnigmaAction();
					log("Switching to Enigma mapping processor.");
				} else if (s.equals("SRG")) {
					currentAction = new SRGAction();
					log("Switching to SRG mapping processor.");
				}
				btnUndo.addActionListener(currentAction);
			}
		});

		combo.setBounds(140, 79, 120, 23);
		frame.getContentPane().add(combo);
	}

	private void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses, String name) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses, true);
		try {
			out.putAll(JarUtils.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		JarUtils.saveAsJar(out, name);
	}

	private void log(String s) {
		txtLog.setText(txtLog.getText() + "\n" + s);
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

	class EnigmaAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Map<String, ClassNode> nodes = JarUtils.loadClasses(jar);
				log("Loaded nodes from jar: " + jar.getAbsolutePath());
				Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromEnigma(map, nodes);
				log("Loaded mappings from engima mappings: " + map.getAbsolutePath());
				saveJar(jar, nodes, mappedClasses, jar.getName() + "-re.jar");
				log("Saved modified file!");

			} catch (IOException e1) {
				log(e1.getMessage());
			}
		}
	}

	class SRGAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Map<String, ClassNode> nodes = JarUtils.loadClasses(jar);
				log("Loaded nodes from jar: " + jar.getAbsolutePath());
				Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromSRG(map, nodes);
				log("Loaded mappings from engima mappings: " + map.getAbsolutePath());
				saveJar(jar, nodes, mappedClasses, jar.getName() + "-re.jar");
				log("Saved modified file!");

			} catch (IOException e1) {
				log(e1.getMessage());
			}
		}
	}

	class ProguardAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Map<String, ClassNode> nodes = JarUtils.loadClasses(jar);
				log("Loaded nodes from jar: " + jar.getAbsolutePath());
				Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromProguard(map, nodes);
				log("Loaded mappings from proguard mappings: " + map.getAbsolutePath());
				saveJar(jar, nodes, mappedClasses, jar.getName() + "-re.jar");
				log("Saved modified file!");

			} catch (IOException e1) {
				log(e1.getMessage());
			}
		}
	}
}