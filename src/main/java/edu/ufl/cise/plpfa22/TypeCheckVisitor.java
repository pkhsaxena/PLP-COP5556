package edu.ufl.cise.plpfa22;

import java.util.Set;
import java.util.Stack;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;
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

public class TypeCheckVisitor implements ASTVisitor {

	boolean changed = false;
	int ScopeNumber;
	int Nest;
	Stack<Integer> ScopeStack;
	SymbolTable symbolTable;

	public TypeCheckVisitor() {
		symbolTable = ScopeVisitor.symbolTable;
		ScopeNumber = 0;
		Nest = 0;
		ScopeStack = new Stack<>();
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
			ScopeNumber = 0;
			program.block.visit(this, 0);
		} while (changed);

		// after nothing can be changed, traverse again to confirm all nodes are typed
		ScopeNumber = 0;
		program.block.visit(this, 1);

		return null;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		// set the type of the expression
		statementAssign.expression.visit(this, arg);
//		Declaration dec = symbolTable.get(new String(statementAssign.ident.getText()), ScopeStack);
		Declaration dec = statementAssign.ident.getDec();
		if (arg.equals(0)) {
			if (dec instanceof ConstDec || dec instanceof ProcDec) {
				throw new TypeCheckException("ASSIGN TO PROC OR CONST", dec.getSourceLocation());
			}
			// check if the identifier already has a type
			if (dec.getType() == null && statementAssign.expression.getType() != null) {
				dec.setType(statementAssign.expression.getType());
				flip();
			}
			if (statementAssign.expression.getType() == null && dec.getType() != null) {
				statementAssign.expression.setType(dec.getType());
				flip();
			}
			// if expression already has a type, check if the type of expression on RHS is
			// the same as the existing type of ident
			if (dec.getType() != statementAssign.expression.getType()) {
				error(dec.getType(), statementAssign.expression.getType(), statementAssign.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			if (dec.getType() == null || statementAssign.expression.getType() == null) {
				throw new TypeCheckException("No type found", dec.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
//		Declaration dec = symbolTable.get(new String(statementCall.ident.getText()), ScopeStack);
		Declaration dec = statementCall.ident.getDec();
		if (arg.equals(0)) {
			if (dec.getType() == null) {
				dec.setType(Type.PROCEDURE);
				flip();
			} else if (dec.getType() != Type.PROCEDURE) {
				error(Type.PROCEDURE, dec.getType(), statementCall.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			if (dec.getType() == null) {
				throw new TypeCheckException("No type found", dec.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
//		Declaration dec = symbolTable.get(new String(statementInput.ident.getText()), ScopeStack);
		Declaration dec = statementInput.ident.getDec();
		if(dec instanceof ConstDec) {
			throw new TypeCheckException("Cannot take input in a constant variable");
		}
		if (arg.equals(0)) {
			if (dec.getType() == Type.PROCEDURE) {
				error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), dec.getType(),
						statementInput.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			if (arg.equals(1)) {
				if (dec.getType() == null) {
					throw new TypeCheckException("No type found", dec.getSourceLocation());
				}
			}
		}
		return null;
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		statementOutput.expression.visit(this, arg);
		if (arg.equals(0)) {
			if (statementOutput.expression.getType() == Type.PROCEDURE) {
				error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), statementOutput.expression.getType(),
						statementOutput.expression.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			if (statementOutput.expression.getType() == null) {
				throw new TypeCheckException("No type found", statementOutput.expression.getSourceLocation());
			}
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
			statementIf.expression.visit(this, arg);
			statementIf.statement.visit(this, arg);
			if (statementIf.expression.getType() == null) {
				statementIf.expression.setType(Type.BOOLEAN);
				flip();
			}
			if (statementIf.expression.getType() != Type.BOOLEAN) {
				error(Type.BOOLEAN, statementIf.expression.getType(), statementIf.expression.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			statementIf.expression.visit(this, arg);
			statementIf.statement.visit(this, arg);
			if (statementIf.expression.getType() == null) {
				throw new TypeCheckException("No type found", statementIf.expression.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		if (arg.equals(0)) {
			statementWhile.expression.visit(this, arg);
			statementWhile.statement.visit(this, arg);
			if (statementWhile.expression.getType() == null) {
				statementWhile.expression.setType(Type.BOOLEAN);
				flip();
			}
			if (statementWhile.expression.getType() != Type.BOOLEAN) {
				error(Type.BOOLEAN, statementWhile.expression.getType(), statementWhile.expression.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			statementWhile.expression.visit(this, arg);
			statementWhile.statement.visit(this, arg);
			if (statementWhile.expression.getType() == null) {
				throw new TypeCheckException("No type found", statementWhile.expression.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		expressionBinary.e0.visit(this, arg);
		expressionBinary.e1.visit(this, arg);
		if (!arg.equals(1)) {
			Type e0_Type = expressionBinary.e0.getType();
			Type e1_Type = expressionBinary.e1.getType();

			if (Kind.PLUS == expressionBinary.op.getKind()) {
				if (e0_Type != null && e1_Type == null) {
					expressionBinary.e1.setType(e0_Type);
					e1_Type = e0_Type;
					flip();
				} else if (e0_Type == null && e1_Type != null) {
					expressionBinary.e0.setType(e1_Type);
					e0_Type = e1_Type;
					flip();
				}
				if (e0_Type == null && expressionBinary.getType() != null) {
					e0_Type = expressionBinary.getType();
					expressionBinary.e0.setType(e0_Type);
					flip();
				}
				if (e1_Type == null && expressionBinary.getType() != null) {
					e1_Type = expressionBinary.getType();
					expressionBinary.e1.setType(e1_Type);
					flip();
				}
				if (e0_Type != null && e1_Type != null) {
					if (!Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN).contains(e0_Type) || e0_Type != e1_Type) {
						error(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN), e0_Type + " + " + e1_Type,
								expressionBinary.getSourceLocation());
					}
					if (expressionBinary.getType() == null) {
						expressionBinary.setType(e0_Type);
						flip();
					}
				}
			}

			else if (Set.of(Kind.MINUS, Kind.DIV, Kind.MOD).contains(expressionBinary.op.getKind())) {
				// if e0 or e1 already have a non-numeric type, throw error
				if (e0_Type == null) {
					expressionBinary.e0.setType(Type.NUMBER);
					e0_Type = Type.NUMBER;
					flip();
				}
				if (e1_Type == null) {
					expressionBinary.e1.setType(Type.NUMBER);
					e1_Type = Type.NUMBER;
					flip();
				}
				if (e0_Type != Type.NUMBER) {
					error(Type.NUMBER, e0_Type, expressionBinary.getSourceLocation());
				}
				if (e1_Type != Type.NUMBER) {
					error(Type.NUMBER, e1_Type, expressionBinary.getSourceLocation());
				}
				if (expressionBinary.getType() == null) {
					expressionBinary.setType(Type.NUMBER);
					flip();
				}
			}

			else if (Kind.TIMES == expressionBinary.op.getKind()) {
				if (e0_Type != null && e1_Type == null) {
					expressionBinary.e1.setType(e0_Type);
					e1_Type = e0_Type;
					flip();
				} else if (e0_Type == null && e1_Type != null) {
					expressionBinary.e0.setType(e1_Type);
					e0_Type = e1_Type;
					flip();
				}
				if (e0_Type == null && expressionBinary.getType() != null) {
					e0_Type = expressionBinary.getType();
					expressionBinary.e0.setType(e0_Type);
					flip();
				}
				if (e1_Type == null && expressionBinary.getType() != null) {
					e1_Type = expressionBinary.getType();
					expressionBinary.e1.setType(e1_Type);
					flip();
				}
				if (e0_Type != null && e1_Type != null) {
					if (!Set.of(Type.NUMBER, Type.BOOLEAN).contains(e0_Type) || e0_Type != e1_Type) {
						error(Set.of(Type.NUMBER, Type.BOOLEAN), e0_Type + " ⨉ " + e1_Type,
								expressionBinary.getSourceLocation());
					}
					if (expressionBinary.getType() == null) {
						expressionBinary.setType(e0_Type);
						flip();
					}
				}
			}

			else if (Set.of(Kind.EQ, Kind.NEQ, Kind.LT, Kind.LE, Kind.GT, Kind.GE)
					.contains(expressionBinary.op.getKind())) {
				if (e0_Type != null && e1_Type == null) {
					expressionBinary.e1.setType(e0_Type);
					e1_Type = e0_Type;
					flip();
				} else if (e0_Type == null && e1_Type != null) {
					expressionBinary.e0.setType(e1_Type);
					e0_Type = e1_Type;
					flip();
				}
				if ((e0_Type != null && e1_Type != null)
						&& (!Set.of(Type.NUMBER, Type.BOOLEAN, Type.STRING).contains(e0_Type) || e0_Type != e1_Type)) {
					error(Set.of(Type.NUMBER, Type.BOOLEAN, Type.STRING), e0_Type + " ⨉ " + e1_Type,
							expressionBinary.getSourceLocation());
				}
				if (expressionBinary.getType() == null) {
					expressionBinary.setType(Type.BOOLEAN);
					flip();
				}
			}
			if (expressionBinary.e0 instanceof ExpressionIdent && symbolTable
					.get(new String(expressionBinary.e0.firstToken.getText()), ScopeStack) instanceof Declaration) {
				symbolTable.get(new String(expressionBinary.e0.firstToken.getText()), ScopeStack).setType(e0_Type);
			}
			if (expressionBinary.e1 instanceof ExpressionIdent && symbolTable
					.get(new String(expressionBinary.e0.firstToken.getText()), ScopeStack) instanceof Declaration) {
				symbolTable.get(new String(expressionBinary.e1.firstToken.getText()), ScopeStack).setType(e1_Type);
			}
		} else if (arg.equals(1)) {
			if (expressionBinary.getType() == null) {
				throw new TypeCheckException("No type found", expressionBinary.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
//		Declaration dec = symbolTable.get(new String(expressionIdent.firstToken.getText()), ScopeStack);
		Declaration dec = expressionIdent.getDec();
		if (arg.equals(0)) {
			if (dec.getType() == null && expressionIdent.getType() != null) {
				dec.setType(expressionIdent.getType());
				flip();
			}
			if (expressionIdent.getType() == null && dec.getType() != null) {
				expressionIdent.setType(dec.getType());
				flip();
			}
		} else if (arg.equals(1)) {
			if (expressionIdent.getType() == null) {
				throw new TypeCheckException("No type found", expressionIdent.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		if (expressionNumLit.getType() == null) {
			expressionNumLit.setType(Type.NUMBER);
			flip();
		}
		return Type.NUMBER;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		if (expressionStringLit.getType() == null) {
			expressionStringLit.setType(Type.STRING);
			flip();
		}
		return Type.STRING;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		if (expressionBooleanLit.getType() == null) {
			expressionBooleanLit.setType(Type.BOOLEAN);
			flip();
		}
		return Type.BOOLEAN;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		Declaration dec = symbolTable.get(new String(procDec.ident.getText()), ScopeStack);
		if (arg.equals(0)) {
			if (dec.getType() == null) {
				dec.setType(Type.PROCEDURE);
				flip();
			} else if (dec.getType() != Type.PROCEDURE) {
				error(Type.PROCEDURE, dec.getType(), dec.getSourceLocation());
			}
		} else if (arg.equals(1)) {
			if (dec.getType() == null) {
				throw new TypeCheckException("No type found", dec.getSourceLocation());
			}
		}
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
		Declaration dec = symbolTable.get(new String(constDec.ident.getText()), ScopeStack);
		if (arg.equals(0)) {
			if (dec.getType() == null) {
				Object val = constDec.val;
				if (val instanceof Integer) {
					dec.setType(Type.NUMBER);
				} else if (val instanceof String) {
					dec.setType(Type.STRING);
				} else if (val instanceof Boolean) {
					dec.setType(Type.BOOLEAN);
				}
				flip();
			}
		} else if (arg.equals(1)) {
			if (dec.getType() == null) {
				throw new TypeCheckException("No type found", dec.getSourceLocation());
			}
		}
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		return null;
	}

	private void error(Object expected, Object actual, SourceLocation loc) throws PLPException {
		throw new TypeCheckException("cannot assign type " + actual + " to identifier with type " + expected, loc);
	}

	private void flip() {
		changed = true;
	}
}
