package edu.ufl.cise.plpfa22;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plpfa22.CodeGenUtils.DynamicClassLoader;
import edu.ufl.cise.plpfa22.CodeGenUtils.GenClass;
import edu.ufl.cise.plpfa22.ast.ASTNode;
import edu.ufl.cise.plpfa22.ast.PrettyPrintVisitor;

public class FinalTests {

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

	/**
	 * Generates  classfiles for the given source program.  The classfile containing the main method has the given name and package.
	 * Other classfiles are synthetic inner classes that correspond to procedures.
	 *
	 * @param input
	 * @param className
	 * @param packageName
	 * @return  List of CodeGenUtils.GenClass records
	 * @throws Exception
	 */
	List<GenClass> compile(String input, String className, String packageName) throws Exception {
		show("*****************");
		show(input);
		ILexer lexer = CompilerComponentFactory.getLexer(input);
		ASTNode ast = CompilerComponentFactory.getParser(lexer).parse();
		ast.visit(CompilerComponentFactory.getScopeVisitor(), null);
		ast.visit(CompilerComponentFactory.getTypeInferenceVisitor(), null);
		show(ast);
		List<GenClass> classes =  (List<GenClass>) ast.visit(CompilerComponentFactory.getCodeGenVisitor(className, packageName, ""), null);
		show(classes);
		show("----------------");
		return classes;
	}



	Object loadClassesAndRunMain(List<GenClass> classes, String className) throws Exception{
		DynamicClassLoader loader = new DynamicClassLoader();
		Class<?> mainClass = loader.define(classes);
		Object[] args = new Object[1];
		return runMethod(mainClass, "main", args);
	}

	private Method findMethod(String name, Method[] methods) {
		for (Method m : methods) {
			String methodName = m.getName();
			if (name.equals(methodName))
				return m;
		}
		throw new RuntimeException("Method " + name + " not found in generated bytecode");
	}

	Object runMethod(Class<?> testClass, String methodName, Object[] args) throws Exception {
		Method[] methods = testClass.getDeclaredMethods();
		Method m = findMethod(methodName, methods);
		return m.invoke(null, args);
	}


	static boolean VERBOSE = true;
	void show(Object o) {
		if (VERBOSE) {
			System.out.println(o);
		}
	}
	void show(byte[] bytecode) {
		show(CodeGenUtils.bytecodeToString(bytecode));
	}

	void show(GenClass genClass) {
			show(genClass.className());
			show(genClass.byteCode());
	}

	void show(List<GenClass> classes) {
		for(GenClass aClass: classes) show(aClass);
	}

	void show(ASTNode ast) throws PLPException {
		if(VERBOSE) {if (ast != null) {System.out.println(PrettyPrintVisitor.AST2String(ast));}
		else {System.out.println("ast = null");}
		}
	}


