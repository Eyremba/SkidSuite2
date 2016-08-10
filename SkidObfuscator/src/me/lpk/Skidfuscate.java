package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.MappingRenamer;
import me.lpk.mapping.remap.impl.ModeSimple;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Skidfuscate {
	private static Skidfuscate instance;
	private static Map<String, File> libraries = new HashMap<String, File>();
	public static Skidfuscate get(){
		if (instance == null){
			instance = new Skidfuscate();
		}
		return instance;
	}
	public void addLibrary(File jar) {
		libraries.put(jar.getAbsolutePath(), jar);
	}
	public void parse(File jar, Map<String, Boolean> options) {
		LazySetupMaker dat = LazySetupMaker.get(jar.getAbsolutePath(), true);
		Map<String, ClassNode> nodes = new HashMap<String, ClassNode>(dat.getNodes());
		Map<String, MappedClass> mappings = new HashMap<String, MappedClass>(dat.getMappings());
		MappingRenamer.remapClasses(mappings, new ModeSimple());
		saveJar("Out.jar", jar, nodes, mappings);
	}
	
	private static void saveJar(String name, File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses, true);
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
