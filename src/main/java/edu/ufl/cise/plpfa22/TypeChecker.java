package edu.ufl.cise.plpfa22;

import java.util.Stack;

import edu.ufl.cise.plpfa22.ast.ASTVisitor;
import edu.ufl.cise.plpfa22.ast.Block;
import edu.ufl.cise.plpfa22.ast.ConstDec;
import edu.ufl.cise.plpfa22.ast.Declaration;
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

public class TypeChecker implements ASTVisitor {

	boolean changed = false;

	public TypeChecker() {

	}

	int ScopeNumber;
	int Nest;
	Stack<Integer> ScopeStack;
	SymbolTable symbolTable;

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		if (arg.equals(0)) {
			for (ConstDec constDec : block.constDecs) {
				constDec.visit(this, arg);
			}
			for (VarDec varDec : block.varDecs) {
				varDec.visit(this, arg);
			}
			for (ProcDec procDec : block.procedureDecs) {
				procDec.visit(this, arg);
			}
		} else {
			for (ProcDec procDec : block.procedureDecs) {
				procDec.visit(this, arg);
			}
			block.statement.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		ScopeStack.push(ScopeNumber);
		program.block.visit(this, 0); // Make the block traverse only proc decs to insert into symbol table

		ScopeNumber = 0; // Reset Scope number
		program.block.visit(this, 1); // Properly traverse the entire ast
		do {
			changed = false;
			program.block.visit(this, 1);
		} while (changed);

		// after nothing can be changed, traverse again to see if all nodes are typed or
		// not

		return null;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		statementAssign.ident.setNest(Nest);
		Declaration dec = symbolTable.get(new String(statementAssign.ident.getText()), ScopeStack);
		statementAssign.ident.setDec(dec);
		statementAssign.expression.visit(this, arg);
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		varDec.setNest(Nest);
		symbolTable.put(new String(varDec.ident.getText()), ScopeStack, varDec);
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		statementCall.ident.setNest(Nest);
		Declaration dec = symbolTable.get(new String(statementCall.ident.getText()), ScopeStack);
		statementCall.ident.setDec(dec);
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		statementInput.ident.setNest(Nest);
		Declaration dec = symbolTable.get(new String(statementInput.ident.getText()), ScopeStack);
		statementInput.ident.setDec(dec);
		return null;
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		statementOutput.expression.visit(this, arg);
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
		statementIf.expression.visit(this, arg);
		statementIf.statement.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		statementWhile.expression.visit(this, arg);
		statementWhile.statement.visit(this, arg);
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		expressionBinary.e0.visit(this, arg);
		expressionBinary.e1.visit(this, arg);
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		expressionIdent.setNest(Nest);
		Declaration dec = symbolTable.get(new String(expressionIdent.firstToken.getText()), ScopeStack);
		if (expressionIdent.getType() == null && dec != null) {
			expressionIdent.setType(dec.getType());
			changed = true;
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		if (expressionNumLit.getType() == null) {
			expressionNumLit.setType(Type.NUMBER);
			changed = true;
		}
		return Type.NUMBER;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		if (expressionStringLit.getType() == null) {
			expressionStringLit.setType(Type.STRING);
			changed = true;
		}
		return Type.STRING;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		if (expressionBooleanLit.getType() == null) {
			expressionBooleanLit.setType(Type.BOOLEAN);
			changed = true;
		}
		return Type.BOOLEAN;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		// insert proc iden name into symbol table
		// in first pass only insert into table
		if (arg.equals(0)) {
			symbolTable.put(new String(procDec.ident.getText()), ScopeStack, procDec);
		}
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
		constDec.setNest(Nest);
		symbolTable.put(new String(constDec.ident.getText()), ScopeStack, constDec);
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		ident.setNest(Nest);
		Declaration dec = symbolTable.get(new String(ident.firstToken.getText()), ScopeStack);
		ident.setDec(dec);
		return null;
	}

}
