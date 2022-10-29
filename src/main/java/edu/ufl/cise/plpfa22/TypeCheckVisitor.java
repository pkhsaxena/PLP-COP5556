package edu.ufl.cise.plpfa22;

import java.util.Set;
import java.util.Stack;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;
import edu.ufl.cise.plpfa22.ast.ASTVisitor;
import edu.ufl.cise.plpfa22.ast.Block;
import edu.ufl.cise.plpfa22.ast.ConstDec;
import edu.ufl.cise.plpfa22.ast.Declaration;
import edu.ufl.cise.plpfa22.ast.Expression;
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

public class TypeCheckVisitor implements ASTVisitor {

	boolean changed = false;
	int ScopeNumber;
	int Nest;
	Stack<Integer> ScopeStack;
	SymbolTable symbolTable;

	public TypeCheckVisitor() {
		ScopeStack = new Stack<>();
		symbolTable = ScopeVisitor.symbolTable;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		for (ConstDec constDec : block.constDecs) {
			constDec.visit(this, arg);
		}
		for (ProcDec procDec : block.procedureDecs) {
			procDec.visit(this, arg);
		}
		block.statement.visit(this, arg);
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		ScopeNumber = 0;
		ScopeStack.push(ScopeNumber);
		do {
			changed = false;
			program.block.visit(this, 0);
		} while (changed);

		// after nothing can be changed, traverse again to confirm all nodes are typed
		program.block.visit(this, 1);

		return null;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		// set the type of the expression
		statementAssign.expression.visit(this, arg);
		Declaration dec = symbolTable.get(new String(statementAssign.ident.getText()), ScopeStack);
		// check if the identifier already has a type
		if (dec.getType() == null && statementAssign.expression.getType() != null) {
			setDecType(dec, statementAssign.expression.getType());
//			dec.setType(statementAssign.expression.getType());
			flip();
		}
		if (statementAssign.expression.getType() == null && dec.getType() != null) {
			setExpressionType(statementAssign.expression, dec.getType());
//			statementAssign.expression.setType(dec.getType());
			flip();
		}
		// if expression already has a type, check if the type of expression on RHS is
		// the same as the existing type of ident
		if (dec.getType() != statementAssign.expression.getType()) {
			error(dec.getType(), statementAssign.expression.getType(), statementAssign.getSourceLocation());
		}
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
//		varDec.setNest(Nest);
//		symbolTable.put(new String(varDec.ident.getText()), ScopeStack, varDec);
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		Declaration dec = symbolTable.get(new String(statementCall.ident.getText()), ScopeStack);
		if (dec.getType() == null) {
			setDecType(dec, Type.PROCEDURE);
//			dec.setType(Type.PROCEDURE);
			flip();
		} else if (dec.getType() != Type.PROCEDURE) {
			error(Type.PROCEDURE, dec.getType(), statementCall.getSourceLocation());
		}
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		Declaration dec = symbolTable.get(new String(statementInput.ident.getText()), ScopeStack);
		if (dec.getType() == Type.PROCEDURE) {
			error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), dec.getType(), statementInput.getSourceLocation());
		}
		return null;
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		statementOutput.expression.visit(this, arg);
		if (statementOutput.expression.getType() == Type.PROCEDURE) {
			error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), statementOutput.expression.getType(),
					statementOutput.expression.getSourceLocation());
		}
		return null;
	}

	@Override
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
		for (Statement s : statementBlock.statements) {
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
		if (arg.equals(0)) {
			statementIf.expression.visit(this, 2);
		}
		if (statementIf.expression.getType() != Type.BOOLEAN) {
			error(Type.BOOLEAN, statementIf.expression.getType(), statementIf.expression.getSourceLocation());
		}
		statementIf.statement.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		if (arg.equals(0)) {
			statementWhile.expression.visit(this, 2);
		}
		if (statementWhile.expression.getType() != Type.BOOLEAN) {
			error(Type.BOOLEAN, statementWhile.expression.getType(), statementWhile.expression.getSourceLocation());
		}
		statementWhile.statement.visit(this, arg);
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		expressionBinary.e0.visit(this, arg);
		expressionBinary.e1.visit(this, arg);
		Type e0_Type = expressionBinary.e0.getType();
		Type e1_Type = expressionBinary.e1.getType();
		if (e0_Type == null && e1_Type != null) {
			setExpressionType(expressionBinary, e1_Type);
		} else if (e1_Type == null && e0_Type != null) {
			setExpressionType(expressionBinary, e0_Type);
		}
		if (Kind.PLUS == expressionBinary.op.getKind()) {
			if (!Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN).contains(e0_Type) || e0_Type != e1_Type) {
				error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), e0_Type + " + " + e1_Type,
						expressionBinary.getSourceLocation());
			}
			setExpressionType(expressionBinary, e0_Type);
