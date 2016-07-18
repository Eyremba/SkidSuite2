package me.lpk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.lpk.log.Logger;
import me.lpk.util.AccessHelper;
import me.lpk.util.RegexUtils;

// Ok so the new idea was to start from the entry point and remove any method that wasn't referenced from there in a branching-out pattern,
// Things like anonymous classes in frameworks like Swing therefore get entierly removed and everything breaks...
// All of the code below is temporary and the concept will probably have to be redesigned to accomidate for things like Swing...
//
// ;-;
//
// halp
//
public class Remover {
	private final Set<String> visited = new HashSet<String>();
	private Map<String, Clas> classes = new HashMap<String, Clas>();

	public boolean isMethodUsed(String className, String mthdKey) {
		Clas c = classes.get(className);
		if (c == null) {
			return true;
		}
		if (c.methods.containsKey(mthdKey)) {
			return c.methods.get(mthdKey);
		}
		return true;
	}

	public Set<String> evaluate_new(String mainClass, Map<String, ClassNode> nodes) {
		ClassNode initNode = nodes.get(mainClass);
		if (initNode == null) {
			JOptionPane.showMessageDialog(null, "Main class '" + mainClass + "' is not in the Jar!", "Error", JOptionPane.ERROR_MESSAGE);
			return classes.keySet();
		}
		Clas c = new Clas(initNode);
		MethodNode mn = findMain(initNode);
		if (mn == null) {
			JOptionPane.showMessageDialog(null, "Main class '" + mainClass + "' does not have a main method!", "Error", JOptionPane.ERROR_MESSAGE);
			return classes.keySet();
		}
		try {
			for (MethodNode mn2 : initNode.methods) {
				//check_new(c, mn2, nodes);
			}
			check_new(c, mn, nodes);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return classes.keySet();
	}

	private Map<String, Clas> check_new(Clas c, MethodNode mn, Map<String, ClassNode> nodes) {
		boolean noMthd = mn == null;
		ClassNode noodle = nodes.get(c.getName());

		for (String interf : noodle.interfaces) {
			if (nodes.containsKey(interf)) {
				ClassNode interfacee = nodes.get(interf);
				check_new(getClas(interfacee), null, nodes);
			}
		}

		if (nodes.containsKey(noodle.superName)) {
			ClassNode superr = nodes.get(noodle.superName);
			check_new(getClas(superr), null, nodes);
		}
		if (noMthd) {
			// Field or some access to class (so keep it), but no methods
			// registered to keep
			putClass(c);

			return classes;
		} else {
			// Visiting method and class
			if (classes.containsKey(c.getName()) && classes.get(c.getName()).isMarked(mn)) {
				return classes;
			}
			putClass(c);
			c.markUsed(mn);
		}
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			switch (ain.getType()) {
			case AbstractInsnNode.METHOD_INSN:
				// Visit method and mark it if possible.
				MethodInsnNode min = (MethodInsnNode) ain;
				if (!nodes.containsKey(min.owner)) {
					continue;
				}
				Clas c2 = getClas(nodes.get(min.owner));
				MethodNode mn2 = findMethod(nodes, min);
				if (mn2 == null) {
					System.err.println("Uhhhh.... fuck." + min.owner + ":" + min.name + min.desc);

					continue;
				}
				check_new(c2, mn2, nodes);
				break;
			case AbstractInsnNode.FIELD_INSN:
				// Visit class, but don't mark any methods.
				FieldInsnNode fin = (FieldInsnNode) ain;
				if (!nodes.containsKey(fin.owner)) {
					continue;
				}
				ClassNode fnode = nodes.get(fin.owner);
				Clas c3 = getClas(fnode);
				check_new(c, null, nodes);
				break;
			case AbstractInsnNode.TYPE_INSN:
				// No visit, but add to classes
				TypeInsnNode tin = (TypeInsnNode) ain;
				if (!nodes.containsKey(tin.desc)) {
					continue;
				}
				Clas c4 = getClas(nodes.get(tin.desc));
				check_new(c4, null, nodes);
				break;
			case AbstractInsnNode.LDC_INSN:
				LdcInsnNode ldc = (LdcInsnNode) ain;
				if (ldc.cst instanceof Type) {
					Type t = (Type) ldc.cst;
					String name = t.getClassName().replace(".", "/");
					if (!nodes.containsKey(name)) {
						continue;
					}
					Clas c5 = getClas(nodes.get(name));
					check_new(c5, null, nodes);
				}
				break;
			}
		}
		return classes;
	}

	private Clas getClas(ClassNode cn) {
		if (cn != null && classes.containsKey(cn.name)) {
			return classes.get(cn.name);
		}
		return new Clas(cn);
	}

	private void putClass(Clas c) {
		if (classes.containsKey(c.getName())) {
			return;
		}
		classes.put(c.getName(), c);
	}

