import java.io.PrintWriter;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

public class JDTOOTest extends TestCase {
	public static void compile(String clas) throws Exception {
		String file = "../../examples/"+clas+".java";
		System.out.print("Compiling " + file + ": ");
		boolean res = BatchCompiler.compile(file + " -source 1.7 -d bin", new PrintWriter(System.out), new PrintWriter(System.err), null);
		System.out.println(res ? "ok" : "fail");
		assertEquals(Boolean.TRUE, Class.forName(clas).getDeclaredMethod("test", new Class[0]).invoke(null, new Object[0]));
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
}