	@Test
	public void test1() throws Exception{
		String input = """
				BEGIN
					IF ("FA" <= "FALSE")
					THEN
						! ((3+4)*10)%2
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
		String expected = """
				0
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test2() throws Exception{
		String input = """
				CONST a=123, b=346;
				VAR c,d;
				BEGIN
					c:=((a*b)+(a+b));
					d:=c%(a+b+c);
					! (c/d) >= 0
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
		String expected = """
				true
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test3() throws Exception{
		String input = """
				CONST d=23, e=34, f=45, g=TRUE;
				VAR a,b,c;
				BEGIN
					c:=d*e;
					b:=c>=f;
					IF b=g
					THEN
						a:="IF PASSED";
						! a
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
		String expected = """
				IF PASSED
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test4() throws Exception{
		String input = """
				BEGIN
					! (FALSE<TRUE)*(FALSE>TRUE);
					! (FALSE+TRUE)*(FALSE*TRUE);
					! (FALSE*TRUE)*(FALSE*TRUE);
					! ((FALSE+FALSE)+(FALSE*TRUE)+FALSE)+(FALSE*(FALSE+TRUE)*(FALSE*TRUE))
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				false
				false
				false
				false
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test5() throws Exception{
		String input = """
				BEGIN
					! "Hello "+"World!";
					! ("Hello "+"World!") = "Hello World!"
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				Hello World!
				true
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test6() throws Exception{
		String input = """
				CONST name="@Name", world="World";
				VAR hello;
				PROCEDURE p;
					BEGIN
						hello:="HELLO+";
						IF TRUE
						THEN
							! hello+" " + "-" + world+ " :"+" " + name
					END;
				CALL p
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				HELLO+ -World : @Name
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test7() throws Exception{
		String input = """
				CONST str="Is this", intstr = "12345", intstr2 = "456", str2="equal";
				VAR a, b, c;
				BEGIN
					a:= str+str2;
					WHILE (a = "Is this equal?")
					DO
						BEGIN
							IF ("STRiNG CoMPaRe" > "strIng cOmpArE")
							THEN
								BEGIN
									! "THIS is";
									! TRUE
								END;
							!"This is Equal!";
							a:=a+" not equal";
							!a
						END;
					b:=intstr+"6";
					WHILE ((b >= intstr2) * (b > "56") * ("123" < intstr))
					DO
						BEGIN
							IF ((b >= intstr2) + (intstr2 > "56") * ("123" < intstr))
							THEN
								BEGIN
									IF (((b >= intstr2) + (intstr2 > "56")) * ("123" < intstr))
									THEN
										! "IF 3 PASSED!";
									! "IF 2 PASSED!"
								END;
							! "IF 1 PASSED!";
							b:=b+"7"
						END
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				IF 3 PASSED!
				IF 2 PASSED!
				IF 1 PASSED!
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test8() throws Exception{
		String input = """
				CONST int=123;
				VAR int1;
				PROCEDURE p;
					CONST int1=456;
					BEGIN
						IF ((int*int1) >= (int1*int)) + ((int+int1) <= int1+int)
						THEN
							BEGIN
								! "(int*int1)" + "(int1+int) =";
								! (int*int1) + (int1+int)
							END
					END;

				PROCEDURE q;
					BEGIN
						int1:=456;
						IF ((int*int1) >= (int1*int)) * ((int+int1) <= int1+int)
						THEN
							BEGIN
								! "(int*int1)" + "(int1+int) =";
								! (int*int1) + (int1+int)
							END;
						!"Print here :)"
					END;

				BEGIN
					CALL p;
					CALL q
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				(int*int1)(int1+int) =
				56667
				(int*int1)(int1+int) =
				56667
				Print here :)
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test9() throws Exception{
		String input = """
				CONST space="SPACE";	VAR spaces;
				PROCEDURE call;
					BEGIN
						spaces:=space+"SPACES";
						! spaces >= "PACES";
						! spaces > "SPACE";
						! spaces <= "SPACE";
						! spaces < "PACE";
						! "\\n-+*- END -+*-\\n"
					END;
				BEGIN
					CALL call;
					BEGIN
						BEGIN
							IF spaces >= "PACES"
							THEN
								! "fALSE" >= "SE";
							! "1234sTRIGN" # "1234Strign";
							IF spaces > "SPACE"
							THEN
								! "Checks for spaces" > "spaces";
							! "doesntcheckforspaces" > "space";
							! "\\n-+*- END -+*-\\n"
						END;
						! (FALSE*TRUE+FALSE*FALSE+TRUE)
					END
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				true
				false
				false
				false

				-+*- END -+*-

				true
				true
				false

				-+*- END -+*-

				true
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test10() throws Exception{
		String input = """
				VAR e, f, g;
				PROCEDURE proc;
					BEGIN
						e:=e*2;
						!e;
						IF (2+2-3 <= 7)*(FALSE < TRUE)
						THEN
							IF ("2+2-3" = "2+2+3")+(222/2 >= 111)
							THEN
								! 222/2 + 111;
						!"Hello!!";
					END;
				CALL proc
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				0
				222
				Hello!!
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test11() throws Exception{
		String input = """
				PROCEDURE p;
					PROCEDURE q;;;

				PROCEDURE q;
					PROCEDURE p;;;

				PROCEDURE r;
					PROCEDURE p;
						PROCEDURE q;
							VAR r;
							BEGIN
								r:=3;
								IF r=3
								THEN
									WHILE r>=0
									DO
										BEGIN
											r:=r+r;
											!r
										END
							END
						;
						CALL q
					;
					CALL p
				;
				CALL r
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				6
				12
				24
				48
				96
				192
				384
				768
				1536
				3072
				6144
				12288
				24576
				49152
				98304
				196608
				393216
				786432
				1572864
				3145728
				6291456
				12582912
				25165824
				50331648
				100663296
				201326592
				402653184
				805306368
				1610612736
				-1073741824
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test12() throws Exception{
		String input = """
			CONST int="int", string="string", false=FALSE;
			VAR true;
			PROCEDURE p;
				VAR int;
				BEGIN
					int :="int";
					true:=TRUE;
					int:=int;
					WHILE (true = TRUE)
					DO
						BEGIN
							int:=int+int;
							IF false >= FALSE
							THEN
								BEGIN
									true:= false;
									!"TRUE 1"
								END;

							IF false # FALSE
							THEN
								BEGIN
									true:= TRUE;
									!"TRUE 2"
								END
						END
				END;
			CALL p
			.
			""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		String expected = """
				TRUE 1
				""";

		loadClassesAndRunMain(classes, className);
		assertEquals(expected, outContent.toString());
	}

	@Test
	public void test13() throws Exception{
		String input = """
				BEGIN
				//COMMENT throws Error @
				//Throw Error --> \\n -->
					IF ((123@456) >= (456*123)) + ((123+456) <= 456+123)
					THEN
						BEGIN
							! "(123*456)" + "(456+123) = "
						END
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		assertThrows(LexicalException.class, ()->{
			List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		loadClassesAndRunMain(classes, className);
		});
	}

	@Test
	public void test14() throws Exception{
		String input = """
				BEGIN
				//COMMENT throws Error SYNTAX
				//
					IF (TRUE * TRUE) + (FALSE *FALSE)
					THEN
						BEGIN
						//Throw Error --> \\n --> \\n
							! "Error Here"
							! "Nope, error here"
						END
				END
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		assertThrows(SyntaxException.class, ()->{
			List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		loadClassesAndRunMain(classes, className);
		});
	}

	@Test
	public void test15() throws Exception{
		String input = """
				! "COncat string bool err"+((FALSE+TRUE)+(FALSE*TRUE)+TRUE)
				.
				""";

		String shortClassName = "prog";
		String JVMpackageName = "edu/ufl/cise/plpfa22";
		Object[] args = new Object[1];
		String className = "edu.ufl.cise.plpfa22.prog";
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		assertThrows(TypeCheckException.class, ()->{
			List<GenClass> classes = compile(input, shortClassName, JVMpackageName);
		loadClassesAndRunMain(classes, className);
		});
	}

}

