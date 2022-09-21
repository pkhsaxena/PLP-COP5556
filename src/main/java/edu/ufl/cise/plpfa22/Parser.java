package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.ASTNode;

public class Parser implements IParser {

	private final ILexer lexer;
	private IToken token;

	public Parser(ILexer lexer) {
		this.lexer = lexer;
	}

	@Override
	public ASTNode parse() throws PLPException {
		// TODO implement
		return null;
	}

	private void primaryExp() throws LexicalException {
		if (isKind(Kind.IDENT) || constVal()) {

		} else if (isKind(Kind.LPAREN)) {
			// TODO call function to handle expression production
			//expecting the function to increment the token
			if (isKind(Kind.RPAREN)) {
				consume();
			}
		}
	}

	private boolean constVal() throws LexicalException {
		if (isKind(Kind.NUM_LIT) || isKind(Kind.STRING_LIT) || isKind(Kind.BOOLEAN_LIT)) {
			consume();
			return true;
		}
		return false;
	}

	private boolean isKind(Kind kind) {
		return kind == token.getKind();
	}

	private IToken consume() throws LexicalException {
		token = lexer.next();
		return token;
	}

}