//			expressionBinary.setType(e0_Type);
		}

		else if (Set.of(Kind.MINUS, Kind.DIV, Kind.MOD).contains(expressionBinary.op.getKind())) {
			// if e0 or e1 already have a non-numeric type, throw error
			if (e0_Type != null && e0_Type != Type.NUMBER) {
				error(Type.NUMBER, e0_Type, expressionBinary.getSourceLocation());
			}
			if (e1_Type != null && e1_Type != Type.NUMBER) {
				error(Type.NUMBER, e1_Type, expressionBinary.getSourceLocation());
			}

			// now we are sure that either e0 or e1 is typed
			// infer the type of the expression that is not typed
			if (e0_Type == null && e1_Type != null) {
				setExpressionType(expressionBinary, e1_Type);
//				expressionBinary.e0.setType(e1_Type);
			} else if (e1_Type == null && e0_Type != null) {
				setExpressionType(expressionBinary, e0_Type);
//				expressionBinary.e1.setType(e0_Type);
			}

			expressionBinary.setType(Type.NUMBER);
		}

		else if (Kind.TIMES == expressionBinary.op.getKind()) {
			if (!Set.of(Type.NUMBER, Type.BOOLEAN).contains(e0_Type) || e0_Type != e1_Type) {
				error(Set.of(Type.NUMBER, Type.BOOLEAN), e0_Type + " ⨉ " + e1_Type,
						expressionBinary.getSourceLocation());
			}
//			setExpressionType(expressionBinary, e0_Type);
			expressionBinary.setType(e0_Type);
		}

		else if (Set.of(Kind.EQ, Kind.NEQ, Kind.LT, Kind.LE, Kind.GT, Kind.GE)
				.contains(expressionBinary.op.getKind())) {
			if (!Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN).contains(e0_Type) || e0_Type != e1_Type) {
				error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), e0_Type + " + " + e1_Type,
						expressionBinary.getSourceLocation());
			}
			expressionBinary.setType(Type.BOOLEAN);
		}

		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		if (arg.equals(2)) {
			if (expressionIdent.getType() != null && expressionIdent.getType() != Type.BOOLEAN) {
				error(Type.BOOLEAN, expressionIdent.getType(), expressionIdent.getSourceLocation());
			} else if (expressionIdent.getType() == null) {
				//FIXME need to do something else here while_do test case is failing
				setExpressionType(expressionIdent, Type.BOOLEAN);
//				expressionIdent.setType(Type.BOOLEAN);
			}
		}
		Declaration dec = symbolTable.get(new String(expressionIdent.firstToken.getText()), ScopeStack);
		if (dec.getType() == null) {
			setDecType(dec, expressionIdent.getType());
//			dec.setType(expressionIdent.getType());
			flip();
		} else {
			// if declaration is already set, that means the identifier has a type already
			// check if the new type is compatible with the already inferred type

		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		if (expressionNumLit.getType() == null) {
			setExpressionType(expressionNumLit, Type.NUMBER);
//			expressionNumLit.setType(Type.NUMBER);
			flip();
		}
		return Type.NUMBER;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		if (expressionStringLit.getType() == null) {
			setExpressionType(expressionStringLit, Type.STRING);
//			expressionStringLit.setType(Type.STRING);
			flip();
		}
		return Type.STRING;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		if (expressionBooleanLit.getType() == null) {
			setExpressionType(expressionBooleanLit, Type.BOOLEAN);
//			expressionBooleanLit.setType(Type.BOOLEAN);
			flip();
		}
		return Type.BOOLEAN;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		// insert proc iden name into symbol table
		// in first pass only insert into table
//		if (arg.equals(0)) {
//			symbolTable.put(new String(procDec.ident.getText()), ScopeStack, procDec);
//		}
		procDec.setNest(Nest);
		ScopeNumber += 1;
		Nest += 1;
		ScopeStack.push(ScopeNumber);
		procDec.block.visit(this, arg);
		Nest -= 1;
		ScopeStack.pop();
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		if (Integer.class.isInstance(constDec.val)) {
			constDec.setType(Type.NUMBER);
		} else if (String.class.isInstance(constDec.val)) {
			constDec.setType(Type.STRING);
		} else if (Boolean.class.isInstance(constDec.val)) {
			constDec.setType(Type.BOOLEAN);
		}
//		constDec.setNest(Nest);
//		symbolTable.put(new String(constDec.ident.getText()), ScopeStack, constDec);
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
//		ident.setNest(Nest);
		Declaration dec = symbolTable.get(new String(ident.firstToken.getText()), ScopeStack);
		ident.setDec(dec);
		return null;
	}

	private void error(Object expected, Object actual, SourceLocation loc) throws PLPException {
		throw new TypeCheckException("cannot assign type " + actual + " to identifier with type " + expected,
				loc.line(), loc.column());
	}

	private void flip() {
		changed = true;
	}

	private void setDecType(Declaration dec, Type type) throws PLPException {
		if (dec instanceof ConstDec) {
			error(null, type, dec.getSourceLocation());
		}
		dec.setType(type);
	}

	private void setExpressionType(Expression exp, Type type) throws PLPException {
		exp.setType(type);
	}
}
