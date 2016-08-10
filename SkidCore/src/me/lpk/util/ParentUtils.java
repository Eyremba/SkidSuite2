package me.lpk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;

public class ParentUtils {
	/**
	 * Returns true if the method has called it's super method.
	 * 
	 * @param mn
	 * @return
	 */
	public static boolean callsSuper(MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getOpcode() == Opcodes.INVOKESPECIAL) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (min.name.equals(mn.name)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the field in the given class with the name and description. If
	 * it's not in the given class, parents are checked. Returns null if nothing
	 * is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findMethod(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getMethods()) {
			if (matches(mm, name, desc)) {
				return mm;
			}
		}
		return findMethodParent(owner, name, desc);
	}

	/**
	 * Returns the method in the given class with the name and description. If
	 * it's not in the given class, parents are checked. Returns null if nothing
	 * is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findField(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getFields()) {
			if (matches(mm, name, desc)) {
				return mm;
			}
		}
		return findFieldInParent(owner, name, desc);
	}

	/**
	 * Returns the method in the given class's parent with the name and
	 * description. If it's not in the given class, further parents are checked.
	 * Returns null if nothing is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findMethodParent(MappedClass owner, String name, String desc) {
		// Check for interfaces in the method's class.
		for (MappedClass interfaceClass : owner.getInterfaces()) {
			MappedMember mm = findMethod(interfaceClass, name, desc);
			if (mm != null) {
				return mm;
			}
		}
		// Check the parents
		if (owner.getParent() != null) {
			MappedMember mm = findMethod(owner.getParent(), name, desc);
			if (mm != null) {
				return mm;
			}
		}
		return null;
	}

	/**
	 * Returns the field in the given class's parent with the name and
	 * description. If it's not in the given class, further parents are checked.
	 * Returns null if nothing is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findFieldInParent(MappedClass owner, String name, String desc) {
		// Check for interfaces in the field's class.
		for (MappedClass interfaceClass : owner.getInterfaces()) {
			MappedMember mm = findField(interfaceClass, name, desc);
			if (mm != null) {
				return mm;
			}
		}
		// Check the parents
		if (owner.getParent() != null) {
			MappedMember mm = findField(owner.getParent(), name, desc);
			if (mm != null) {
				return mm;
			}
		}
		return null;
	}

	/**
	 * Finds the parent-most overridden member.
	 * 
	 * @param mm
	 * @return
	 */
	public static MappedMember findMethodOverride(MappedMember mm) {
		if (mm.doesOverride()) {
			// Overridden method's parent == given method's parent.
			if (mm.getOverride().getOwner().getOriginalName().equals(mm.getOwner().getOriginalName())) {
				return mm;
			}
			return findMethodOverride(mm.getOverride());
		}
		return mm;
	}

	/**
	 * For some reason MappedMember.findNameAndDescWhatever(name,desc) doesn't
	 * exactly work. This is an external implemtation which does the same thing
	 * but somehow works.
	 * 
	 * @param mm
	 * @param name
	 * @param desc
	 * @return
	 */
	public static boolean matches(MappedMember mm, String name, String desc) {
		if (mm.getOriginalName().equals(name)) {
			String o = "java/lang/Object";
			if (mm.getDesc().equals(desc)) {
				return true;
			} else if (mm.getDesc().contains(o) && !desc.contains(o)) {
				// Generic info is saved in the signature so if there is data in
				// the signature, check for generics.
				if (mm.getOwner().getNode().signature != null) {
					List<String> classes = RegexUtils.matchDescriptionClasses(desc);
					String descCopy = desc + "";
					for (String detection : classes) {
						descCopy = descCopy.replace(detection, o);
					}
					if (mm.getDesc().equals(descCopy)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if two mapped members are the same.
	 * 
	 * @param mm
	 * @param mm2
	 * @param orig
	 *            Whether to check original names or the updated names.
	 * @return
	 */
	public static boolean matches(MappedMember mm, MappedMember mm2, boolean orig) {
		return matches(mm, orig ? mm2.getOriginalName() : mm2.getNewName(), mm2.getDesc());
	}

	public static boolean isLoop(ClassNode node, Map<String, ClassNode> nodes, int i) {
		ClassNode parentNode = nodes.get(node.superName);
		if (parentNode == null) {
			return false;
		}
		if (node.name.equals(parentNode.superName)) {
			return true;
		}
		return false;
	}

	public static MappedClass findCommonParent(MappedClass mc1, MappedClass mc2) {
		// Are they the same?
		if (mc1.getNewName().equals(mc2.getNewName())) {
			return mc1;
		}
		// Does m1 extend or implement anything m2 does?
		for (String parentNames1 : getParents(mc1)) {
			for (String parentNames2 : getParents(mc1)) {
				if (parentNames1.equals(parentNames2)) {
					return getParent(parentNames1, mc1);
				}
			}
		}
		return null;
	}

	private static MappedClass getParent(String n, MappedClass m) {
		while (m != null) {
			if (m.getNewName().equals(n)){
				return m;
			}
			for (MappedClass i : m.getInterfaces()) {
				MappedClass p = getParent(n, i);
				if (p != null){
					return p;
				}
			}
			m = m.getParent();
		}
		return null;
	}

	private static List<String> getParents(MappedClass m) {
		List<String> list = new ArrayList<String>();
		while (m != null) {
			list.add(m.getNewName());
			for (MappedClass i : m.getInterfaces()) {
				list.addAll(getParents(i));
			}
			m = m.getParent();
		}
		return list;
	}
}
