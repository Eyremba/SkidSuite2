package me.lpk.optimization;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

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

import me.lpk.lang.Lang;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.MappingClassWriter;
import me.lpk.mapping.SkidRemapper;
import me.lpk.util.JarUtils;

/**
 * It's ugly but it's less ugly than before.
 * 
 * Still works too.
 */
public class Optimizer {
	private final Map<String, Boolean> boolOpts;

	public Optimizer(Map<String, Boolean> boolOpts) {
		this.boolOpts = boolOpts;
	}

	/**
	 * Moves most of the optimize method in here rather than putting it in the
	 * parsing method.
	 * 
	 * @param jar
	 * @param nodes
	 * @param mappings
	 */
	public void optimize(File jar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappings) {
		Logger.logLow("Beginning optimization...");
		String mainClass = JarUtils.getManifestMainClass(jar);
		Logger.logLow("Found main class: " + mainClass);
		Logger.logLow("Searching for unused classes...");
		// TODO: Make remover that removes un-used methods
		Remover remover = new SimpleRemover();
		// Make a new map that does not contain library nodes.
		Map<String, ClassNode> mapForRemoval = new HashMap<String, ClassNode>();
		for (String name : mappings.keySet()) {
			MappedClass mc = mappings.get(name);
			if (!mc.isLibrary() && nodes.containsKey(name)) {
				mapForRemoval.put(name, nodes.get(name));
			}
		}
		remover.getUsedClasses(mainClass, mapForRemoval);
		Set<String> keep = remover.getKeptClasses();
		List<String> allClasses = new ArrayList<String>();
		for (String name : nodes.keySet()) {
			allClasses.add(name);
		}
		Logger.logLow("Removing unused classes [" + (mapForRemoval.size() - keep.size()) + " marked]...");
		for (String name : allClasses) {
			if (!keep.contains(name)) {
				nodes.remove(name);
				mappings.remove(name);
			}
		}
		Logger.logLow("Optimizing remaining classes...");
		for (String name : keep) {
			try {
				MappedClass mc = mappings.get(name);
				if (mc == null) {
					continue;
				}
				MappingClassWriter cw = new MappingClassWriter(mappings, ClassWriter.COMPUTE_FRAMES);
				ClassVisitor remapper = new ClassOptimizerImpl(remover, mc, cw, new SkidRemapper(new HashMap<String, MappedClass>()));
				mc.getNode().accept(remapper);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void shrink(MappedClass cn, Remover r, Remapper m, ClassWriter cw) {
		try {
			ClassVisitor remapper = new ClassOptimizerImpl(r, cn, cw, m);
			cn.getNode().accept(remapper);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, getErr(e), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private String getErr(Exception e) {
		String s = "";
		for (StackTraceElement ste : e.getStackTrace()) {
			s += ste.toString() + "\n";
		}
		return s;
	}

	class ClassOptimizerImpl extends ClassOptimizer {
		private final Remover rem;
		private final MappedClass mapped;

		public ClassOptimizerImpl(Remover r, MappedClass mappedClass, ClassWriter cw, Remapper remapper) {
			super(cw, remapper);
			this.rem = r;
			this.mapped = mappedClass;
		}

		@Override
		public void visitSource(String source, String debug) {
			// remove debug
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_SRC, false) && cv != null) {
				cv.visitSource(source, debug);
			}
		}

		@Override
		public void visitOuterClass(final String owner, final String name, final String desc) {
			// remove debug info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_ATRIB, false) && cv != null) {
				cv.visitOuterClass(owner, name, desc);
			}
		}

		@Override
		public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
			// remove debug info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_SRC, false) && cv != null) {
				cv.visitInnerClass(name, outerName, innerName, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
			// remove annotations
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_ANNO, false) && cv != null) {
				return cv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// remove annotations
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_ANNO, false) && cv != null) {
				return cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public void visitAttribute(final Attribute attr) {
			// remove non standard attributes
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_CLASS_REMOVE_ATRIB, false) && cv != null) {
				cv.visitAttribute(attr);
			}
		}

		@Override
		public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
			// Cancelling what ClassOptimizer does
			FieldVisitor fv = super.visitField(access, remapper.mapFieldName(mapped.getNewName(), name, desc), remapper.mapDesc(desc),
					remapper.mapSignature(signature, true), remapper.mapValue(value));
			return fv == null ? null : createFieldRemapper(fv);
		}

		@Override
		public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
			// Cancelling what ClassOptimizer does
			if (rem.isMethodUsed(mapped.getNewName(), name + desc) || isOverride(name, desc)) {
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
			return null;
			// String newDesc = remapper.mapMethodDesc(desc);
			// MethodVisitor mv = super.visitMethod(access,
			// remapper.mapMethodName(className, name, desc), newDesc,
			// remapper.mapSignature(signature, false), exceptions == null ?
			// null : remapper.mapTypes(exceptions));
			// return mv == null ? null : createMethodRemapper(mv);
		}

		private boolean isOverride(String name, String desc) {
			for (MappedMember mm : mapped.getMethods()) {
				if (mm.getNewName().equals(name) && mm.getDesc().equals(desc)) {
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
			return new MethodOptimizerImpl(this, mv, remapper);
		}
	}

	class MethodOptimizerImpl extends MethodOptimizer {
		public MethodOptimizerImpl(ClassOptimizer classOptimizer, MethodVisitor mv, Remapper remapper) {
			super(classOptimizer, mv, remapper);
		}

		@Override
		public void visitParameter(String name, int access) {
			// remove parameter info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_PARAMNAME, false) && mv != null) {
				mv.visitParameter(name, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			// remove annotations
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_ANNO, false) && mv != null) {
				mv.visitAnnotationDefault();
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			// remove annotations
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_ANNO, false) && mv != null) {
				mv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_ANNO, false) && mv != null) {
				mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
			// remove annotations
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_ANNO, false) && mv != null) {
				mv.visitParameterAnnotation(parameter, desc, visible);
			}
			return null;
		}

		@Override
		public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
			// remove debug info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_LOCALDATA, false) && mv != null) {
				mv.visitLocalVariable(name, desc, signature, start, end, index);
			}
		}

		@Override
		public void visitLineNumber(final int line, final Label start) {
			// remove debug info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_LINES, false) && mv != null) {
				mv.visitLineNumber(line, start);
			}
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			// remove frame info
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_FRAMES, false) && mv != null) {
				mv.visitFrame(type, nLocal, local, nStack, stack);
			}
		}

		@Override
		public void visitAttribute(Attribute attr) {
			// remove non standard attributes
			if (!boolOpts.getOrDefault(Lang.OPTION_OPTIM_METHOD_REMOVE_ATTRIB, false) && mv != null) {
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
