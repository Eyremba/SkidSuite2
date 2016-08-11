package me.lpk.obfuscation;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.lpk.util.AccessHelper;

public class AntiDecompile {
	public static final String s = getMassive();

	public static void massiveLdc(MethodNode mn) {
		if (mn.name.contains("<") || AccessHelper.isAbstract(mn.access)) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			mn.instructions.insert(new InsnNode(Opcodes.POP2));
			mn.instructions.insert(new LdcInsnNode(s));
			mn.instructions.insert(new InsnNode(Opcodes.POP));
			mn.instructions.insert(new InsnNode(Opcodes.SWAP));
			mn.instructions.insert(new InsnNode(Opcodes.POP));
			mn.instructions.insert(new LdcInsnNode(s));
			mn.instructions.insert(new LdcInsnNode(s));
			mn.instructions.insert(new LdcInsnNode(s));
		}
	}
	
	public static void duplicateVars(MethodNode mn) {
		if (AccessHelper.isAbstract(mn.access)) {
			return;
		}
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.VAR_INSN) {
				VarInsnNode vin = (VarInsnNode) ain;
				if (vin.getOpcode() == Opcodes.ASTORE) {
					mn.instructions.insertBefore(vin, new InsnNode(Opcodes.DUP));
					mn.instructions.insertBefore(vin, new InsnNode(Opcodes.ACONST_NULL));
					mn.instructions.insertBefore(vin, new InsnNode(Opcodes.SWAP));
					AbstractInsnNode next = vin.getNext();
					// Astore
					mn.instructions.insertBefore(next, new InsnNode(Opcodes.POP));
					mn.instructions.insertBefore(next, new VarInsnNode(Opcodes.ASTORE, vin.var));
				}
			}
		}
	}

	private static String getMassive() {
		StringBuffer sb = new StringBuffer();
		while (sb.length() < 65536 - 1) {
			sb.append("z");
		}
		return sb.toString();
	}

	public static void badPop(MethodNode mn) {
		if (AccessHelper.isAbstract(mn.access)) {
			return;
		}
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			int op = ain.getOpcode();
			if (op == Opcodes.ALOAD || op == Opcodes.ILOAD || op == Opcodes.FLOAD) {
				VarInsnNode vin = (VarInsnNode) ain;
				mn.instructions.insert(vin, new InsnNode(Opcodes.POP2));
				mn.instructions.insertBefore(vin, new VarInsnNode(op, vin.var));
				mn.instructions.insertBefore(vin, new VarInsnNode(op, vin.var));
			}
		}
	}

	/**
	 * Destroys Fernflower
	 * @param cn
	 */
	public static void retObjErr(ClassNode cn) {
		String mthdN = "getFukt", mthdR = "()Ljava/lang/Object;";
		String catchType = "java/lang/Exception";
		for (MethodNode mn : cn.methods) {
			if (mn.name.contains("<") || AccessHelper.isAbstract(mn.access) || mn.instructions.size() < 4) {
				continue;
			}
			AbstractInsnNode last = mn.instructions.getLast();
			while (last.getType() == AbstractInsnNode.FRAME) {
				last = last.getPrevious();
			}
			LabelNode beforeInvoke = new LabelNode();
			LabelNode afterInvoke = new LabelNode();
			LabelNode beforeAthrow = new LabelNode();
			LabelNode afterAthrow = new LabelNode();
			LabelNode handler = new LabelNode();
			if (mn.localVariables == null) {
				mn.localVariables = new ArrayList<LocalVariableNode>(5);
			}
			int index = mn.localVariables.size();
			LocalVariableNode exVarInvoke = new LocalVariableNode("excptnInvoke", "L" + catchType + ";", null, beforeInvoke, afterInvoke, index);
			LocalVariableNode exVarThrow = new LocalVariableNode("excptnThrow", "L" + catchType + ";", null, beforeAthrow, afterAthrow, index + 1);
			TryCatchBlockNode tryBlockInvoke = new TryCatchBlockNode(beforeInvoke,afterInvoke, handler, null);
			TryCatchBlockNode tryBlockThrow = new TryCatchBlockNode(beforeAthrow,afterAthrow, handler, null);
			mn.instructions.insertBefore(last, beforeInvoke);
			mn.instructions.insertBefore(last, new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, mthdN, mthdR, false));
			mn.instructions.insertBefore(last, afterInvoke);
			mn.instructions.insertBefore(last, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/NullPointerException"));
			mn.instructions.insertBefore(last, beforeAthrow);
			mn.instructions.insertBefore(last, new InsnNode(Opcodes.ATHROW));
			mn.instructions.insertBefore(last, afterAthrow);
			mn.instructions.insertBefore(last, handler);
			mn.tryCatchBlocks.add(tryBlockInvoke);
			mn.tryCatchBlocks.add(tryBlockThrow);
			mn.localVariables.add(exVarInvoke);
			mn.localVariables.add(exVarThrow);

			if (!mn.exceptions.contains(catchType)){
				mn.exceptions.add(catchType);
			}
		}
		int acc = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
		MethodNode objMethod = new MethodNode(acc, mthdN, mthdR, null, new String[] {});
		objMethod.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/NullPointerException"));
		objMethod.instructions.add(new InsnNode(Opcodes.DUP));
		objMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V", false));
		objMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
		cn.methods.add(objMethod);

	}
	
	public static void addTryCatch(MethodNode mn, String catchType, String handleType) {
		if (mn.name.startsWith("<") || AccessHelper.isAbstract(mn.access)) {
			return;
		}
		LabelNode start = new LabelNode();
		LabelNode handler = new LabelNode();
		LabelNode end = new LabelNode();
		if (mn.localVariables == null) {
			mn.localVariables = new ArrayList<LocalVariableNode>(5);
		}
		int index = mn.localVariables.size();
		mn.instructions.insert(start);
		mn.instructions.add(handler);
		mn.instructions.add(new InsnNode(Opcodes.NOP));
		mn.instructions.add(end);
		mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		mn.instructions.add(new InsnNode(Opcodes.ATHROW));

		LocalVariableNode exVar = new LocalVariableNode("excptn", "L" + catchType + ";", null, start, handler, index);
		TryCatchBlockNode tryBlock = new TryCatchBlockNode(start, end, handler, handleType == null ? null : ("L" + handleType + ";"));
		mn.localVariables.add(exVar);
		mn.tryCatchBlocks.add(tryBlock);
		mn.exceptions.add(catchType);
	}
}
