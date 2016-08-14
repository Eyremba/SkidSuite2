package me.lpk.mapping.remap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.util.ParentUtils;

public class MappingRenamer {
	private static final Set<String> whitelist = new HashSet<String>();
	private final List<String> remapped = new ArrayList<String>();

	/**
	 * Updates the information of the given map of MappedClasses according to
	 * the mapping standards given by the MappingMode.
	 * 
	 * @param mappings
	 * @param mode
	 * @return
	 */
	public Map<String, MappedClass> remapClasses(Map<String, MappedClass> mappings, MappingMode mode) {
		for (MappedClass mc : mappings.values()) {
			if (!mc.isLibrary()) {
				remapClass(mc, mappings, mode);
			}
		}
		return mappings;
	}

	/**
	 * Updates a given class in the given mappings with names based on the given
	 * MappingMode.
	 * 
	 * @param mc
	 * @param mappings
	 * @param mode
	 * @return
	 */
	public Map<String, MappedClass> remapClass(MappedClass mc, Map<String, MappedClass> mappings, MappingMode mode) {
		if (mc.isLibrary() || remapped.contains(mc.getOriginalName())) {
			return mappings;
		}
		if (mc.hasParent()) {
			mappings = remapClass(mc.getParent(), mappings, mode);
		}
		for (MappedClass interfaze : mc.getInterfaces()) {
			mappings = remapClass(interfaze, mappings, mode);
		}
		if (mc.isInnerClass()) {
			mappings = remapClass(mc.getOuterClass(), mappings, mode);
		}
		if (!mc.isInnerClass()) {
			// Handling naming of normal class
			 mc.setNewName(mode.getClassName(mc));
		} else {
			// Handling naming of inner class names
			MappedClass outter = mc.getOuterClass();
			String newName = mode.getClassName(mc);
			String post = newName.contains("/") ? newName.substring(newName.lastIndexOf("/") + 1, newName.length()) : newName;
			mc.setNewName(outter.getNewName() + "$" + post);
		}
		for (MappedMember mm : mc.getFields()) {
			// Rename fields
			mm.setNewName(mode.getFieldName(mm));
		}
		for (MappedMember mm : mc.getMethods()) {
			// Rename methods
			if (keepName(mm)) {
				// Skip methods that should not be renamed
				continue;
			}
			MappedMember parentMember = ParentUtils.findMethodOverride(mm);
			// Check and see if theres a parent member to pull names from.
			if (parentMember.equals(mm)) {
				// No parent found. Give method a name
				mm.setNewName(mode.getMethodName(mm));
			} else {
				
				// Parent found. Give method parent's name.
				mm.setNewName(parentMember.getNewName());				
				// Make sure if override structure is convoluted it's all named
				// correctly regardless.
				if (mm.doesOverride() && !mm.isOverriden()){
					fixOverrideNames(mm, parentMember);
				}	
			}
			MethodNode mn = mm.getMethodNode();
			updateStrings(mn, mappings);
		}
		remapped.add(mc.getOriginalName());
		return mappings;
	}

	/**
	 * Ensures all methods in the override structure have the same name. This is
	 * only needed for cases like: http://pastebin.com/CpeD6wgN <br>
	 * TODO: Determine if this step is even needed for each input and ignore it
	 * if it's not needed.
	 * 
	 * @param mm
	 * @param override
	 */
	private static void fixOverrideNames(MappedMember mm, MappedMember override) {
		for (MappedMember mm2 : mm.getOverrides()) {
			fixOverrideNames(mm2, override);
		}
		mm.setNewName(override.getNewName());
	}

	/**
	 * Updates strings when they are used in situations such as Class.forName /
	 * Reflection.
	 * 
	 * @param mn
	 * @param mappings
	 */
	private static void updateStrings(MethodNode mn, Map<String, MappedClass> mappings) {
		// TODO: Check for Class.forName(String)
	}

	/**
	 * Checks if a given MappedMember should not be renamed.
	 * 
	 * @param mm
	 * @return
	 */
	public static boolean keepName(MappedMember mm) {
		// Main class
		if (mm.getDesc().equals("([Ljava/lang/String;)V") && mm.getOriginalName().equals("main")) {
			return true;
		}
		// <init> or <clinit>
		if (mm.getOriginalName().contains("<")) {
			return true;
		}
		// A method name that shan't be renamed!
		if (isNameWhitelisted(mm.getOriginalName())) {
			return true;
		}
		return false;
	}

	public static boolean isNameWhitelisted(String name) {
		return whitelist.contains(name);
	}

	static {

		// Should let user add additional names to the list
		// I guess classes like Enum don't have this as parent methods per say,
		// so this will be necessary.
		Collections.addAll(whitelist, "contains", "toString", "equals", "clone", "run", "start");
	}
}
