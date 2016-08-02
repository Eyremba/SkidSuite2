package me.lpk.obfuscation;

import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.analysis.StackFrame;
import me.lpk.analysis.StackUtil;

public class Flow {

	public static void randomGotos(ClassNode cn, MethodNode mn) {
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
		StackFrame[] frames = StackUtil.getFrames(mn);
		mn.instructions.insertBefore(ain, labelBefore);
		mn.instructions.insert(ain, labelAfter);
		mn.instructions.insert(labelAfter, labelFinal);
		mn.instructions.insert(frames[0].toFrame());

		mn.instructions.insertBefore(labelBefore, gotoLogic(mn, labelAfter));
		mn.instructions.insertBefore(labelAfter, gotoLogic(mn, labelFinal));
		mn.instructions.insertBefore(labelFinal, gotoLogic(mn, labelBefore));

	}

	private static InsnList gotoLogic(MethodNode mn, LabelNode label) {
		InsnList list = new InsnList();
		// TODO: Randomization
		 list.add(new JumpInsnNode(Opcodes.GOTO, label));
		return list;
	}
}
