package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.MappingMode;
import me.lpk.mapping.remap.MappingRenamer;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Main {

	public static void main(String[] args) {
		obfuscating("SkidASM.jar", "Out.jar");
	}

	public static void renaming(String jarIn, String jarOut) {
		LazySetupMaker dat = LazySetupMaker.get(jarIn, true, false);
		Map<String, ClassNode> nodes = new HashMap<String, ClassNode>(dat.getNodes());
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>(dat.getMappings());
		//
		//
		Logger.logLow("Renaming");
		mappings = MappingRenamer.remapClasses(mappings, mm());
		mappings.get("me/lpk/MainWindow").setNewName("MainWindow");
		//
		//
		Logger.logLow("Saving");
		saveJar(jarOut, new File(jarIn), nodes, mappings);
		System.out.println("Renaming done!");
	}

	public static void obfuscating(String jarIn, String jarOut) {
		LazySetupMaker dat = LazySetupMaker.get(jarIn, false, false);
		Map<String, ClassNode> nodes = new HashMap<String, ClassNode>(dat.getNodes());
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>(dat.getMappings());
		//
		//
		// mappings.get("me/lpk/MainWindow").setNewName("MainWindow");
		//
		//
		Logger.logLow("Modifying - Encryption");
		for (ClassNode cn : nodes.values()) {
			if (mappings.get(cn.name) == null) {
				System.err.println(cn.name);
			}
			Stringer.stringEncrypt(cn, mappings.get(cn.name).getNewName());
			//
			//

			//
			//
			// for (MethodNode mn : cn.methods) for (int i = 0; i < 50; i++) Flow.shift_failed(cn, mn);

		}
		Logger.logLow("Modifying - Member Access");
		for (ClassNode cn : nodes.values()) {
			for (FieldNode fn : cn.fields) {
				fn.access = fn.access | Opcodes.ACC_SYNTHETIC;
			}
			for (MethodNode mn : cn.methods) {
				mn.access = mn.access | Opcodes.ACC_SYNTHETIC;
			}
		}
		//
		//
		Logger.logLow("Saving");
		saveJar(jarOut, new File(jarIn), nodes, mappings);
		System.out.println("Obfuscating done!");
	}

	public static void obfuscating2(String jarIn, String jarOut) {
		LazySetupMaker dat = LazySetupMaker.get(jarIn, false, false);
		Map<String, ClassNode> nodes = new HashMap<String, ClassNode>(dat.getNodes());
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>(dat.getMappings());
		Logger.logLow("Modifying - Encryption");
		for (ClassNode cn : nodes.values()) {
			for (MethodNode mn : cn.methods) {
				for (AbstractInsnNode ain : mn.instructions.toArray()) {
					int t = ain.getType();
					if (t == AbstractInsnNode.FRAME || t == AbstractInsnNode.LINE) {
						mn.instructions.remove(ain);
					}
				}
					Flow.shift_failed(cn, mn);
				
			}

		}
		Logger.logLow("Saving");
		saveJar(jarOut, new File(jarIn), nodes, mappings);
		System.out.println("Obfuscating done!");
	}

	private static MappingMode mm() {
		return (new MappingMode() {

			private Set<String> used = new HashSet<String>();

			@Override
			public String getClassName(ClassNode cn) {
				return randName();
			}

			@Override
			public String getMethodName(MethodNode mn) {
				return randName();
			}

			@Override
			public String getFieldName(FieldNode fn) {
				return randName();
			}

			private String randName() {
				StringBuilder sb = new StringBuilder();
				while (sb.length() < 9 || used.contains(sb.toString())) {
					int o = (int) Math.round(Math.random() * 9);
					switch (o) {
					case 0:
						sb.append('\u2580');
						break;
					case 1:
						sb.append('\u2581');
						break;
					case 2:
						sb.append('\u2582');
						break;
					case 3:
						sb.append('\u2583');
						break;
					case 4:
						sb.append('\u2584');
						break;
					case 5:
						sb.append('\u2585');
						break;
					case 6:
						sb.append('\u2586');
						break;
					case 7:
						sb.append('\u2587');
						break;
					case 8:
						sb.append('\u2588');
						break;
					case 9:
						sb.append('\u2589');
						break;
					}
				}
				used.add(sb.toString());
				return sb.toString();
			}
		});
	}

	private static void saveJar(String name, File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses, false);
		try {
			out.putAll(JarUtils.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int renamed = 0;
		for (MappedClass mc : mappedClasses.values()) {
			if (mc.isTruelyRenamed()) {
				renamed++;
			}
		}
		System.out.println("Saving...  [Ranemed " + renamed + " classes]");
		JarUtils.saveAsJar(out, name);
	}

}
