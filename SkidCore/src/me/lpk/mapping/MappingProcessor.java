package me.lpk.mapping;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.util.ParentUtils;

public class MappingProcessor {
	public static boolean PRINT;
	private static Map<String, MappedClass> temp;

	/**
	 * Given a map of ClassNodes and mappings, returns a map of class names to
	 * class bytes.
	 * 
	 * @param nodes
	 * @param mappings
	 * @return
	 */
	public static Map<String, byte[]> process(Map<String, ClassNode> nodes, Map<String, MappedClass> mappings, boolean useMaxs) {
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		SkidRemapper mapper = new SkidRemapper(mappings);
		temp = mappings;
		try {
			for (ClassNode cn : nodes.values()) {
				ClassWriter cw = new MappingClassWriter(useMaxs ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
				ClassVisitor remapper = new ClassRemapper(cw, mapper);
				cn.accept(remapper);
				out.put(mappings.containsKey(cn.name) ? mappings.get(cn.name).getNewName() : cn.name, cw.toByteArray());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	/**
	 * The temp static map needed for this is ugly, but it beats requring the
	 * classes being loaded in the JVM by a longshot.
	 */
	static class MappingClassWriter extends ClassWriter {

		public MappingClassWriter(int i) {
			super(i);
		}

		@Override
		protected String getCommonSuperClass(final String type1, final String type2) {
			MappedClass mc1 = temp.get(type1);
			MappedClass mc2 = temp.get(type2);
			if (mc1 == null || mc2 == null) {
				return "java/lang/Object";
			}
			MappedClass common = ParentUtils.findCommonParent(mc1, mc2);
			if (common == null) {
				return "java/lang/Object";
			}
			return common.getNewName();
		}
	}
}
