package edu.ufl.cise.plpfa22;

import java.util.List;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;
import edu.ufl.cise.plpfa22.ast.ASTNode;
import edu.ufl.cise.plpfa22.ast.Expression;
import edu.ufl.cise.plpfa22.ast.ExpressionBinary;
import edu.ufl.cise.plpfa22.ast.ExpressionBooleanLit;
import edu.ufl.cise.plpfa22.ast.ExpressionIdent;
import edu.ufl.cise.plpfa22.ast.ExpressionNumLit;
import edu.ufl.cise.plpfa22.ast.ExpressionStringLit;
import edu.ufl.cise.plpfa22.ast.Ident;
import edu.ufl.cise.plpfa22.ast.Statement;
import edu.ufl.cise.plpfa22.ast.StatementAssign;
import edu.ufl.cise.plpfa22.ast.StatementBlock;
import edu.ufl.cise.plpfa22.ast.StatementCall;
import edu.ufl.cise.plpfa22.ast.StatementEmpty;
import edu.ufl.cise.plpfa22.ast.StatementIf;
import edu.ufl.cise.plpfa22.ast.StatementInput;
import edu.ufl.cise.plpfa22.ast.StatementOutput;
import edu.ufl.cise.plpfa22.ast.StatementWhile;
import edu.ufl.cise.plpfa22.ast.SyntaxException;

public class Parser implements IParser {

	private final ILexer lexer;
	private IToken currentToken;

	public Parser(ILexer lexer) throws LexicalException {
		this.lexer = lexer;
		currentToken = lexer.next();
	}

	@Override
	public ASTNode parse() throws PLPException {
		// TODO implement
		return null;
	}

	private void program() throws LexicalException, SyntaxException {
		block();
		if (isKind(Kind.DOT)) {
			consume();
		} else {
			error();
		}
	}

	private void block() throws LexicalException, SyntaxException {
		while (isKind(Kind.KW_CONST)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
				if (isKind(Kind.EQ)) {
					consume();
					constVal();
					while (isKind(Kind.COMMA)) {
						consume();
						if (isKind(Kind.IDENT)) {
							consume();
							if (isKind(Kind.EQ)) {
								consume();
								constVal();
							} else {
								error();
							}
						} else {
							error();
						}
					}
				} else {
					error();
				}
			} else {
				error();
			}
			if (isKind(Kind.SEMI)) {
				consume();
			} else {
				error();
			}
		}

