package me.lpk.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.loaders.*;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionEvent;

public class WindowMappingConverter {
	private JFrame frmMappingConverter;
	private JButton btnConvert;
	private MappingLoader loader;
	private JTextPane txtOutput;
	private JFileChooser chooser;
	private File map;
	private Map<String, MappedClass> mappings = new HashMap<String, MappedClass>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowMappingConverter window = new WindowMappingConverter();
					window.frmMappingConverter.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WindowMappingConverter() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmMappingConverter = new JFrame();
		frmMappingConverter.setTitle("Mapping Converter");
		frmMappingConverter.setBounds(100, 100, 450, 300);
		frmMappingConverter.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel pnlButtons = new JPanel();
		pnlButtons.setPreferredSize(new Dimension(100, 400));
		frmMappingConverter.getContentPane().add(pnlButtons, BorderLayout.WEST);
		pnlButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JButton btnLoadMap = new JButton("Load Map");
		btnLoadMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int val = getFileChooser().showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					map = chooser.getSelectedFile();
					load();
				}
				btnConvert.setEnabled(true);
			}

		});
		pnlButtons.add(btnLoadMap);

		btnConvert = new JButton("  Convert  ");
		btnConvert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				convert();
			}

		});
		btnConvert.setEnabled(false);
		pnlButtons.add(btnConvert);

		JPanel pnlMappings = new JPanel();
		frmMappingConverter.getContentPane().add(pnlMappings, BorderLayout.CENTER);
		pnlMappings.setLayout(new BorderLayout(0, 0));

		JComboBox<String> cmboMappingTypes = new JComboBox<String>(new String[] { "Proguard", "Enigma", "SRG" });
		cmboMappingTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = cmboMappingTypes.getSelectedItem().toString();
				if (s.equals("Proguard")) {
					loader = new ProguardLoader();
				} else if (s.equals("Enigma")) {
					loader = new EnigmaLoader();
				} else if (s.equals("SRG")) {
					loader = new SRGLoader();
				}
			}
		});
		pnlMappings.add(cmboMappingTypes, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane();
		pnlMappings.add(scrollPane, BorderLayout.CENTER);

		txtOutput = new JTextPane();
		scrollPane.setViewportView(txtOutput);
	}

	private void convert() {
		txtOutput.setText(txtOutput.getText() + "Converting '" + map.getName() + "' via: " + loader.getClass().getSimpleName() + "\n");
		loader.save(mappings, new File(map.getName() + "-re.map"));
		txtOutput.setText(txtOutput.getText() + "Finished!" + "\n");
	}

	private void load() {
		txtOutput.setText(txtOutput.getText() + "Loading from: '" + map.getName() + "'\n");
		mappings.clear();
		try (InputStream fis = new FileInputStream(map);
				InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
				BufferedReader br = new BufferedReader(isr);) {
			br.mark(0);
			String line = br.readLine();
			br.reset();
			if (line.startsWith("CLASS ")) {
				txtOutput.setText(txtOutput.getText() + "Detected Enigma format.\n");
				mappings.putAll(new EnigmaLoader().read(br));
			} else if (line.startsWith("PK:") || line.startsWith("CL:")) {
				txtOutput.setText(txtOutput.getText() + "Detected SRG format.\n");
				mappings.putAll(new SRGLoader().read(br));
			} else if (line.contains(" -> ")) {
				txtOutput.setText(txtOutput.getText() + "Detected Proguard format.\n");
				mappings.putAll(new ProguardLoader().read(br));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
