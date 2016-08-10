package me.lpk.mapping.remap.impl;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.remap.MappingMode;

public class ModeSimple extends MappingMode {
	private int classIndex, methodIndex, fieldIndex;

	@Override
	public String getClassName(ClassNode cn) {
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V")) {
				return cn.name;
			}
		}
		return "Class" + classIndex++;
	}

	@Override
	public String getMethodName(MethodNode mn) {
		switch (mn.desc) {
		case "([Ljava/lang/String;)V":
			if (mn.name.equals("main")) {
				return "main";
			} else {
				break;
			}
		case "()I":
			return "getInt" + methodIndex++;
		case "()J":
			return "getLong" + methodIndex++;
		case "()Z":
			return "getBoolean" + methodIndex++;
		case "()Ljava/lang/String":
			return "getString" + methodIndex++;
		case "()Ljava.util.Set;":
		case "()Ljava.util.HashSet;":
			return "getSet" + methodIndex++;
		case "()Ljava.util.List;":
		case "()Ljava.util.ArrayList;":
			return "getList" + methodIndex++;
		case "()Ljava.util.Map;":
		case "()Ljava.util.HashMap;":
			return "getMap" + methodIndex++;
		case "Ljava/lang/Class;":
			return "getClass" + methodIndex++;
		case "()F":
			return "getFloat" + methodIndex++;
		case "()D":
			return "getDouble" + methodIndex++;
		}
		return "method" + methodIndex++;
	}

	@Override
	public String getFieldName(FieldNode fn) {
		switch (fn.desc) {
		case "I":
			return "int" + fieldIndex++;
		case "C":
			return "char" + fieldIndex++;
		case "J":
			return "long" + fieldIndex++;
		case "F":
			return "float" + fieldIndex++;
		case "D":
			return "double" + fieldIndex++;
		case "Z":
			return "boolean" + fieldIndex++;
		case "Ljava/lang/String;":
			return "string" + fieldIndex++;
		case "Ljava/util/Collection;":
			return "collection" + fieldIndex++;
		case "Ljava/util/Set;":
		case "Ljava/util/HashSet;":
		case "Ljava/util/LinkedHashSet;":
			return "set" + fieldIndex++;
		case "Ljava/util/List;":
		case "Ljava/util/ArrayList;":
		case "Ljava/util/LinkedList;":
			return "list" + fieldIndex++;
		case "Ljava/util/Map;":
		case "Ljava/util/HashMap;":
		case "Ljava/util/LinkedHashMap;":
			return "map" + fieldIndex++;
		case "Ljava/lang/Class;":
			return "class" + fieldIndex++;
		}
		if (fn.desc.startsWith("[")) {
			return "array" + fieldIndex++;
		}
		return "field" + fieldIndex++;
	}
}