package me.lpk;

import java.awt.EventQueue;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.optimizer.ClassOptimizer;
import org.objectweb.asm.optimizer.MethodOptimizer;
import org.objectweb.asm.tree.ClassNode;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.SkidRemapper;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

public class Shrinker {
	private JList<String> lstFileSizes;
	private JCheckBox chkMaxs, chkNoRemoval;
	private JCheckBox chkSource, chkInnerOuter, chkClassAnnotations, chkClassAttribs, chkRemMethods;
	private JCheckBox chkParameter, chkMethodsAnnotations, chkLocals, chkLines, chkFrames, chkMethodAttribs;
	private JFrame frmSkidshrink;
	private JPanel pnlOptions;
	private JPanel pnlEmb1;
	private JPanel pnlEmb2;
	private JLabel lblMainOptions;
	private JLabel lblClassOptions;
	private JLabel lblMethodOptions;
	private JPanel pnlContainer;
	private JEditorPane dtrpnhello;
	private JScrollPane scrollPane;
	private JLabel lblLoadLibrary;
	private JSplitPane splitPane;
	private JPanel panel;
	private JList<String> lstLoadedLibs;
	private Set<File> libraries = new HashSet<File>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Shrinker window = new Shrinker();
		window.initialize();
		window.frmSkidshrink.setVisible(true);
	}

	private void addLibrary(File jar) {
		libraries.add(jar);
		DefaultListModel<String> model = (DefaultListModel<String>) lstLoadedLibs.getModel();
		model.addElement("  " + jar.getName() + "  ");
	}

	private void shrink(File jar) {
		long origSize = jar.length();
		LazySetupMaker.clearExtraLibraries();
		for (File lib : libraries) {
			LazySetupMaker.addExtraLibraryJar(lib);
		}
		LazySetupMaker lsm = LazySetupMaker.get(jar.getAbsolutePath(), false, true);
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		try {
			out.putAll(JarUtils.loadNonClassEntries(jar));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String mainClass = JarUtils.getManifestMainClass(jar);
		boolean isLibrary = chkNoRemoval.isSelected() || mainClass == null;
		boolean remFuncs = chkRemMethods.isSelected();
		Logger.logLow("Compting classes to ignore... (Processing time scales exponentially with file size)");
		Remover r = new Remover();
		Remover r2 = new Remover();
		Set<String> keep = isLibrary ? null : r.evaluate_old(mainClass, lsm.getNodes());
		Set<String> keep2 = isLibrary ? null : r2.evaluate_new(mainClass, lsm.getNodes());
		isLibrary = isLibrary || keep.size() == 0;
		Logger.logLow("Optimizing classes...");
		if (!chkMaxs.isSelected()) {
			try {
				Classpather.addFile(jar);
			} catch (IOException e2) {
				JOptionPane.showMessageDialog(null, "Failed adding input to classpath. Enable 'Use Maxs' to force shrinking.", "Error: " + e2.getClass().getSimpleName(),
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		Remapper m = new SkidRemapper(new HashMap<String, MappedClass>());
		for (ClassNode cn : lsm.getNodes().values()) {
			try {
				if (isLibrary || (keep.contains(cn.name) || keep2.contains(cn.name))) {
					ClassWriter cw = new ClassWriter(chkMaxs.isSelected() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
					ClassVisitor remapper = new MyClassOptimizer(remFuncs ? r2 : r, lsm.getMappings().get(cn.name), cw, m);
					cn.accept(remapper);
					out.put(cn.name, cw.toByteArray());
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Please export with 'Extract required libraries into Jar' and try again.\nOr click the checkbox 'Use Maxs'.",
						"Error: " + e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		Logger.logLow("Saving...");
		File newJar = new File(jar.getName().substring(0, jar.getName().indexOf(".")) + "-opti.jar");
		JarUtils.saveAsJar(out, newJar.getName());
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long newSize = newJar.length();
		DefaultListModel<String> model = (DefaultListModel<String>) lstFileSizes.getModel();
		double l = (0.0 + newSize) / (0.0 + origSize);
		int i = Integer.parseInt((l + "").substring(2, 4));
		String x = (100 - i) + "%";
		model.addElement(jar.getName() + ": " + x);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSkidshrink = new JFrame();
		frmSkidshrink.setTitle("SkidShrink");
		frmSkidshrink.getContentPane().setBackground(SystemColor.controlHighlight);
		frmSkidshrink.setBounds(100, 100, 880, 480);
		frmSkidshrink.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel lbloptimizedJarsWill = new JLabel(
				"*Optimized jars will be placed ajacent to this program. Optimization only affects file size, not performance. Read tooltips for information (hover over checks).");
		lbloptimizedJarsWill.setFont(new Font("Tahoma", Font.ITALIC, 11));
		lbloptimizedJarsWill.setBackground(SystemColor.desktop);
		frmSkidshrink.getContentPane().add(lbloptimizedJarsWill, BorderLayout.SOUTH);
		JPanel panelx = new JPanel();
		panelx.setBackground(SystemColor.controlHighlight);
		panelx.setLayout(new BorderLayout());
		DefaultListModel<String> model = new DefaultListModel<String>();
		model.addElement("File: Percent Shrink ");
		frmSkidshrink.getContentPane().add(panelx, BorderLayout.NORTH);

		pnlContainer = new JPanel();
		pnlContainer.setBackground(SystemColor.controlHighlight);
		panelx.add(pnlContainer, BorderLayout.CENTER);
		pnlContainer.setLayout(new BorderLayout(0, 0));

		pnlOptions = new JPanel();
		pnlContainer.add(pnlOptions, BorderLayout.EAST);
		pnlOptions.setLayout(new BorderLayout(0, 0));

		// Method optimizations
		JPanel pnlMethodOptions = new JPanel();
		pnlOptions.add(pnlMethodOptions, BorderLayout.EAST);
		pnlMethodOptions.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.setLayout(new BoxLayout(pnlMethodOptions, BoxLayout.Y_AXIS));

		lblMethodOptions = new JLabel("Method Options");
		lblMethodOptions.setFont(new Font("Tahoma", Font.BOLD, 11));
		pnlMethodOptions.add(lblMethodOptions);
		chkParameter = new JCheckBox("Remove Parameter Names");
		chkParameter.setSelected(true);
		chkParameter.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkParameter);
		chkMethodsAnnotations = new JCheckBox("Remove Annotations");
		chkMethodsAnnotations.setSelected(false);
		chkMethodsAnnotations.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkMethodsAnnotations);
		chkLocals = new JCheckBox("Remove Local Variables");
		chkLocals.setSelected(true);
		chkLocals.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkLocals);
		chkLines = new JCheckBox("Remove Line Numbers");
		chkLines.setSelected(true);
		chkLines.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkLines);
		chkFrames = new JCheckBox("Remove Frames");
		chkFrames.setSelected(false);
		chkFrames.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkFrames);
		chkMethodAttribs = new JCheckBox("Remove Attributes");
		chkMethodAttribs.setSelected(true);
		chkMethodAttribs.setBackground(SystemColor.controlHighlight);
		pnlMethodOptions.add(chkMethodAttribs);

		pnlEmb1 = new JPanel();
		pnlOptions.add(pnlEmb1, BorderLayout.CENTER);
		pnlEmb1.setLayout(new BorderLayout(0, 0));

		// Class optimizations
		JPanel pnlClassOptions = new JPanel();
		pnlEmb1.add(pnlClassOptions, BorderLayout.EAST);
		pnlClassOptions.setBackground(SystemColor.controlHighlight);
		pnlClassOptions.setLayout(new BoxLayout(pnlClassOptions, BoxLayout.Y_AXIS));

		lblClassOptions = new JLabel("Class Options");
		lblClassOptions.setFont(new Font("Tahoma", Font.BOLD, 11));
		pnlClassOptions.add(lblClassOptions);
		chkSource = new JCheckBox("Remove Source Name");
		chkSource.setSelected(true);
		chkSource.setBackground(SystemColor.controlHighlight);
		pnlClassOptions.add(chkSource);
		pnlClassOptions.add(chkSource);
		chkClassAnnotations = new JCheckBox("Remove Annotations");
		chkClassAnnotations.setSelected(false);
		chkClassAnnotations.setBackground(SystemColor.controlHighlight);
		pnlClassOptions.add(chkClassAnnotations);
		chkClassAttribs = new JCheckBox("Remove Attributes");
		chkClassAttribs.setSelected(true);
		chkClassAttribs.setBackground(SystemColor.controlHighlight);
		pnlClassOptions.add(chkClassAttribs);
		chkRemMethods = new JCheckBox("Remove Unused Methods");
		chkRemMethods.setSelected(true);
		chkRemMethods.setBackground(SystemColor.controlHighlight);
		pnlClassOptions.add(chkRemMethods);
		pnlEmb2 = new JPanel();
		pnlEmb2.setBackground(SystemColor.controlHighlight);
		pnlEmb1.add(pnlEmb2, BorderLayout.CENTER);
		pnlEmb2.setLayout(new BorderLayout(0, 0));

		// Main options
		JPanel pnlMainOptions = new JPanel();
		pnlEmb2.add(pnlMainOptions, BorderLayout.EAST);
		pnlMainOptions.setBackground(SystemColor.controlHighlight);
		pnlMainOptions.setBorder(null);
		pnlMainOptions.setLayout(new BoxLayout(pnlMainOptions, BoxLayout.Y_AXIS));

		lblMainOptions = new JLabel("Main Options");
		lblMainOptions.setFont(new Font("Tahoma", Font.BOLD, 11));
		pnlMainOptions.add(lblMainOptions);
		chkNoRemoval = new JCheckBox("No Removal");
		chkNoRemoval.setBackground(SystemColor.controlHighlight);
		pnlMainOptions.add(chkNoRemoval);
		chkNoRemoval.setToolTipText("This may be needed when 'Use Maxs' is included");
		chkMaxs = new JCheckBox("Use Maxs");
		chkMaxs.setBackground(SystemColor.controlHighlight);
		pnlMainOptions.add(chkMaxs);
		chkMaxs.setToolTipText("This may require you to start shrunk programs with '-noverify'.");

		scrollPane = new JScrollPane();
		pnlContainer.add(scrollPane, BorderLayout.CENTER);

		dtrpnhello = new JEditorPane();
		scrollPane.setViewportView(dtrpnhello);
		dtrpnhello.setContentType("text/html");
		dtrpnhello.setEditable(false);
		pnlContainer.setPreferredSize(new Dimension(1000, 200));
		pnlContainer.setMaximumSize(new Dimension(1000, 200));

		// New thread because caret position stays at the top this way
		new Thread() {
			@Override
			public void run() {
				try {
					sleep(25);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Credits for the first paragraph's content:
				// http://stackoverflow.com/questions/29332094/asm-5-when-initializing-a-classwriter-what-is-the-difference-between-compute-m
				dtrpnhello.setText(
						"<html>\r\n<body style=\"font: Arial\">\r\n<h2>Information</h2>\r\n\r\n\r\n<h3 id=\"maxs\">Maxs vs Frames</h3>\r\n<p>In recent versions of the JVM, classes contain a stack map along with the method code. This map describes the layout of the stack at key points <i>(jump targets)</i> during the method's execution. By computing with Maxs the existing frames are not touched and only the local variable and stack sizes are calculated and updated. However if the method's bytecode is modified and the stack frames aren't updated problems will occur unless the JVM is started without the verifer running<i>(-noverify)</i>. Computing the frames extends computing maxs by also computing the frames of any modified bytecode. However this process requires that all referenced classes are on the classpath and are accessible via reflection.</p>\r\n<p><b>Take-away</b>: If any code you use <i>(Libraries included)</i> is not loaded when shrinking, computing by frames will fail.</p>\r\n<hr>\r\n<h3 id=\"sourcefile\">Source File</h3>\r\n<p>Compiled classes remember the name of the java file they were compiled from. This extra data isn't required though and can be removed.</p>\r\n<hr>\r\n<h3 id=\"lines\">Line Number Table</h3>\r\n<p>Much like the source file attribute, compiled java bytecode remembers what operands were on which line. This extra data can be removed.</p>\r\n<hr>\r\n<h3 id=\"anno\">Annotations</h3>\r\n<p>Annotations for the most part do not directly affect the operation of code. They are mainly a form of metadata. However this is not true for all annotations such as an annotation-driven event system or annotations that change serialization.</p>\r\n<hr>\r\n<h3 id=\"attr\">Attributes</h3>\r\n<p>Additional attribytes that are not the <i>Line Number Table, Source File, etc.</i>.</p>\r\n<hr>\r\n<h3 id=\"varnames\">Variable Names</h3>\r\n<p>Variables are stored in by indecies in a table. For legibility names are given to these indecies. However these names are completely optional once the class is compiled and can be removed. Most decompilers will automatically rename variables that have no name in the table.</p>\r\n<hr>\r\n</body>\r\n</html>");
			}
		}.start();
		dtrpnhello.setBackground(SystemColor.controlHighlight);
		chkInnerOuter = new JCheckBox("Remove Inner/Outers");
		chkInnerOuter.setSelected(true);
		chkInnerOuter.setBackground(SystemColor.controlHighlight);

		// Main display
		lstFileSizes = new JList<String>();
		lstFileSizes.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelx.add(lstFileSizes, BorderLayout.EAST);
		lstFileSizes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lstFileSizes.setBackground(SystemColor.controlHighlight);
		lstFileSizes.setModel(model);
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(new Color(227, 227, 227));
		mainPanel.setBorder(null);
		frmSkidshrink.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));
		TransferHandler handler = new TransferHandler() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport info) {
				return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean importData(TransferHandler.TransferSupport info) {
				if (!info.isDrop())
					return false;
				Transferable t = info.getTransferable();
				List<File> data = null;
				try {
					data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				} catch (Exception e) {
					return false;
				}
				for (File jar : data)
					if (jar.getName().toLowerCase().endsWith(".jar"))
						shrink(jar);
				return true;
			}
		};
		JPanel mainSubpanel = new JPanel();
		mainSubpanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		mainSubpanel.setBackground(SystemColor.scrollbar);
		mainPanel.add(mainSubpanel, BorderLayout.CENTER);
		mainSubpanel.setLayout(new BorderLayout(0, 0));

		splitPane = new JSplitPane();
		splitPane.setBackground(SystemColor.controlShadow);
		mainSubpanel.add(splitPane, BorderLayout.CENTER);
		JLabel lblDragAJar = new JLabel("Drag a jar here for optimization");
		lblDragAJar.setFont(new Font("Tahoma", Font.PLAIN, 14));
		splitPane.setLeftComponent(lblDragAJar);
		lblDragAJar.setBackground(new Color(240, 240, 240));
		lblDragAJar.setHorizontalAlignment(SwingConstants.CENTER);
		lblDragAJar.setTransferHandler(handler);

		panel = new JPanel();
		panel.setBackground(SystemColor.controlShadow);
		splitPane.setRightComponent(panel);
		panel.setLayout(new BorderLayout(0, 0));

		lblLoadLibrary = new JLabel("Load library");
		lblLoadLibrary.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblLoadLibrary.setBackground(SystemColor.controlShadow);
		panel.add(lblLoadLibrary);
		lblLoadLibrary.setHorizontalAlignment(SwingConstants.CENTER);

		lstLoadedLibs = new JList<String>();
		lstLoadedLibs.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		lstLoadedLibs.setBackground(SystemColor.controlHighlight);
		DefaultListModel<String> model2 = new DefaultListModel<String>();
		model2.addElement("  Loaded Libraries  ");
		lstLoadedLibs.setModel(model2);
		panel.add(lstLoadedLibs, BorderLayout.EAST);
		splitPane.setDividerLocation(frmSkidshrink.getWidth() / 2);
		TransferHandler handler2 = new TransferHandler() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport info) {
				return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean importData(TransferHandler.TransferSupport info) {
				if (!info.isDrop())
					return false;
				Transferable t = info.getTransferable();
				List<File> data = null;
				try {
					data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				} catch (Exception e) {
					return false;
				}
				for (File jar : data)
					if (jar.getName().toLowerCase().endsWith(".jar"))
						addLibrary(jar);
				return true;
			}
		};
		lblLoadLibrary.setTransferHandler(handler2);

	}

	class MyClassOptimizer extends ClassOptimizer {
		private final Remover r;
		private final MappedClass mapped;

		public MyClassOptimizer(Remover r, MappedClass mappedClass, ClassVisitor cv, Remapper remapper) {
			super(cv, remapper);
			this.r = r;
			this.mapped = mappedClass;
		}

		@Override
		public void visitSource(final String source, final String debug) {
			// remove debug info
			if (!chkSource.isSelected() && cv != null) {
				cv.visitSource(source, debug);
			}
		}

		@Override
		public void visitOuterClass(final String owner, final String name, final String desc) {
			// remove debug info
			if (!chkInnerOuter.isSelected() && cv != null) {
				cv.visitOuterClass(owner, name, desc);
			}
		}

		@Override
		public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
			// remove debug info
			if (!chkInnerOuter.isSelected() && cv != null) {
				cv.visitInnerClass(name, outerName, innerName, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
			// remove annotations
			if (!chkClassAnnotations.isSelected() && cv != null) {
				return cv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// remove annotations
			if (!chkClassAnnotations.isSelected() && cv != null) {
				return cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public void visitAttribute(final Attribute attr) {
			// remove non standard attributes
			if (!chkClassAttribs.isSelected() && cv != null) {
				cv.visitAttribute(attr);
			}
		}

		@Override
		public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
			// Cancelling what ClassOptimizer does
			FieldVisitor fv = super.visitField(access, remapper.mapFieldName(mapped.getNewName(), name, desc), remapper.mapDesc(desc), remapper.mapSignature(signature, true),
					remapper.mapValue(value));
			return fv == null ? null : createFieldRemapper(fv);
		}

		@Override
		public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
			// Cancelling what ClassOptimizer does
			if (r.isMethodUsed(mapped.getNewName(), name + desc) || isOverride(name, desc)) {
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
			System.out.println("PRAISE KEK: " + mapped.getNewName() + ":" + name + desc);
			return null;
			// String newDesc = remapper.mapMethodDesc(desc);
			// MethodVisitor mv = super.visitMethod(access,
			// remapper.mapMethodName(className, name, desc), newDesc,
			// remapper.mapSignature(signature, false), exceptions == null ?
			// null : remapper.mapTypes(exceptions));
			// return mv == null ? null : createMethodRemapper(mv);
		}

		private boolean isOverride(String name, String desc) {
			for (MappedMember mm : mapped.getMethods()){
				if (mm.getNewName().equals(name) && mm.getDesc().equals(desc)){
					return mm.doesOverride();
				}
			}
			return false;
		}

		@Override
		public void visitEnd() {
			// Cancelling what ClassOptimizer does
			super.visitEnd();
		}

		@Override
		protected MethodVisitor createMethodRemapper(MethodVisitor mv) {
			return new MyMethodOptimizer(this, mv, remapper);
		}
	}

	class MyMethodOptimizer extends MethodOptimizer {
		public MyMethodOptimizer(ClassOptimizer classOptimizer, MethodVisitor mv, Remapper remapper) {
			super(classOptimizer, mv, remapper);
		}

		@Override
		public void visitParameter(String name, int access) {
			// remove parameter info
			if (!chkParameter.isSelected() && mv != null) {
				mv.visitParameter(name, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			// remove annotations
			if (!chkMethodsAnnotations.isSelected() && mv != null) {
				mv.visitAnnotationDefault();
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			// remove annotations
			if (!chkMethodsAnnotations.isSelected() && mv != null) {
				mv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			if (!chkMethodsAnnotations.isSelected() && mv != null) {
				mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
			// remove annotations
			if (!chkMethodsAnnotations.isSelected() && mv != null) {
				mv.visitParameterAnnotation(parameter, desc, visible);
			}
			return null;
		}

		@Override
		public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
			// remove debug info
			if (!chkLocals.isSelected() && mv != null) {
				mv.visitLocalVariable(name, desc, signature, start, end, index);
			}
		}

		@Override
		public void visitLineNumber(final int line, final Label start) {
			// remove debug info
			if (!chkLines.isSelected() && mv != null) {
				mv.visitLineNumber(line, start);
			}
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			// remove frame info
			if (!chkFrames.isSelected() && mv != null) {
				mv.visitFrame(type, nLocal, local, nStack, stack);
			}
		}

		@Override
		public void visitAttribute(Attribute attr) {
			// remove non standard attributes
			if (!chkMethodAttribs.isSelected() && mv != null) {
				mv.visitAttribute(attr);
			}
		}

		@Override
		public void visitLdcInsn(Object cst) {
			// Cancelling what MethodOptimizer does
			if (mv != null) {
				mv.visitLdcInsn(remapper.mapValue(cst));
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			// Cancelling what MethodOptimizer does
			if (mv != null) {
				mv.visitMethodInsn(opcode, remapper.mapType(owner), remapper.mapMethodName(owner, name, desc), remapper.mapMethodDesc(desc), itf);
			}
		}
	}
}
