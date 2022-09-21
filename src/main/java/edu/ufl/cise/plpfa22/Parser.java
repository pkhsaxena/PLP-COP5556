package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;
import edu.ufl.cise.plpfa22.ast.ASTNode;
import edu.ufl.cise.plpfa22.ast.SyntaxException;

public class Parser implements IParser {

	private final ILexer lexer;
	private IToken currentToken;

	public Parser(ILexer lexer) {
		this.lexer = lexer;
	}

	@Override
	public ASTNode parse() throws PLPException {
		// TODO implement
		return null;
	}

	private void statement() throws LexicalException, SyntaxException {
		// <ident> := <expression
		if (isKind(Kind.IDENT)) {
			if (isKind(Kind.ASSIGN)) {
				exp();
			} else {
				error();
			}
		}
		// CALL <ident>
		else if (isKind(Kind.KW_CALL)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
			} else {
				error();
			}
		}
		// ? <ident>
		else if (isKind(Kind.QUESTION)) {
			consume();
			if (isKind(Kind.IDENT)) {
				consume();
			} else {
				error();
			}
		}
		// ! <expression>
		else if (isKind(Kind.BANG)) {
			consume();
			exp();
		}
		// BEGIN <statement> ( ; <statement> )* END
		else if (isKind(Kind.KW_BEGIN)) {
			consume();
			statement();
			while (isKind(Kind.SEMI)) {
				consume();
				statement();
			}
			if (isKind(Kind.KW_END)) {
				consume();
			} else {
				error();
			}
		}
		// IF <expression> THEN <statement>
		else if (isKind(Kind.KW_IF)) {
			consume();
			exp();
			if (isKind(Kind.KW_THEN)) {
				consume();
				statement();
			} else {
				error();
			}
		}
		// WHILE <expression> DO <statement>
		else if (isKind(Kind.KW_WHILE)) {
			consume();
			exp();
			if (isKind(Kind.KW_DO)) {
				consume();
				statement();
			} else {
				error();
			}
		}

	}

	private void exp() throws LexicalException {
		// TODO addExp();
		while (isKind(Kind.LT) || isKind(Kind.LE) || isKind(Kind.GT) || isKind(Kind.GE) || isKind(Kind.EQ)
				|| isKind(Kind.NEQ)) {
			consume();

		}
	}

	private void primaryExp() throws LexicalException, SyntaxException {
		if (isKind(Kind.IDENT) || constVal()) {
			// cannot let constVal consume token otherwise we won't be able to consume iden
			// token here without checking token kind again
			consume();
		} else if (isKind(Kind.LPAREN)) {
			exp();
			if (isKind(Kind.RPAREN)) {
				consume();
			} else {
				error();
			}
		} else {
			error();
		}
	}

	// only returning true or false to allow usage inside if conditions
	private boolean constVal() throws LexicalException {
		if (isKind(Kind.NUM_LIT) || isKind(Kind.STRING_LIT) || isKind(Kind.BOOLEAN_LIT)) {
			return true;
		}
		return false;
	}

	private boolean isKind(Kind kind) {
		return kind.equals(currentToken.getKind());
	}

	private IToken consume() throws LexicalException {
		currentToken = lexer.next();
		return currentToken;
	}

	private void error() throws SyntaxException {
		SourceLocation loc = currentToken.getSourceLocation();
		throw new SyntaxException("Parser encountered an error", loc.line(), loc.column());
	}

}
