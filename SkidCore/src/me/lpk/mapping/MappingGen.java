package me.lpk.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.loaders.EnigmaLoader;
import me.lpk.mapping.loaders.MappingLoader;
import me.lpk.mapping.loaders.ProguardLoader;
import me.lpk.mapping.loaders.SRGLoader;
import me.lpk.util.AccessHelper;
import me.lpk.util.JarUtils;
import me.lpk.util.ParentUtils;
import me.lpk.util.RegexUtils;

public class MappingGen {
	/**
	 * 
	 * @param map
	 * @param nodes
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromSRG(File file, Map<String, ClassNode> nodes) {
		Map<String, MappedClass> base = mappingsFromNodes(nodes);
		MappingLoader loader = new SRGLoader(nodes);
		try {
			Map<String, MappedClass> newMappings = loader.read(new FileReader(file));
			for (MappedClass mappedClass : newMappings.values()) {
				newMappings = linkMappings(mappedClass, newMappings);
			}
			base = fixFromMappingsText(base, newMappings);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return base;
	}

	/**
	 * Returns a map of class names to mapped classes given an Engima mapping
	 * file.
	 * 
	 * @param file
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromEnigma(File file, Map<String, ClassNode> nodes) {
		Map<String, MappedClass> base = mappingsFromNodes(nodes);
		MappingLoader loader = new EnigmaLoader(nodes);
		try {
			Map<String, MappedClass> newMappings = loader.read(new FileReader(file));
			for (MappedClass mappedClass : newMappings.values()) {
				newMappings = linkMappings(mappedClass, newMappings);
			}
			base = fixFromMappingsText(base, newMappings);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return base;
	}

	/**
	 * Returns a map of class names to mapped classes given a proguard mapping
	 * file.
	 * 
	 * @param file
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromProguard(File file, Map<String, ClassNode> nodes) {
		Map<String, MappedClass> base = mappingsFromNodes(nodes);
		MappingLoader loader = new ProguardLoader(nodes);
		try {
			Map<String, MappedClass> newMappings = loader.read(new FileReader(file));
			for (MappedClass mappedClass : newMappings.values()) {
				newMappings = linkMappings(mappedClass, newMappings);
			}
			base = fixFromMappingsText(base, newMappings);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return base;
	}

	/**
	 * Given two maps of mappings, applies the data from the new map to the base
	 * map.
	 * 
	 * @param base
	 * @param newMappings
	 * @return
	 */
	private static Map<String, MappedClass> fixFromMappingsText(Map<String, MappedClass> base, Map<String, MappedClass> newMappings) {
		for (String className : newMappings.keySet()) {
			MappedClass baseClass = base.get(className);
			MappedClass newClass = newMappings.get(className);
			if (baseClass == null) {
				continue;
			}
			baseClass.setNewName(newClass.getNewName());
			for (MappedMember newMember : newClass.getFields()) {
				MappedMember baseMember = ParentUtils.findField(baseClass, newMember.getOriginalName(), newMember.getDesc());
				if (baseMember != null && ParentUtils.matches(baseMember, newMember.getOriginalName(), newMember.getDesc())) {
					baseMember.setNewName(newMember.getNewName());
				}
			}
			for (MappedMember newMember : newClass.getMethods()) {
				MappedMember baseMember = ParentUtils.findMethod(baseClass, newMember.getOriginalName(), newMember.getDesc());
				if (baseMember != null && ParentUtils.matches(baseMember, newMember.getOriginalName(), newMember.getDesc())) {
					baseMember.setNewName(newMember.getNewName());
				}
			}
			base.put(className, baseClass);
		}
		return base;
	}

	/**
	 * Returns a map of class names to mapped classes given a jar file.
	 * 
	 * @param file
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromJar(File file) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtils.loadClasses(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mappingsFromNodes(nodes);
	}

	/**
	 * Returns a map of class names to mapped classes given a map of class names
	 * to ClassNodes.
	 * 
	 * @param nodes
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromNodes(Map<String, ClassNode> nodes) {
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>();
		for (ClassNode node : nodes.values()) {
			mappings = generateClassMapping(node, nodes, mappings);
		}
		for (String name : mappings.keySet()) {
			mappings = linkMappings(mappings.get(name), mappings);
		}
		return mappings;
	}

	/**
	 * Returns a map of class names to mapped classes given a map of class names
	 * to ClassNodes. Does not link the classes once the mappings are generated.
	 * 
	 * @param nodes
	 * @return
	 */
	public static Map<String, MappedClass> mappingsFromNodesNoLinking(Map<String, ClassNode> nodes) {
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>();
		for (ClassNode node : nodes.values()) {
			mappings = generateClassMapping(node, nodes, mappings);
		}
		return mappings;
	}

