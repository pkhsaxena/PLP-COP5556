package edu.ufl.cise.plpfa22;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ufl.cise.plpfa22.CodeGenUtils.GenClass;
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
	private List<GenClass> classList = new ArrayList<>();
	int Nest;
	Stack<String> ScopeStack;

	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc = "L" + this.fullyQualifiedClassName + ';';
		Nest = 0;
		ScopeStack = new Stack<>();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		ClassWriter classWriter = (ClassWriter) arg;
		for (ConstDec constDec : block.constDecs) {
			constDec.visit(this, null);
		}
		for (VarDec varDec : block.varDecs) {
			varDec.visit(this, classWriter);
		}
		for (ProcDec procDec : block.procedureDecs) {
			classWriter.visitInnerClass(getFullyQualifiedName() + '$' + procDec.ident.getText().toString(),
					this.fullyQualifiedClassName, procDec.ident.getText().toString(), 0);
			procDec.visit(this, null);
		}
		MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		// add instructions from statement to method
		methodVisitor.visitCode();
		block.statement.visit(this, methodVisitor);
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
		classWriter.visit(V17, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });

		// get a method visitor for the main method.
		MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();

		methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitTypeInsn(NEW, fullyQualifiedClassName);
		methodVisitor.visitInsn(DUP);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, fullyQualifiedClassName, "<init>", "()V", false);
		methodVisitor.visitMethodInsn(INVOKEVIRTUAL, fullyQualifiedClassName, "run", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
		// visit the block, passing it the methodVisitor
		program.block.visit(this, classWriter);
		// finish up the class
		classWriter.visitEnd();
		// return the bytes making up the classfile
		// return classWriter.toByteArray();
		classList.add(new GenClass(fullyQualifiedClassName, classWriter.toByteArray()));
		return classList;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		String iden = String.valueOf(statementAssign.ident.getText());
		String idenType = statementAssign.ident.getDec().getType().getJVMType();
		int IdenNest = statementAssign.ident.getNest();

		mv.visitVarInsn(ALOAD, 0);
		if(Nest!=IdenNest)
		{
			mv.visitFieldInsn(GETFIELD, getFullyQualifiedName(), "this$" + String.valueOf(Nest - 1), "L" + getParentFullyQualifiedName() + ";");
			if (Math.abs(Nest - IdenNest) > 1)
			{
				visitInvokeStatic(mv, IdenNest);
			}
		}
		statementAssign.expression.visit(this, arg);
		mv.visitFieldInsn(PUTFIELD, getFullyQualifiedName(IdenNest), iden, idenType);
		// mv.visitFieldInsn(PUTFIELD, "edu/ufl/cise/plpfa22/prog", iden, idenType);
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException { // Done
		ClassWriter classWriter = (ClassWriter) arg;
		String varName = String.valueOf(varDec.ident.getText());
		String type = varDec.getType().getJVMType();
		FieldVisitor fieldVisitor = classWriter.visitField(0, varName, type, null, null);
		fieldVisitor.visitEnd();
		return null;
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
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException { // Done
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
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException { // Done
		for (Statement s : statementBlock.statements) {
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException { // Done
		statementIf.expression.visit(this, arg);
		MethodVisitor mv = (MethodVisitor) arg;
		Label labelNumEqFalseBr = new Label();
		mv.visitJumpInsn(IFEQ, labelNumEqFalseBr); // If false, jump ahead to labelNumEqFalseBr
		statementIf.statement.visit(this, arg);
		mv.visitLabel(labelNumEqFalseBr); // After if statement, visit the label
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException { // Done
		MethodVisitor mv = (MethodVisitor) arg;
		Label guard = new Label();
		mv.visitJumpInsn(GOTO, guard);

		Label body = new Label();
		mv.visitLabel(body);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		statementWhile.statement.visit(this, arg);

		mv.visitLabel(guard);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		statementWhile.expression.visit(this, arg);
		mv.visitJumpInsn(IFNE, body);
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException { // Done
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
		MethodVisitor mv = (MethodVisitor) arg;
		String varName = String.valueOf(expressionIdent.firstToken.getText());
		String type = expressionIdent.getDec().getType().getJVMType();
		if (expressionIdent.getDec() instanceof ConstDec) {
			ConstDec Dec = (ConstDec) expressionIdent.getDec();
			if (type == "I") {
				mv.visitIntInsn(BIPUSH, (int) Dec.val);
			} else if (type == "Z") {
				if ((boolean) Dec.val == true) {
					mv.visitInsn(ICONST_1);
				} else {
					mv.visitInsn(ICONST_0);
				}
			} else if (type == "Ljava/lang/String;") {
				mv.visitLdcInsn((String) Dec.val);
			}
		} else {
			mv.visitVarInsn(ALOAD, 0);
			if(Nest != expressionIdent.getNest())
			{
				mv.visitFieldInsn(GETFIELD, getFullyQualifiedName(), "this$" + String.valueOf(Nest - 1), "L" + getFullyQualifiedName(expressionIdent.getNest()) +";");
				if (Math.abs(Nest - expressionIdent.getNest()) > 1)
				{
					visitInvokeStatic(mv, expressionIdent.getNest());
				}
			}
			mv.visitFieldInsn(GETFIELD, getFullyQualifiedName(expressionIdent.getNest()), varName, type);
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException { // Done
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitIntInsn(BIPUSH, expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException { // Done
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException { // Done
		MethodVisitor mv = (MethodVisitor) arg;
		if (expressionBooleanLit.getFirstToken().getBooleanValue() == true) {
			mv.visitInsn(ICONST_1);
		} else {
			mv.visitInsn(ICONST_0);
		}
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		Nest += 1;
		ScopeStack.push(procDec.getFirstToken().getText().toString());
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classWriter.visit(V17, ACC_PUBLIC | ACC_SUPER, getFullyQualifiedName(), null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });

		classWriter.visitNestHost(this.fullyQualifiedClassName);
		visitParentsAndSelf(classWriter);
		FieldVisitor fieldVisitor = classWriter.visitField(ACC_FINAL | ACC_SYNTHETIC,
				"this$" + String.valueOf(Nest - 1), "L" + getParentFullyQualifiedName() + ';', null, null);
		fieldVisitor.visitEnd();

		MethodVisitor methodVisitor = classWriter.visitMethod(0, "<init>", "(L" + getParentFullyQualifiedName() + ";)V",
				null, null);
		methodVisitor.visitCode();

		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitVarInsn(ALOAD, 1);
		methodVisitor.visitFieldInsn(PUTFIELD, getFullyQualifiedName(), "this$" + String.valueOf(Nest - 1),
				"L" + getParentFullyQualifiedName() + ";");
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();

		methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		methodVisitor.visitCode();


		procDec.block.visit(this, null);

		methodVisitor = classWriter.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "access$0", "(L"+ getFullyQualifiedName() +";)L"+getParentFullyQualifiedName() +";", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitFieldInsn(GETFIELD, getFullyQualifiedName(), "this$" + String.valueOf(Nest - 1), "L" + getParentFullyQualifiedName()+";");
		methodVisitor.visitInsn(ARETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
		classWriter.visitEnd();
		classList.add(new GenClass(fullyQualifiedClassName, classWriter.toByteArray()));
		Nest -= 1;
		ScopeStack.pop();
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	public String getFullyQualifiedName() {
		String rString = this.fullyQualifiedClassName;
		for (int i = 0; i < Nest; i++) {
			rString += '$' + ScopeStack.get(i);
		}
		return rString;
	}
	
	public String getFullyQualifiedName(int Nest) {
		String rString = this.fullyQualifiedClassName;
		for (int i = 0; i < Nest; i++) {
			rString += '$' + ScopeStack.get(i);
		}
		return rString;
	}

	public void visitParentsAndSelf(ClassWriter classWriter) {
		String rString = this.fullyQualifiedClassName;
		for (int i = 0; i < Nest; i++) {
			rString += '$' + ScopeStack.get(i);
			classWriter.visitInnerClass(rString, this.fullyQualifiedClassName, ScopeStack.get(i), 0);
		}
	}

	public String getParentFullyQualifiedName(int Nest) {
		String rString = this.fullyQualifiedClassName;
		for (int i = 0; i < Nest - 1; i++) {
			rString += '$' + ScopeStack.get(i);
		}
		return rString;
	}

	public String getParentFullyQualifiedName() {
		String rString = this.fullyQualifiedClassName;
		for (int i = 0; i < Nest - 1; i++) {
			rString += '$' + ScopeStack.get(i);
		}
		return rString;
	}

	public void visitInvokeStatic(MethodVisitor mv, int VarNest)
	{
		for(int i = Nest-1; i > VarNest; i--)
		{
			mv.visitMethodInsn(INVOKESTATIC, getFullyQualifiedName(i), "access$0", "(L"+getFullyQualifiedName(i)+";)L"+getParentFullyQualifiedName(i)+";", false);
		}
	}
}
