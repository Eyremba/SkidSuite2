package me.lpk.mapping;

import java.util.Map;

import org.objectweb.asm.commons.Remapper;

import me.lpk.util.ParentUtils;
import me.lpk.util.StringUtils;

/**
 * An implementation of ASM's Remapper. Given a map of MappedClasses in the
 * constructor updates the modified values in descriptions and names.
 * 
 * <ul>
 * <li>{@link #map(String)} - map type</li>
 * <li>{@link #mapFieldName(String, String, String)} - map field name</li>
 * <li>{@link #mapMethodName(String, String, String)} - map method name</li>
 * </ul>
 */
public class SkidRemapper extends Remapper {
	private final Map<String, MappedClass> mappings;

	public SkidRemapper(Map<String, MappedClass> renamed) {
		this.mappings = renamed;
	}

	@Override
	public String mapDesc(String desc) {
		return super.mapDesc(StringUtils.fixDesc(desc, mappings));
	}

	@Override
	public String mapType(String type) {
		if (type == null) {
			return null;
		}
		return super.mapType(StringUtils.fixDesc(type, mappings));
	}

	@Override
	public String[] mapTypes(String[] types) {
		for (int i = 0; i < types.length; i++) {
			types[i] = StringUtils.fixDesc(types[i], mappings);
		}
		return super.mapTypes(types);
	}

	@Override
	public String mapMethodDesc(String desc) {
		if ("()V".equals(desc)) {
			return desc;
		}
		if (!desc.startsWith("(")) {
			// Note to self: Keep this here. Helped me realize a desc I had made
			// was invalid.
			throw new RuntimeException();
		}
		return super.mapMethodDesc(StringUtils.fixDesc(desc, mappings));
	}

	@Override
	public Object mapValue(Object value) {
		return super.mapValue(value);
	}

	@Override
	public String mapSignature(String signature, boolean typeSignature) {
		if (signature == null) {
			return null;
		}
		String s = signature;
		try {
			s = super.mapSignature(StringUtils.fixDesc(signature, mappings), typeSignature);
		} catch (java.lang.StringIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return s;
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		if (me.lpk.mapping.remap.MappingRenamer.isNameWhitelisted(name)) {
			return super.mapMethodName(owner, name, desc);
		}
		MappedClass mc = mappings.get(owner);
		if (mc == null) {
			return super.mapMethodName(owner, name, desc);
		} else {
			MappedMember mm = ParentUtils.findMethodInParentInclusive(mc, name, desc);
			if (mm != null) {
				if (mm.doesOverride()) {
					return super.mapMethodName(owner, ParentUtils.findMethodOverride(mm).getNewName(), desc);
				}else {
					return super.mapMethodName(owner, mm.getNewName(), desc);
				}
			}
		}
		return super.mapMethodName(owner, name, desc);
	}

	@Override
	public String mapInvokeDynamicMethodName(String name, String desc) {
		MappedClass mc = mappings.get(StringUtils.getMappedFromDesc(mappings, desc));
		if (me.lpk.mapping.remap.MappingRenamer.isNameWhitelisted(name)) {
			return super.mapInvokeDynamicMethodName(name, desc);
		}
		if (mc == null) {
			return super.mapInvokeDynamicMethodName(name, desc);
		} else {
			MappedMember mm = ParentUtils.findMethodInParentInclusive(mc, name, desc);
			if (mm != null) {
				return super.mapInvokeDynamicMethodName(ParentUtils.findMethodOverride(mm).getNewName(), desc);
			}
		}
		return super.mapInvokeDynamicMethodName(name, desc);
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		MappedClass mc = mappings.get(owner);
		if (mc != null) {
			MappedMember field = ParentUtils.findFieldInParentInclusive(mc, name, desc);
			if (field != null) {
				return super.mapFieldName(owner, field.getNewName(), desc);
			}
		}
		return super.mapFieldName(owner, name, desc);
	}

	@Override
	public String map(String typeName) {
		return super.map(StringUtils.fixDesc(typeName, mappings));
	}
}