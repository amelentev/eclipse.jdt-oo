import java.io.PrintWriter;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

@SuppressWarnings("nls")
public class JDTOOTest extends TestCase {
	public static void compile(String clas, String path) throws Exception {
		String file = path+"/"+clas+".java";
		System.out.println("Compiling " + file);
		assertTrue(BatchCompiler.compile(file + " -source 1.7 -d bin", new PrintWriter(System.out), new PrintWriter(System.err), null));
		assertEquals(Boolean.TRUE, Class.forName(clas).getDeclaredMethod("test", new Class[0]).invoke(null, new Object[0]));
	}
	public static void compile(String clas) throws Exception {
		compile(clas, "../../examples");
	}
	public void testValueOf() throws Exception {
		compile("ValueOf");
	}
	public void testMath() throws Exception {
		compile("Math");
	}
	public void testCmp() throws Exception {
		compile("Cmp");
	}
	public void testListIndex() throws Exception {
		compile("ListIndexGet");
		compile("ListIndexSet");
	}
	public void testMapIndex() throws Exception {
		compile("MapIndex");
	}
	public void testCompAss() throws Exception {
		compile("CompAss", "../../tests");
	}
}
