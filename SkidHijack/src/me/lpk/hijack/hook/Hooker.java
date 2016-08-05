package me.lpk.hijack.hook;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.hijack.ClassModder;
import me.lpk.hijack.match.AbstractMatcher;

public class Hooker {
	/**
	 * Creates a modder that injects a single void method call to the beginning
	 * of a method.
	 * 
	 * @param matcher
	 * @param methodName
	 * @param methodDesc
	 * @param injectedMethodOwner
	 * @param injectedMethodName
	 * @return
	 */
	public static ClassModder hookMethod(AbstractMatcher<String> matcher, final String methodName, final String methodDesc, final String injectedMethodOwner,
			final String injectedMethodName) {
		return new ClassModder(matcher) {
			@Override
			public void modify(ClassNode cn) {
				for (MethodNode mn : cn.methods) {
					if (mn.name.equals(methodName) && mn.desc.equals(methodDesc)) {

					}
				}
			}
		};
	}
}
