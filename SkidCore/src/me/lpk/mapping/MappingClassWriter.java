package me.lpk.mapping;

import java.util.Map;

import org.objectweb.asm.ClassWriter;

import me.lpk.util.ParentUtils;

/**
 * A ClassWriter that works off of MappedClasses. Does not require classes being
 * loaded into the JVM.
 */
public class MappingClassWriter extends ClassWriter {
	private final Map<String, MappedClass> mappings;

	public MappingClassWriter(Map<String, MappedClass> mappings, int i) {
		super(i);
		this.mappings = mappings;
	}

	@Override
	protected String getCommonSuperClass(final String type1, final String type2) {
		MappedClass mc1 = mappings.get(type1);
		MappedClass mc2 = mappings.get(type2);
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