		while (isKind(Kind.KW_VAR)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
				while (isKind(Kind.COMMA)) {
					consume();
					if (isKind(Kind.IDENT)) {
						consume();
					} else {
						error();
					}
				}
			} else {
				error();
			}
			if (isKind(Kind.SEMI)) {
				consume();
			} else {
				error();
			}
		}

		while (isKind(Kind.KW_PROCEDURE)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
				if (isKind(Kind.SEMI)) {
					consume();
					block();
					if (isKind(Kind.SEMI)) {
						consume();
					} else {
						error();
					}
				} else {
					error();
				}
			} else {
				error();
			}
		}

		statement();
	}

	private Statement statement() throws LexicalException, SyntaxException {
		IToken firstToken = currentToken;
		Statement s = null;
		// <ident> := <expression
		if (isKind(Kind.IDENT)) {
			Ident var = new Ident(firstToken);
			Expression e = null;
			consume();
			if (isKind(Kind.ASSIGN)) {
				consume();
				e = exp();
				s = new StatementAssign(firstToken, var, e);
			} else {
				error();
			}
		}
		// CALL <ident>
		else if (isKind(Kind.KW_CALL)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
				Ident ident = new Ident(firstToken);
				s = new StatementCall(firstToken, ident);
			} else {
				error();
			}
		}
		// ? <ident>
		else if (isKind(Kind.QUESTION)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
				Ident name = new Ident(firstToken);
				s = new StatementInput(firstToken, name);
			} else {
				error();
			}
		}
		// ! <expression>
		else if (isKind(Kind.BANG)) {
			consume();
			Expression e = exp();
			s = new StatementOutput(firstToken, e);
		}
		// BEGIN <statement> ( ; <statement> )* END
		else if (isKind(Kind.KW_BEGIN)) { //TODO: Verify logic.
			consume();
			List<Statement> statements = new ArrayList<Statement>();
			Statement listItem = null;
			listItem = statement();
			statements.add(listItem);
			while (isKind(Kind.SEMI)) {
				consume();
				listItem = statement();
				statements.add(listItem);
			}
			if (isKind(Kind.KW_END)) {
				consume();
				s = new StatementBlock(firstToken, statements);
			} else {
				error();
			}
		}
		// IF <expression> THEN <statement>
		else if (isKind(Kind.KW_IF)) {
			consume();
			Expression condition= exp();
			if (isKind(Kind.KW_THEN)) {
				consume();
				Statement statement = statement();
				s = new StatementIf(firstToken, condition, statement);
			} else {
				error();
			}
		}
		// WHILE <expression> DO <statement>
		else if (isKind(Kind.KW_WHILE)) {
			consume();
			Expression expression = exp();
			if (isKind(Kind.KW_DO)) {
				consume();
				Statement statement = statement();
				s = new StatementWhile(firstToken, expression, statement);
			} else {
				error();
			}
		}

		else {
			s = new StatementEmpty(firstToken);
		}
		return s;
	}

	private Expression exp() throws LexicalException, SyntaxException {
		IToken firsToken = currentToken;
		Expression left = null;
		Expression right = null;
		left = addExp();
		while (isKind(Kind.LT) || isKind(Kind.LE) || isKind(Kind.GT) || isKind(Kind.GE) || isKind(Kind.EQ)
				|| isKind(Kind.NEQ)) {
			IToken op = currentToken;
			consume();
			right = addExp();
			left = new ExpressionBinary(firsToken, left, op, right);
		}
		return left;
	}

	private Expression addExp() throws LexicalException, SyntaxException {
		IToken firstToken = currentToken;
		Expression left = null;
		Expression right = null;
		left = mulExp();
		while (isKind(Kind.PLUS) || isKind(Kind.MINUS)) {
			IToken op = currentToken;
			consume();
			right = mulExp();
			left = new ExpressionBinary(firstToken, left, op, right);
		}
		return left;
	}

	private Expression mulExp() throws LexicalException, SyntaxException {
		IToken firstToken = currentToken;
		Expression left = null;
		Expression right = null;
		left = primaryExp();
		while (isKind(Kind.TIMES) || isKind(Kind.DIV) || isKind(Kind.MOD)) {
			IToken op = currentToken;
			consume();
			right = primaryExp();
			left = new ExpressionBinary(firstToken, left, op, right);
		}
		return left;
	}

	private Expression primaryExp() throws LexicalException, SyntaxException {
		IToken firstToken = currentToken;
		Expression e = null;
		if (isKind(Kind.IDENT)) {
			consume();
			e = new ExpressionIdent(firstToken);
		} else if (isKind(Kind.NUM_LIT) || isKind(Kind.STRING_LIT) || isKind(Kind.BOOLEAN_LIT)) {
			e = constVal();
		} else if (isKind(Kind.LPAREN)) {
			consume();
			e = exp();
			if (isKind(Kind.RPAREN)) {
				consume();
			} else {
				error();
			}
		} else {
			error();
		}
		return e;
	}

	private Expression constVal() throws LexicalException {
		IToken firstToken = currentToken;
		Expression e = null;
		if (isKind(Kind.NUM_LIT)) {
			consume();
			e = new ExpressionNumLit(firstToken);
		} else if (isKind(Kind.STRING_LIT)) {
			consume();
			e = new ExpressionStringLit(firstToken);
		} else if (isKind(Kind.BOOLEAN_LIT)) {
			consume();
			e = new ExpressionBooleanLit(firstToken);
		}
		return e;
	}

	private boolean isKind(Kind kind) {
		return kind.equals(currentToken.getKind());
	}

	private IToken consume() throws LexicalException {
		// TODO: Remove return? Not being used?
		currentToken = lexer.next();
		return currentToken;
	}

	private void error() throws SyntaxException {
		SourceLocation loc = currentToken.getSourceLocation();
		throw new SyntaxException("Parser encountered an error", loc.line(), loc.column());
	}
}
