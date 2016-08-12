package me.lpk.obfuscation.rename;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.remap.MappingMode;

public abstract class MappingModeImpl extends MappingMode {

	public abstract String getName(String alphabet, int i);
}
