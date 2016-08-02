package me.lpk.optimization;

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
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;

public class Optimizer {

	private boolean removeSource, removeInnerOuters, removeClassAnnos, removeClassAttribs;
	private boolean removeParameterData, removeMethodAnnos, removeLocalData, removeLines, removeFrames, removeMethodAttribs;

	public void shrink(MappedClass cn, Remover r, Remapper m, ClassWriter cw) {
		try {
			ClassVisitor remapper = new ClassOptimizerImpl(r, cn, cw, m);
			cn.getNode().accept(remapper);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Please export with 'Extract required libraries into Jar' and try again.\nOr click the checkbox 'Use Maxs'.",
					"Error: " + e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
		}
	}

	class ClassOptimizerImpl extends ClassOptimizer {
		private final Remover r;
		private final MappedClass mapped;

		public ClassOptimizerImpl(Remover r, MappedClass mappedClass, ClassWriter cw, Remapper remapper) {
			super(cw, remapper);
			this.r = r;
			this.mapped = mappedClass;
		}

		@Override
		public void visitSource(final String source, final String debug) {
			// remove debug info
			if (!removeSource && cv != null) {
				cv.visitSource(source, debug);
			}
		}

		@Override
		public void visitOuterClass(final String owner, final String name, final String desc) {
			// remove debug info
			if (!removeInnerOuters && cv != null) {
				cv.visitOuterClass(owner, name, desc);
			}
		}

		@Override
		public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
			// remove debug info
			if (!removeInnerOuters && cv != null) {
				cv.visitInnerClass(name, outerName, innerName, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
			// remove annotations
			if (!removeClassAnnos && cv != null) {
				return cv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			// remove annotations
			if (!removeClassAnnos && cv != null) {
				return cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public void visitAttribute(final Attribute attr) {
			// remove non standard attributes
			if (!removeClassAttribs && cv != null) {
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
			if (r.isMethodUsed(mapped.getNewName(), name + desc) || isOverride(name, desc)) {
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
			if (!removeParameterData && mv != null) {
				mv.visitParameter(name, access);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			// remove annotations
			if (!removeMethodAnnos && mv != null) {
				mv.visitAnnotationDefault();
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			// remove annotations
			if (!removeMethodAnnos && mv != null) {
				mv.visitAnnotation(desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			if (!removeMethodAnnos && mv != null) {
				mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
			// remove annotations
			if (!removeMethodAnnos && mv != null) {
				mv.visitParameterAnnotation(parameter, desc, visible);
			}
			return null;
		}

		@Override
		public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
			// remove debug info
			if (!removeLocalData && mv != null) {
				mv.visitLocalVariable(name, desc, signature, start, end, index);
			}
		}

		@Override
		public void visitLineNumber(final int line, final Label start) {
			// remove debug info
			if (!removeLines && mv != null) {
				mv.visitLineNumber(line, start);
			}
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			// remove frame info
			if (!removeFrames && mv != null) {
				mv.visitFrame(type, nLocal, local, nStack, stack);
			}
		}

		@Override
		public void visitAttribute(Attribute attr) {
			// remove non standard attributes
			if (!removeMethodAttribs && mv != null) {
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

	public void update(boolean useMaxs, boolean isLibrary, boolean removeSource, boolean removeInnerOuters, boolean removeClassAnnos, boolean removeClassAttribs,
			boolean removeMethods, boolean removeParameterData, boolean removeMethodAnnos, boolean removeLocalData, boolean removeLines, boolean removeFrames,
			boolean removeMethodAttribs) {
		this.removeSource = removeSource;
		this.removeInnerOuters = removeInnerOuters;
		this.removeClassAnnos = removeClassAnnos;
		this.removeClassAttribs = removeClassAttribs;
		this.removeParameterData = removeParameterData;
		this.removeMethodAnnos = removeMethodAnnos;
		this.removeLocalData = removeLocalData;
		this.removeLines = removeLines;
		this.removeFrames = removeFrames;
		this.removeMethodAttribs = removeMethodAttribs;

	}
}
