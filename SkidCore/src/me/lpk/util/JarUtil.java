package me.lpk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class JarUtil {
	/**
	 * Creates a map of <String(Class name), ClassNode> for a given jar file
	 * 
	 * @param jarFile
	 * @author Konloch (Bytecode Viewer)
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ClassNode> loadClasses(File jarFile) throws IOException {
		Map<String, ClassNode> classes = new HashMap<String, ClassNode>();
		ZipInputStream jis = new ZipInputStream(new FileInputStream(jarFile));
		ZipEntry entry;
		while ((entry = jis.getNextEntry()) != null) {
			try {
				final String name = entry.getName();
				if (name.endsWith(".class")) {
					byte[] bytes = IOUtils.toByteArray(jis);
					String cafebabe = String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);
					if (cafebabe.toLowerCase().equals("cafebabe")) {
						try {
							final ClassNode cn = ASMUtil.getNode(bytes);
							if (cn != null && cn.superName != null) {
								for (MethodNode mn : cn.methods) {
									mn.owner = cn.name;
								}
								classes.put(cn.name, cn);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				jis.closeEntry();
			}
		}
		jis.close();
		return classes;
	}

	/**
	 * Creates a map of <String(entry name), byte[]> for a given jar file
	 * 
	 * 
	 * @param jarFile
	 * @return
	 * @throws IOException
	 */
	public static Map<String, byte[]> loadNonClassEntries(File jarFile) throws IOException {
		Map<String, byte[]> entries = new HashMap<String, byte[]>();
		ZipInputStream jis = new ZipInputStream(new FileInputStream(jarFile));
		ZipEntry entry;
		while ((entry = jis.getNextEntry()) != null) {
			try {
				final String name = entry.getName();
				if (!name.endsWith(".class") && !entry.isDirectory()) {
					byte[] bytes = IOUtils.toByteArray(jis);
					entries.put(name, bytes);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				jis.closeEntry();
			}
		}
		jis.close();
		return entries;
	}

	/**
	 * Saves a map of bytes to a jar file
	 * 
	 * @param outBytes
	 * @param fileName
	 * @param makeMainClass
	 */
	public static void saveAsJar(Map<String, byte[]> outBytes, String fileName) {
		try {
			JarOutputStream out = new JarOutputStream(new java.io.FileOutputStream(fileName));
			for (String entry : outBytes.keySet()) {
				String ext = entry.contains(".") ? "" : ".class";
				out.putNextEntry(new ZipEntry(entry + ext));
				out.write(outBytes.get(entry));
				out.closeEntry();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}