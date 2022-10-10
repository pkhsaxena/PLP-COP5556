package edu.ufl.cise.plpfa22;

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
import edu.ufl.cise.plpfa22.ast.StatementAssign;
import edu.ufl.cise.plpfa22.ast.StatementBlock;
import edu.ufl.cise.plpfa22.ast.StatementCall;
import edu.ufl.cise.plpfa22.ast.StatementEmpty;
import edu.ufl.cise.plpfa22.ast.StatementIf;
import edu.ufl.cise.plpfa22.ast.StatementInput;
import edu.ufl.cise.plpfa22.ast.StatementOutput;
import edu.ufl.cise.plpfa22.ast.StatementWhile;
import edu.ufl.cise.plpfa22.ast.VarDec;

import java.util.Stack;

public class ScopeVisitor implements ASTVisitor {
	int ScopeNumber;
	int Nest;
	Stack<Integer> ScopeStack;
	SymbolTable symbolTable;

	public ScopeVisitor() {
		ScopeNumber = 0;
		Nest = 0;
		ScopeStack = new Stack<>();
		symbolTable = new SymbolTable();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		if (arg.equals(0))
		{
			for (ConstDec constDec : block.constDecs) {
				constDec.visit(this, arg);
			}
			for (VarDec varDec : block.varDecs) {
				varDec.visit(this, arg);
			}
			for (ProcDec procDec : block.procedureDecs) {
				procDec.visit(this, arg);
			}
		}
		else{
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
		return null;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		// insert proc iden name into symbol table
		// in first pass only insert into table
		symbolTable.put(procDec.ident.getText(), ScopeStack, procDec, true); //TODO: Change as required
		procDec.setNest(Nest);
		ScopeNumber+=1;
		Nest+=1;
		ScopeStack.push(ScopeNumber);
		procDec.block.visit(this, arg);
		Nest-=1;
		ScopeStack.pop();
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		constDec.setNest(Nest);
		symbolTable.put(constDec.ident.getText(), ScopeStack, constDec, false);
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		// TODO Auto-generated method stub
		return null;
	}

}
