package me.lpk.antis.impl;

import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.antis.AntiBase;
import me.lpk.util.OpUtil;

public class AntiDashO extends AntiBase {

	public AntiDashO(Map<String, ClassNode> nodes) {
		super(nodes);
	}

	@Override
	protected ClassNode scan(ClassNode node) {
		for (MethodNode mnode : node.methods) {
			replace(mnode);
		}
		return node;
	}

	/**
	 * Update values of the ZKM String[] with the original strings in a given
	 * method.
	 * 
	 * @param method
	 */
	private void replace(MethodNode method) {
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.LDC_INSN && 
				(ain.getNext().getOpcode() == Opcodes.BIPUSH || (ain.getNext().getOpcode() >= Opcodes.ICONST_M1 && ain.getNext().getOpcode() <= Opcodes.ICONST_5) ) && 
				ain.getNext().getNext().getOpcode() == Opcodes.INVOKESTATIC){
				String desc = ((MethodInsnNode)ain.getNext().getNext()).desc;
				if (!desc.equals("(Ljava/lang/String;I)Ljava/lang/String;")){
					continue;
				}
				String inText = ((LdcInsnNode)ain).cst.toString();
				int inNum = OpUtil.getIntValue(ain.getNext());
				String out = deobfuscate(inText, inNum);
				method.instructions.remove(ain.getNext().getNext());
				method.instructions.remove(ain.getNext());
				method.instructions.set(ain, new LdcInsnNode(out));
			}
		}
	}

	public static String deobfuscate(final String paramText, int paramIndex) {
		final char[] inArray = paramText.toCharArray();
		final int inLength = inArray.length;
		final char[] array = inArray;
		int index = 0;
		final int mod = (4 << 5) - 1 ^ 0x20;
		char[] outArray;
		while (true) {
			outArray = array;
			if (index == inLength) {
				break;
			}
			final int indexCopy = index;
			final int charInt = (paramIndex & mod) ^ outArray[indexCopy];
			++paramIndex;
			++index;
			outArray[indexCopy] = (char) charInt;
		}
		return String.valueOf(outArray, 0, inLength).intern();
	}
}