	public Set<String> evaluate_old(String mainClass, Map<String, ClassNode> nodes) {
		Set<String> keep = new HashSet<String>();
		ClassNode initNode = nodes.get(mainClass);
		if (initNode == null) {
			JOptionPane.showMessageDialog(null, "Main class '" + mainClass + "' is not in the Jar!", "Error", JOptionPane.ERROR_MESSAGE);
			return keep;
		}
		keep.add(mainClass);
		keep.addAll(check_old(initNode, nodes));
		return keep;
	}

	/**
	 * 
	 * @param node
	 * @param nodes
	 * @return
	 */
	private Set<String> check_old(ClassNode node, Map<String, ClassNode> nodes) {
		Set<String> keep = new HashSet<String>();
		visited.add(node.name);
		String parent = node.superName;
		if (parent != null) {
			keep.add(parent);
			if (!visited.contains(parent) && nodes.containsKey(parent)) {
				keep.addAll(check_old(nodes.get(parent), nodes));
			}
		}
		for (String name : node.interfaces) {
			keep.add(name);
			if (!visited.contains(name) && nodes.containsKey(name)) {
				keep.addAll(check_old(nodes.get(name), nodes));
			}
		}
		for (FieldNode fn : node.fields) {
			for (String name : RegexUtils.matchDescriptionClasses(fn.desc)) {
				keep.add(name);
				if (!visited.contains(name) && nodes.containsKey(name)) {
					keep.addAll(check_old(nodes.get(name), nodes));
				}
			}
		}
		for (MethodNode mn : node.methods) {
			for (String name : RegexUtils.matchDescriptionClasses(mn.desc)) {
				keep.add(name);
				if (!visited.contains(name) && nodes.containsKey(name)) {
					keep.addAll(check_old(nodes.get(name), nodes));
				}
			}
			for (AbstractInsnNode ain : mn.instructions.toArray()) {
				if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
					FieldInsnNode fin = (FieldInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(fin.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check_old(nodes.get(name), nodes));
						}
					}
					keep.add(fin.owner);
					if (!visited.contains(fin.owner) && nodes.containsKey(fin.owner)) {
						keep.addAll(check_old(nodes.get(fin.owner), nodes));
					}
				} else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode min = (MethodInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(min.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check_old(nodes.get(name), nodes));
						}
					}
					keep.add(min.owner);
					if (!visited.contains(min.owner) && nodes.containsKey(min.owner)) {
						keep.addAll(check_old(nodes.get(min.owner), nodes));
					}
				} else if (ain.getType() == AbstractInsnNode.LDC_INSN) {
					LdcInsnNode ldc = (LdcInsnNode) ain;
					if (ldc.cst instanceof Type) {
						Type t = (Type) ldc.cst;
						String name = t.getClassName().replace(".", "/");
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check_old(nodes.get(name), nodes));
						}
					}
				} else if (ain.getType() == AbstractInsnNode.TYPE_INSN) {
					TypeInsnNode tin = (TypeInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(tin.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check_old(nodes.get(name), nodes));
						}
					}
				}
			}
		}
		return keep;
	}

	private static MethodNode findMain(ClassNode initNode) {
		for (MethodNode mn : initNode.methods) {
			if (AccessHelper.isStatic(mn.access) && AccessHelper.isPublic(mn.access) && mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V")) {
				return mn;
			}
		}
		return null;
	}

	private static MethodNode findMethod(Map<String, ClassNode> nodes, MethodInsnNode min) {
		if (!nodes.containsKey(min.owner)) {
			return null;
		}
		ClassNode node = nodes.get(min.owner);
		for (MethodNode mn : node.methods) {
			if (mn.name.equals(min.name) && mn.desc.equals(min.desc)) {
				return mn;
			}
		}
		return null;
	}

	class Clas {
		private final String name;
		private final Map<String, Boolean> methods = new HashMap<String, Boolean>();

		public Clas(ClassNode node) {
			name = node.name;
			for (MethodNode mn : node.methods) {
				methods.put(mn.name + mn.desc, false);
			}
		}

		public String getName() {
			return name;
		}

		public Set<String> getMethods() {
			return methods.keySet();
		}

		public boolean hasMethod(MethodNode mn) {
			return methods.containsKey(mn.name + mn.desc);
		}

		public boolean hasMethod(MethodInsnNode min) {
			return methods.containsKey(min.name + min.desc);
		}

		public void markUsed(MethodNode mn) {
			methods.put(mn.name + mn.desc, true);
		}

		public void markUsed(MethodInsnNode min) {
			methods.put(min.name + min.desc, true);
		}

		public boolean isMarked(MethodNode mn) {
			return methods.get(mn.name + mn.desc);
		}

		public boolean isMarked(MethodInsnNode min) {
			return methods.get(min.name + min.desc);
		}

		public void removeMethod(MethodNode mn) {
			methods.remove(mn.name + mn.desc);
		}

		public void removeMethod(MethodInsnNode min) {
			methods.remove(min.name + min.desc);
		}
	}
}
