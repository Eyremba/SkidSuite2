package me.lpk.obfuscation;

import java.util.ArrayList;
import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import me.lpk.util.AccessHelper;

public class Flow {

	public static void randomGotos(MethodNode mn) {
		if (mn.name.startsWith("<") || AccessHelper.isAbstract(mn.access)) {
			return;
		}
		int instructs = mn.instructions.size();
		if (instructs < 4) {
			// Too short, don't bother.
			return;
		}
		int min = 1;
		int max = Math.max(min, instructs - 2);
		Random r = new Random();
		int randCut = (int) (min + (r.nextDouble() * (max - min)));
		AbstractInsnNode ain = mn.instructions.get(randCut);
		LabelNode labelAfter = new LabelNode();
		LabelNode labelBefore = new LabelNode();
		LabelNode labelFinal = new LabelNode();
		mn.instructions.insertBefore(ain, labelBefore);
		mn.instructions.insert(ain, labelAfter);
		mn.instructions.insert(labelAfter, labelFinal);
		// TODO: Add variety. Make opaque predicates
		// Tried before but muh stackframes
		mn.instructions.insertBefore(labelBefore, new JumpInsnNode(Opcodes.GOTO, labelAfter));
		mn.instructions.insertBefore(labelAfter, new JumpInsnNode(Opcodes.GOTO, labelFinal));
		mn.instructions.insertBefore(labelFinal, new JumpInsnNode(Opcodes.GOTO, labelBefore));
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
		mn.instructions.add(end);
		mn.instructions.add(handler);
		mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		mn.instructions.add(new InsnNode(Opcodes.ATHROW));

		LocalVariableNode exVar = new LocalVariableNode("excptn", "L" + catchType + ";", null, start, handler, index);
		TryCatchBlockNode tryBlock = new TryCatchBlockNode(start, end, handler, handleType == null ? null : ("L" + handleType + ";"));
		mn.localVariables.add(exVar);
		mn.tryCatchBlocks.add(tryBlock);
		mn.exceptions.add(catchType);
	}

	
}
