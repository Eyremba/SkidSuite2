package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.antis.AntiBase;
import me.lpk.antis.impl.*;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Main {

	public static void main(String[] args) {
		try {
			runAnti(new File("ZKMNew.jar"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runAnti(File jar) throws IOException {
		LazySetupMaker.clearExtraLibraries();
		Classpather.addFile(jar);
		LazySetupMaker lsm = LazySetupMaker.get(jar.getAbsolutePath(), false, true);
		for (String className : lsm.getNodes().keySet()) {
			AntiBase anti = new AntiZKM8();
			ClassNode node = lsm.getNodes().get(className);
			lsm.getNodes().put(className, anti.scan(node));
		}
		Map<String, byte[]> out = MappingProcessor.process(lsm.getNodes(), new HashMap<String, MappedClass>(), true);
		out.putAll(JarUtils.loadNonClassEntries(jar));
		Logger.logLow("Saving...");
		JarUtils.saveAsJar(out, jar.getName() + "-re.jar");
	}
}
