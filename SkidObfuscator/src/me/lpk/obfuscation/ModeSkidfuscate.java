package me.lpk.obfuscation;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.remap.MappingMode;

public class ModeSkidfuscate extends MappingMode {
	/**
	 * Alphabets
	 */
	private final String c, f, m;
	/**
	 * Current classnode.
	 */
	private ClassNode current;
	/**
	 * Map of descriptions with times they've been named before. Used for making
	 * as many of the same named things as possible.
	 */
	private final Map<String, Integer> descs = new HashMap<String, Integer>();
	/**
	 * Number of classes encountered
	 */
	private int classes;

	public ModeSkidfuscate(String c, String f, String m) {
		this.c = c;
		this.f = f;
		this.m = m;
	}

	@Override
	public String getClassName(ClassNode cn) {
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V")) {
				return cn.name;
			}
		}
		String name = getName(c, classes);
		classes++;
		current = cn;
		descs.clear();
		return name;
	}

	@Override
	public String getMethodName(MethodNode mn) {
		if (mn.desc.equals("([Ljava/lang/String;)V") && mn.name.equals("main")) {
			return "main";
		}
		// For some odd reason, there seems to be random instances where it goes
		// "Nope, never seen this sig before. But yeah I've seen it before"
		// This increments it by one breaking that thought train.
		for (MethodNode mnn : current.methods) {
			if (mnn.desc.equals(mn.desc)) {
				descs.put(mn.desc, descs.getOrDefault(mn.desc, 1) + 1);
			}
		}
		if (!descs.containsKey(mn.desc)) {
			descs.put(mn.desc, 0);
		}
		String name = getName(m, descs.get(mn.desc));
		descs.put(mn.desc, descs.get(mn.desc) + 1);
		return name;
	}

	@Override
	public String getFieldName(FieldNode fn) {
		if (!descs.containsKey(fn.desc)) {
			descs.put(fn.desc, 0);
		} else {
			descs.put(fn.desc, descs.get(fn.desc) + 1);
		}
		String name = getName(f, descs.get(fn.desc));
		descs.put(fn.desc, descs.get(fn.desc) + 1);
		return name;
	}

	public String getName(String alphabet, int i) {
		return getString(alphabet, i, alphabet.length());
	}

	/**
	 * Copy pasted from Integer.toString. Only change was providing the alphabet
	 * via parameter.
	 * 
	 * @param alpha
	 * @param i
	 * @param n
	 * @return
	 */
	public static String getString(String alpha, int i, int n) {
		char[] charz = alpha.toCharArray();
		if (n < 2) {
			n = 2;
		} else if (n > alpha.length()) {
			n = alpha.length();
		}
		final char[] array = new char[33];
		final boolean b = i < 0;
		int n2 = 32;
		if (!b) {
			i = -i;
		}
		while (i <= -n) {
			array[n2--] = charz[-(i % n)];
			i /= n;
		}
		array[n2] = charz[-i];
		if (b) {
			array[--n2] = '-';
		}
		return new String(array, n2, 33 - n2);
	}
}
