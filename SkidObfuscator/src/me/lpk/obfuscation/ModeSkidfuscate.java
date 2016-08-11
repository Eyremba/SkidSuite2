package me.lpk.obfuscation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.remap.MappingMode;

public class ModeSkidfuscate extends MappingMode {
	private final String c, f, m;
	private final Map<String, List<String>> descs= new HashMap<String, List<String>>();
	private final List<String> mc = new ArrayList<String>();
	public ModeSkidfuscate(String c, String f, String m) {
		this.c = c; this.f = f; this.m = m;
	}

	@Override
	public String getClassName(ClassNode cn) {
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V")) {
				return cn.name;
			}
		}
		String name =  getName(c, mc.size());
		mc.add(name);
		descs.clear();
		return name;
	}

	@Override
	public String getMethodName(MethodNode mn) {
		if (mn.desc.equals("([Ljava/lang/String;)V") && mn.name.equals("main")) {
			return "main";
		} 
		int mi = descs.containsKey(mn.desc) ? descs.get(mn.desc).size() : 0;
		String name =  getName(m, mi);
		if (descs.containsKey(mn.desc)){
			descs.get(mn.desc).add(name);
		} else {
			descs.put(mn.desc, new ArrayList<String>());
			descs.get(mn.desc).add(name);
		}
		return name;
	}

	@Override
	public String getFieldName(FieldNode fn) {
		int mi = descs.containsKey(fn.desc) ? descs.get(fn.desc).size() : 0;
		String name =  getName(m, mi);
		if (descs.containsKey(fn.desc)){
			descs.get(fn.desc).add(name);
		} else {
			descs.put(fn.desc, new ArrayList<String>());
			descs.get(fn.desc).add(name);
		}
		return name;
	}

	public String getName(String alphabet, int i) {
		String s = "";
		int length = (int)((i + 0.0f) / (alphabet.length() + 0.0f));
		while (s.length() <= length){
			s += toString(alphabet, i);
		}
		return s;
	}
	
	 public static String toString(String alphabet, int i) {
	        int n = alphabet.length() - 1;
	        final char[] array = new char[33];
	        final boolean b = i < 0;
	        int n2 = 32;
	        if (!b) {
	            i = -i;
	        }
	        while (i <= -n) {
	            array[n2--] = alphabet.toCharArray()[-(i % n)];
	            i /= n;
	        }
	        array[n2] = alphabet.toCharArray()[-i];
	        if (b) {
	            array[--n2] = '-';
	        }
	        return new String(array, n2, 33 - n2);
	    }

}
