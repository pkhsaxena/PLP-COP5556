package edu.ufl.cise.plpfa22;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.ufl.cise.plpfa22.IToken.Kind;

public class Lexer implements ILexer {
	private List<IToken> tokenList = new ArrayList<>();

	/*
	 * HAS_QUOTE -> A single quote was found. Could be the " token.
	 * HAS_BACKSLASH -> An open quote is followed by a \ somewhere (Goes to
	 * HAS_STRINGLIT state next (after a b|t|n|f|r|"|') based on testcases)
	 * HAS_STRINGLIT -> An open quote is followed by something not(\ | ")
	 * state. Just return the value.
	 * IN_IDENT -> Checks for Boolean and for keyword
	 * 
	 * TODO: Unsure when to return " token.
	 */
	private enum State {
		START, IN_IDENT, HAS_ZERO, IN_NUM, HAS_QUOTE, HAS_BACKSLASH, HAS_STRINGLIT, HAS_FOWRWARDSHLASH,
		HAS_COMMENT, HAS_COMMENT_CR, COMMENT, HAS_COLON, HAS_LESS_THAN, HAS_GREATER_THAN
	};

	private Map<String, Kind> reservedWords = Stream.of(new Object[][] {
			{ "TRUE", Kind.BOOLEAN_LIT },
			{ "FALSE", Kind.BOOLEAN_LIT },
			{ "CONST", Kind.KW_CONST },
			{ "VAR", Kind.KW_VAR },
			{ "PROCEDURE", Kind.KW_PROCEDURE },
			{ "CALL", Kind.KW_CALL },
			{ "BEGIN", Kind.KW_BEGIN },
			{ "END", Kind.KW_END },
			{ "IF", Kind.KW_IF },
			{ "THEN", Kind.KW_THEN },
			{ "WHILE", Kind.KW_WHILE },
			{ "DO", Kind.KW_DO }
	}).collect(Collectors.toMap(data -> (String) data[0], data -> (Kind) data[1]));

	private int lineNumber, columnNumber, currentCharacterIndex;

	public Lexer(String input) {
		StringBuilder tokenBuilder = new StringBuilder();
		final int inputLength = input.length();
		State currentState = State.START;
		char currentCharacter;
		StringBuilder tokenBuilder = new StringBuilder();
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
				case START -> {
					// Check for white space
					if (Set.of(' ', '\t', '\r', '\n').contains(currentCharacter)) {
						currentState = State.WS;
						continue;
					}
					// Check single char tokens
					if (Set.of('.', ',', ';', '(', ')', '+', '-', '*', '/', '%', '?', '!', '=', '#')
							.contains(currentCharacter)) {
						// TODO: Return token/add token to list.
					}
					if (currentCharacter == '"') {
						currentState = State.HAS_STRINGLIT;
					}
				}

				case WS -> {// whitespace separates tokens, all token start checks should be here
					if (currentCharacter == '"') {
						currentState = State.HAS_STRINGLIT;
					}
				}
				case HAS_STRINGLIT -> {
					if (!Set.of('\\', '"').contains(currentCharacter)) {
						continue;
					} else if (currentCharacter == '\\') {
						currentState = State.HAS_BACKSLASH;
						continue;
					} else if (currentCharacter == '"') {
						currentState = State.STRING_LIT;
						// TODO: add token to list
					}
				}
				case HAS_BACKSLASH -> {
					// valid escape sequence check
					if (Set.of('b', 't', 'n', 'f', 'r', '"', "'", '\\').contains(currentCharacter)) {
						// if valid escape sequence, continue to search for string literal end
						currentState = State.HAS_STRINGLIT;
						continue;
					} else {
						// TODO: error,store location and token
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