	/**
	 * Generates mapping for the given node and it's parents / interfaces.
	 * 
	 * @param node
	 * @param nodes
	 * @param mappings
	 */
	private static Map<String, MappedClass> generateClassMapping(ClassNode node, Map<String, ClassNode> nodes, Map<String, MappedClass> mappings) {
		boolean hasParents = node.name.equals("java/lang/Object") && !node.superName.equals("java/lang/Object");
		boolean hasInterfaces = node.interfaces.size() > 0;
		if (hasParents) {
			boolean parentRenamed = mappings.containsKey(node.superName);
			ClassNode parentNode = nodes.get(node.superName);
			if (parentNode != null && !parentRenamed) {
				boolean conflict = ParentUtils.isLoop(node, nodes, 0);
				if (conflict) {
					// Really ugly hack for when class super names loop.
					// Only happens on a few obfuscated samples
					parentNode.superName = "java/lang/Object";
				} else {
					generateClassMapping(parentNode, nodes, mappings);
				}
			}
		}
		if (hasInterfaces) {
			for (String interfaze : node.interfaces) {
				boolean interfaceRenamed = mappings.containsKey(interfaze);
				ClassNode interfaceNode = nodes.get(interfaze);
				if (interfaceNode != null && !interfaceRenamed) {
					generateClassMapping(interfaceNode, nodes, mappings);
				}
			}
		}
		boolean isRenamed = mappings.containsKey(node.name);
		if (!isRenamed) {
			MappedClass mappedClass = new MappedClass(node, node.name);
			for (FieldNode fn : node.fields) {
				mappedClass.addField(new MappedMember(mappedClass, fn, mappedClass.getFields().size(), fn.desc, fn.name));
			}
			for (MethodNode mn : node.methods) {
				mappedClass.addMethod(new MappedMember(mappedClass, mn, mappedClass.getMethods().size(), mn.desc, mn.name));
			}
			mappings.put(node.name, mappedClass);
		}
		return mappings;
	}

	/**
	 * Iterates through entries in the given map and matches together parent and
	 * child classes.
	 * 
	 * @param mappings
	 * @return
	 */
	public static Map<String, MappedClass> linkMappings(MappedClass mappedClass, Map<String, MappedClass> mappings) {
		// Setting up parent structure
		if (!mappedClass.hasParent()) {
			// No parent, check to see if one can be found
			MappedClass parentMappedClass = mappings.get(mappedClass.getNode().superName);
			if (parentMappedClass != null) {
				mappings = linkMappings(parentMappedClass, mappings);
				parentMappedClass.addChild(mappedClass);
			}
		}
		// Adding interfaces
		if (mappedClass.getInterfaces().size() == 0) {
			for (String interfaze : mappedClass.getNode().interfaces) {
				MappedClass mappedInterface = mappings.get(interfaze);
				if (mappedInterface != null) {
					mappings = linkMappings(mappedInterface, mappings);
					mappedInterface.addInterfaceImplementation(mappedClass);
				}
			}
		}
		// Setting up outer/inner class structure
		if (mappedClass.getOuterClass() == null) {
			boolean outerClassASM = mappedClass.getNode().outerClass != null;
			boolean outerClassName = mappedClass.getOriginalName().contains("$");
			String outerClass = null;
			if (outerClassASM) {
				outerClass = mappedClass.getNode().outerClass;
			} else if (outerClassName) {
				outerClass = mappedClass.getOriginalName().substring(0, mappedClass.getOriginalName().indexOf("$"));
				if (outerClass.endsWith("/")) {
					// TODO: Do this better, account for obfuscations that
					// purposefully put $'s in names
					// The name starts with the $ so probably not actually an
					// outer class. Just obfuscation.
					outerClass = null;
				}
			} else {
				int synths = 0, synthID = -1;
				for (int fieldKey = 0; fieldKey < mappedClass.getFields().size(); fieldKey++) {
					// Check for synthetic fields
					FieldNode fn = mappedClass.getFields().get(fieldKey).getFieldNode();
					if (fn == null) {
						continue;
					}
					int access = fn.access;
					if (AccessHelper.isSynthetic(access) && AccessHelper.isFinal(access) && !AccessHelper.isPublic(access) && !AccessHelper.isPrivate(access)
							&& !AccessHelper.isProtected(access)) {
						synths++;
						synthID = fieldKey;
					}
				}
				if (synths == 1) {
					// If there is a single synthetic field referencing a class,
					// it's probably an anonymous inner class.
					FieldNode fn = mappedClass.getFields().get(synthID).getFieldNode();
					if (fn != null && fn.desc.contains(";")) {
						List<String> matches = RegexUtils.matchDescriptionClasses(fn.desc);
						if (matches.size() > 0) {
							outerClass = matches.get(0);
						}
					}
				}
			}
			// Adding inner classes
			if (outerClass != null) {
				MappedClass outer = mappings.get(outerClass);
				if (outer != null) {
					outer.addInnerClass(mappedClass);
					mappings = linkMappings(outer, mappings);
				}
			}
		}
		// Adding method overrides
		for (MappedMember method : mappedClass.getMethods()) {
			if (method.getOverride() != null) {
				continue;
			}
			MappedMember methodOverriden = ParentUtils.findMethodParent(mappedClass, method.getOriginalName(), method.getDesc());
			if (methodOverriden != null) {
				method.setOverride(methodOverriden);
			}
		}
		mappings.put(mappedClass.getOriginalName(), mappedClass);
		return mappings;
	}
}
