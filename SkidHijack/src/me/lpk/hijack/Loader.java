package me.lpk.hijack;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.sun.tools.attach.VirtualMachine;

public class Loader {
	/**
	 * Loads an agent from the given path.
	 * 
	 * @param agentPath
	 *            The path to the agent jar
	 */
	public static void loadAgent(String agentPath) {
		String vmName = ManagementFactory.getRuntimeMXBean().getName();
		int index = vmName.indexOf('@');
		String pid = vmName.substring(0, index);
		try {
			File agentFile = new File(agentPath);
			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(agentFile.getAbsolutePath(), "");
			VirtualMachine.attach(vm.id());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}