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

	private Map<String, Kind> singleToken = Stream.of(new Object[][] {
			{ ".", Kind.DOT },
			{ ",", Kind.COMMA },
			{ ";", Kind.SEMI },
			{ "(", Kind.LPAREN },
			{ ")", Kind.RPAREN },
			{ "+", Kind.PLUS },
			{ "-", Kind.MINUS },
			{ "*", Kind.TIMES },
			{ "%", Kind.MOD },
			{ "?", Kind.QUESTION },
			{ "!", Kind.BANG },
			{ "=", Kind.EQ },
			{ "#", Kind.NEQ }
	}).collect(Collectors.toMap(data -> (String) data[0], data -> (Kind) data[1]));

	private int tokenIndex;

	public Lexer(String input) {
		final int inputLength;
		int lineNumber, columnNumber, currentColumnNumber, currentCharacterIndex;
		StringBuilder tokenBuilder = new StringBuilder();
		State currentState = State.START;
		char currentCharacter;
		String tokenString;

		input += Character.toString(0);
		inputLength = input.length();
		lineNumber = columnNumber = 1;
		currentCharacterIndex = currentColumnNumber = 0;
		tokenIndex = 0;

		while (currentCharacterIndex < inputLength) {
			currentCharacter = input.charAt(currentCharacterIndex);

			// Start statemachine
			switch (currentState) {

				case START -> {

					// Check for whitespace/newline/cr
					if (Set.of(' ', '\t', '\r', '\n').contains(currentCharacter)) {
						// Check for new line.
						if (currentCharacter == '\n' || currentCharacter == '\r') {
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

						// Check for white space
						if (currentCharacter == ' ' || currentCharacter == '\t') {
							if (currentCharacter == ' ')
								columnNumber += 1;
							if (currentCharacter == '\t') {
								// TODO: Do we have tabs in input?
							}
						}
						currentCharacterIndex += 1;
						continue;
					}

					// Check single char tokens
					if (Set.of('.', ',', ';', '(', ')', '+', '-', '*', '%', '?', '!', '=', '#')
							.contains(currentCharacter)) {
						tokenList.add(new Token(singleToken.get(Character.toString(currentCharacter)), lineNumber,
								columnNumber,
								Character.toString(currentCharacter)));
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for COLON
					if (currentCharacter == ':') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_COLON;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for >
					if (currentCharacter == '>') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_GREATER_THAN;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for <
					if (currentCharacter == '<') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_LESS_THAN;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// check for identifier
					if ((currentCharacter >= 'a' && currentCharacter <= 'z')
							|| (currentCharacter >= 'A' && currentCharacter <= 'Z') || (currentCharacter == '_')
							|| (currentCharacter >= '$')) {
						currentColumnNumber = columnNumber;
						currentState = State.IN_IDENT;
						columnNumber += 1;
						currentCharacterIndex += 1;
						tokenBuilder.append(currentCharacter);
						continue;
					}

					// Check for open quote
					if (false && currentCharacter == '"') { // TODO: Remove false
						currentState = State.HAS_STRINGLIT;
					}

					// Check EOF
					if (currentCharacter == 0) {
						currentColumnNumber = columnNumber;
						tokenList.add(new Token(Kind.EOF, lineNumber, currentColumnNumber,
								Character.toString(currentCharacter)));
						currentCharacterIndex += 1;
					}

					// TODO: Handle errors
					else {
						currentCharacterIndex += 1;
					}
				}

				case HAS_COLON -> {
					if (currentCharacter == '=') {
						tokenList.add(new Token(Kind.ASSIGN, lineNumber, currentColumnNumber, ":="));
						columnNumber += 1;
						currentCharacterIndex += 1;
						currentState = State.START;
					} else {
						tokenList.add(new Token(Kind.ERROR, lineNumber, currentColumnNumber, (":" + currentCharacter)));
						break;
					}
				}

				case IN_IDENT -> {
					if ((currentCharacter >= 'a' && currentCharacter <= 'z')
							|| (currentCharacter >= 'A' && currentCharacter <= 'Z') || (currentCharacter == '_')
							|| (currentCharacter >= '$') || (currentCharacter >= '0' && currentCharacter <= '9')) {
						columnNumber += 1;
						currentCharacterIndex += 1;
						tokenBuilder.append(currentCharacter);
					} else {
						tokenString = tokenBuilder.toString();
						if (reservedWords.containsKey(tokenString)) {
							tokenList.add(new Token(reservedWords.get(tokenString), lineNumber, currentColumnNumber,
									tokenString));
						} else {
							tokenList.add(new Token(Kind.IDENT, lineNumber, currentColumnNumber, tokenString));
						}
						tokenBuilder.setLength(0);
						currentState = State.START;
					}
				}

				case HAS_GREATER_THAN -> {
					if (currentCharacter == '=') {
						tokenList.add(new Token(Kind.GE, lineNumber, currentColumnNumber, ">="));
						columnNumber += 1;
						currentCharacterIndex += 1;
						currentState = State.START;
					} else {
						tokenList.add(new Token(Kind.GT, lineNumber, currentColumnNumber, ">"));
						currentState = State.START;
					}
				}

				case HAS_LESS_THAN -> {
					if (currentCharacter == '=') {
						tokenList.add(new Token(Kind.LE, lineNumber, currentColumnNumber, "<="));
						columnNumber += 1;
						currentCharacterIndex += 1;
						currentState = State.START;
					} else {
						tokenList.add(new Token(Kind.LT, lineNumber, currentColumnNumber, "<"));
						currentState = State.START;
					}
				}

				case HAS_STRINGLIT -> {
					if (!Set.of('\\', '"').contains(currentCharacter)) {
						continue;
					} else if (currentCharacter == '\\') {
						currentState = State.HAS_BACKSLASH;
						continue;
					} else if (currentCharacter == '"') {
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
		if (tokenIndex < tokenList.size()) {
			IToken rToken = tokenList.get(tokenIndex);
			tokenIndex += 1;
			if (rToken.getKind() != Kind.ERROR) {
				return rToken;
			} else {
				// TODO: Throw error
			}
		}
		return null;
	}

	@Override
	public IToken peek() throws LexicalException {
		if (tokenIndex < tokenList.size()) {
			IToken rToken = tokenList.get(tokenIndex);
			if (rToken.getKind() != Kind.ERROR) {
				return rToken;
			} else {
				// TODO: Throw error
			}
		}
		return null;
	}
}
