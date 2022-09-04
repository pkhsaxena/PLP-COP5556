package edu.ufl.cise.plpfa22;

import java.util.Set;

public class Lexer implements ILexer {

	/*
	 * HAS_QUOTE -> A single quote was found. Could be the " token.
	 * HAS_BACKSLASH -> An open quote is followed by a \ somewhere (Goes to
	 * HAS_STRINGLIT state next (after a b|t|n|f|r|"|') based on testcases)
	 * HAS_STRINGLIT -> An open quote is followed by something not(\ | ")
	 * STRING_LIT -> HAS_STRINGLIT is followed by a "
	 * IN_IDENT -> Checks for Boolean and for keyword
	 * 
	 * TODO: Unsure when to return " token.
	 */
	private enum State {
		START, IN_IDENT, HAS_ZERO, IN_NUM, HAS_QUOTE, HAS_BACKSLASH, HAS_STRINGLIT, STRING_LIT, HAS_FOWRWARDSHLASH,
		HAS_COMMENT, HAS_COMMENT_CR, COMMENT, HAS_COLON, HAS_LESS_THAN, HAS_GREATER_THAN
	};

	private int lineNumber, columnNumber, currentCharacterIndex;

	public Lexer(String input) {
		// TODO Auto-generated constructor stub
		final int inputLength = input.length();
		State currentState = State.START;
		char currentCharacter;
		input += Character.toString(0);
		lineNumber = columnNumber = 1;
		currentCharacterIndex = 0;

		while (currentCharacterIndex < inputLength) {
			currentCharacter = input.charAt(currentCharacterIndex);
			if (currentCharacter == '\n' || currentCharacter == '\r') {
				// TODO: Check if this is to be moved elsewhere
				if (currentCharacter == '\n') {
					lineNumber += 1;
					columnNumber = 1;
				}
				if (currentCharacter == '\r') {
					lineNumber += 1;
					columnNumber = 1;
					currentCharacterIndex += 1; // Skip the \n
				}
			}
			if (currentCharacter == ' ' || currentCharacter == '\t') {
				// TODO: Check if this is to be moved elsewhere
				if (currentCharacter == ' ')
					columnNumber += 1;
				if (currentCharacter == '\t') {
					// TODO: Do we have tabs in input?
				}
			}
			switch (currentState) {
				case START: {
					// Check for white space
					if (Set.of(' ', '\t', '\r', '\n').contains(currentCharacter)) {
						continue;
					}
					// Che
					if (Set.of('.', ',', ';', '(', ')', '+', '-', '*', '/', '%', '?', '!', '=', '#')
							.contains(currentCharacter)) {
						// TODO: Return token/add token to list.
					}

				}
			}

		}

	}

	@Override
	public IToken next() throws LexicalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IToken peek() throws LexicalException {
		// TODO Auto-generated method stub
		return null;
	}

}
