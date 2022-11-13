package edu.ufl.cise.plpfa22;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.ASTVisitor;
import edu.ufl.cise.plpfa22.ast.Block;
import edu.ufl.cise.plpfa22.ast.ConstDec;
import edu.ufl.cise.plpfa22.ast.ExpressionBinary;
import edu.ufl.cise.plpfa22.ast.ExpressionBooleanLit;
import edu.ufl.cise.plpfa22.ast.ExpressionIdent;
import edu.ufl.cise.plpfa22.ast.ExpressionNumLit;
import edu.ufl.cise.plpfa22.ast.ExpressionStringLit;
import edu.ufl.cise.plpfa22.ast.Ident;
import edu.ufl.cise.plpfa22.ast.ProcDec;
import edu.ufl.cise.plpfa22.ast.Program;
import edu.ufl.cise.plpfa22.ast.Statement;
import edu.ufl.cise.plpfa22.ast.StatementAssign;
import edu.ufl.cise.plpfa22.ast.StatementBlock;
import edu.ufl.cise.plpfa22.ast.StatementCall;
import edu.ufl.cise.plpfa22.ast.StatementEmpty;
import edu.ufl.cise.plpfa22.ast.StatementIf;
import edu.ufl.cise.plpfa22.ast.StatementInput;
import edu.ufl.cise.plpfa22.ast.StatementOutput;
import edu.ufl.cise.plpfa22.ast.StatementWhile;
import edu.ufl.cise.plpfa22.ast.Types.Type;
import edu.ufl.cise.plpfa22.ast.VarDec;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	final String packageName;
	final String className;
	final String sourceFileName;
	final String fullyQualifiedClassName;
	final String classDesc;

	ClassWriter classWriter;

	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc = "L" + this.fullyQualifiedClassName + ';';
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException { // DONE?
		MethodVisitor methodVisitor = (MethodVisitor) arg;
		methodVisitor.visitCode();
		for (ConstDec constDec : block.constDecs) {
			constDec.visit(this, null);
		}
		for (VarDec varDec : block.varDecs) {
			varDec.visit(this, methodVisitor);
		}
		for (ProcDec procDec : block.procedureDecs) {
			procDec.visit(this, null);
		}
		// add instructions from statement to method
		block.statement.visit(this, arg);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
		return null;

	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException { // DONE?
		// create a classWriter and visit it
		classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// Hint: if you get failures in the visitMaxs, try creating a ClassWriter with 0
		// instead of ClassWriter.COMPUTE_FRAMES. The result will not be a valid
		// classfile,
		// but you will be able to print it so you can see the instructions. After
		// fixing,
		// restore ClassWriter.COMPUTE_FRAMES
		classWriter.visit(V17, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", null);

		// get a method visitor for the main method.
		MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V",
				null, null);
		// visit the block, passing it the methodVisitor
		program.block.visit(this, methodVisitor);
		// finish up the class
		classWriter.visitEnd();
		// return the bytes making up the classfile
		return classWriter.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException { // DONE?
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		statementOutput.expression.visit(this, arg);
		Type etype = statementOutput.expression.getType();
		String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		String printlnSig = "(" + JVMType + ")V";
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
		return null;
	}

	@Override
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException { // DONE?
		for (Statement s : statementBlock.statements) {
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
		statementIf.expression.visit(this, arg);
		MethodVisitor mv = (MethodVisitor) arg;
		Label labelNumEqFalseBr = new Label();
		mv.visitJumpInsn(IFEQ, labelNumEqFalseBr); // If false, jump ahead to labelNumEqFalseBr
		statementIf.statement.visit(this, arg);
		mv.visitLabel(labelNumEqFalseBr); // After if statement, visit the label
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		Type argType = expressionBinary.e0.getType();
		Kind op = expressionBinary.op.getKind();
		switch (argType) {
			case NUMBER -> {
				expressionBinary.e0.visit(this, arg);
				expressionBinary.e1.visit(this, arg);
				switch (op) {
					case PLUS -> mv.visitInsn(IADD);
					case MINUS -> mv.visitInsn(ISUB);
					case TIMES -> mv.visitInsn(IMUL);
					case DIV -> mv.visitInsn(IDIV);
					case MOD -> mv.visitInsn(IREM);
					case EQ -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPNE, labelNumEqFalseBr); // If they are not equal, jump ahead to
																		// labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // They are equal load True -> 1
						Label labelPostNumEq = new Label(); // If they were equal we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case NEQ -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPEQ, labelNumEqFalseBr); // If they are equal, jump ahead to
																		// labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // They are not equal load True -> 1
						Label labelPostNumEq = new Label(); // If they were not equal we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case LT -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPGE, labelNumEqFalseBr); // If a >= b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a < b load True -> 1
						Label labelPostNumEq = new Label(); // If a < b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case LE -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPGT, labelNumEqFalseBr); // If a > b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a <= b load True -> 1
						Label labelPostNumEq = new Label(); // If a <= b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case GT -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPLE, labelNumEqFalseBr); // If a <= b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a > b load True -> 1
						Label labelPostNumEq = new Label(); // If a > b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case GE -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPLT, labelNumEqFalseBr); // If a < b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a >= b load True -> 1
						Label labelPostNumEq = new Label(); // If a >= b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
					}
				}
				;
			}
			case BOOLEAN -> {
				expressionBinary.e0.visit(this, arg);
				expressionBinary.e1.visit(this, arg);
				switch (op) {
					case PLUS -> {
						mv.visitInsn(IOR);
					}
					case TIMES -> {
						mv.visitInsn(IAND);
					}
					case EQ -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPNE, labelNumEqFalseBr); // If a != b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a == b load True -> 1
						Label labelPostNumEq = new Label(); // If a == b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case NEQ -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPEQ, labelNumEqFalseBr); // If a == b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a != b load True -> 1
						Label labelPostNumEq = new Label(); // If a != b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case LT -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPGE, labelNumEqFalseBr); // If a >= b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a < b load True -> 1
						Label labelPostNumEq = new Label(); // If a < b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case LE -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPGT, labelNumEqFalseBr); // If a > b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a <= b load True -> 1
						Label labelPostNumEq = new Label(); // If a <= b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case GT -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPLE, labelNumEqFalseBr); // If a <= b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a > b load True -> 1
						Label labelPostNumEq = new Label(); // If a > b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case GE -> {
						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IF_ICMPLT, labelNumEqFalseBr); // If a < b, jump ahead to labelNumEqFalseBr
						mv.visitInsn(ICONST_1); // a >= b load True -> 1
						Label labelPostNumEq = new Label(); // If a >= b we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary BOOLEAN");
					}
				}
			}
			case STRING -> {
				switch (op) {
					case PLUS -> {
						expressionBinary.e0.visit(this, arg);
						expressionBinary.e1.visit(this, arg);
						String concatSig = "(Ljava/lang/String;)Ljava/lang/String;";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", concatSig, false);
					}
					case EQ -> {
						expressionBinary.e0.visit(this, arg);
						expressionBinary.e1.visit(this, arg);
						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);
					}
					case NEQ -> {
						expressionBinary.e0.visit(this, arg);
						expressionBinary.e1.visit(this, arg);
						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);

						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IFNE, labelNumEqFalseBr); // If val != 0, jump ahead to labelNumEqFalseBr (ie
																	// if equals method returned true(1))
						mv.visitInsn(ICONST_1); // a != b load True -> 1 (neq)
						Label labelPostNumEq = new Label(); // If a != b (val==0) we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
					}
					case LT -> {
						expressionBinary.e1.visit(this, arg);
						expressionBinary.e0.visit(this, arg);
						mv.visitInsn(DUP2);
						String startsWithSig = "(Ljava/lang/String;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", startsWithSig, false);

						mv.visitInsn(DUP_X2);
						mv.visitInsn(POP);

						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);

						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IFNE, labelNumEqFalseBr); // If val != 0, jump ahead to labelNumEqFalseBr (ie
																	// if equals method returned true(1))
						mv.visitInsn(ICONST_1); // a != b load True -> 1 (neq)
						Label labelPostNumEq = new Label(); // If a != b (val==0) we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
						mv.visitInsn(IAND);
					}
					case LE -> {
						expressionBinary.e1.visit(this, arg);
						expressionBinary.e0.visit(this, arg);
						mv.visitInsn(DUP2);
						String startsWithSig = "(Ljava/lang/String;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", startsWithSig, false);

						mv.visitInsn(DUP_X2);
						mv.visitInsn(POP);

						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);

						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IFEQ, labelNumEqFalseBr); // If val != 0, jump ahead to labelNumEqFalseBr (ie
																	// if equals method returned true(1))
						mv.visitInsn(ICONST_1); // a != b load True -> 1 (neq)
						Label labelPostNumEq = new Label(); // If a != b (val==0) we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
						mv.visitInsn(IOR);
					}
					case GT -> {
						expressionBinary.e0.visit(this, arg);
						expressionBinary.e1.visit(this, arg);
						mv.visitInsn(DUP2);
						String endsWithSig = "(Ljava/lang/String;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", endsWithSig, false);

						mv.visitInsn(DUP_X2);
						mv.visitInsn(POP);

						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);

						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IFNE, labelNumEqFalseBr); // If val != 0, jump ahead to labelNumEqFalseBr (ie
																	// if equals method returned true(1))
						mv.visitInsn(ICONST_1); // a != b load True -> 1 (neq)
						Label labelPostNumEq = new Label(); // If a != b (val==0) we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
						mv.visitInsn(IAND);
					}
					case GE -> {
						expressionBinary.e0.visit(this, arg);
						expressionBinary.e1.visit(this, arg);
						mv.visitInsn(DUP2);
						String endsWithSig = "(Ljava/lang/String;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", endsWithSig, false);

						mv.visitInsn(DUP_X2);
						mv.visitInsn(POP);

						String equalsSig = "(Ljava/lang/Object;)Z";
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", equalsSig, false);

						Label labelNumEqFalseBr = new Label();
						mv.visitJumpInsn(IFEQ, labelNumEqFalseBr); // If val != 0, jump ahead to labelNumEqFalseBr (ie
																	// if equals method returned true(1))
						mv.visitInsn(ICONST_1); // a != b load True -> 1 (neq)
						Label labelPostNumEq = new Label(); // If a != b (val==0) we need to skip the next section
						mv.visitJumpInsn(GOTO, labelPostNumEq); // Skip next section of loading False -> 0
						mv.visitLabel(labelNumEqFalseBr); // If we are not going to the GOTO, visit the label
						mv.visitInsn(ICONST_0); // Load False -> 0
						mv.visitLabel(labelPostNumEq); // Goto the position after loading, ie GOTO location.
						mv.visitInsn(IOR);
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary BOOLEAN");
					}
				}
				;
			}
			default -> {
				throw new IllegalStateException("code gen bug in visitExpressionBinary");
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException { // Done?
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException { // Done?
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException { // Done?
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

}
