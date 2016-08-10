package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.MappingMode;
import me.lpk.mapping.remap.MappingRenamer;
import me.lpk.obfuscation.AntiDecompile;
import me.lpk.obfuscation.Flow;
import me.lpk.obfuscation.Stringer;
import me.lpk.util.AccessHelper;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Main {

	public static void main(String[] args) {
		obfuscating("In.jar", "Out.jar");
	}

	public static void obfuscating(String jarIn, String jarOut) {
		LazySetupMaker dat = LazySetupMaker.get(jarIn, false);
		Map<String, ClassNode> nodes = new HashMap<String, ClassNode>(dat.getNodes());
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>(dat.getMappings());
		//
		boolean tryCatch = false;
		boolean ldc = false;
		boolean math = true;
		boolean types = false;
		boolean varDupes = false;
		boolean access = false;
		boolean string = true;
		boolean gotos = false;
		boolean badPop = false;
		boolean retObjErr = false;
		if (tryCatch) {
			Logger.logLow("Modifying - Try Catch");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					Flow.addTryCatch(mn, "java/lang/Exception", null);
				}
			}
		}
		if (retObjErr) {
			Logger.logLow("Modifying - Bad Return");
			for (ClassNode cn : nodes.values()) {
				AntiDecompile.retObjErr(cn);
			}
		}
		if (badPop) {
			Logger.logLow("Modifying - Bad Pop");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					AntiDecompile.badPop(mn);
				}
			}
		}
		if (access) {
			Logger.logLow("Modifying - Member Access");
			for (ClassNode cn : nodes.values()) {
				for (FieldNode fn : cn.fields) {
					if (!AccessHelper.isSynthetic(fn.access)) {
						fn.access = fn.access | Opcodes.ACC_SYNTHETIC;
					}
				}
				for (MethodNode mn : cn.methods) {
					if (mn.name.contains("<")) {
						continue;
					}
					if (!AccessHelper.isSynthetic(mn.access)) {
						mn.access = mn.access | Opcodes.ACC_SYNTHETIC;
					}
					if (!AccessHelper.isBridge(mn.access)) {
						mn.access = mn.access | Opcodes.ACC_BRIDGE;
					}
				}
			}
		}
		if (string) {
			Logger.logLow("Modifying - Encryption");
			for (ClassNode cn : nodes.values()) {
				Stringer.stringEncrypt(cn);
			}
		}
		if (gotos) {
			Logger.logLow("Modifying - Flow Obfuscation");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					for (int i = 0; i < 10; i++) {
						Flow.randomGotos(cn, mn);
					}
				}
			}
		}
		if (types) {
			Logger.logLow("Modifying - Type Overload");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					AntiDecompile.types(mn);
				}
			}
		}
		if (varDupes) {
			Logger.logLow("Modifying - Var Dupes");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					AntiDecompile.duplicateVars(mn);
				}
			}
		}
		if (ldc) {
			Logger.logLow("Modifying - Massive LDC");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					AntiDecompile.massiveLdc(mn);
				}
			}
		}
		if (math) {
			Logger.logLow("Modifying - Math obfuscation");
			for (ClassNode cn : nodes.values()) {
				for (MethodNode mn : cn.methods) {
					Flow.destroyMath(mn);
				}
			}
		}
		//
		//
		saveJar(jarOut, new File(jarIn), nodes, mappings);
		System.out.println("Finished!");
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
