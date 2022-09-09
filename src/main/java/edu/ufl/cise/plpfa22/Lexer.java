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
		START, IN_IDENT, IN_NUM, HAS_BACKSLASH, HAS_STRINGLIT, HAS_FOWRWARDSHLASH, HAS_COMMENT, HAS_COLON,
		HAS_LESS_THAN, HAS_GREATER_THAN
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

	private Map<Character, Character> escSeqReplacement = Stream.of(new Object[][] {
			{ 'b', '\b' },
			{ 't', '\t' },
			{ 'n', '\n' },
			{ 'f', '\f' },
			{ 'r', '\r' },
	}).collect(Collectors.toMap(data -> (Character) data[0], data -> (Character) data[1]));

	private int tokenIndex;

	public Lexer(String input) {
		final int inputLength;
		int lineNumber, columnNumber, currentColumnNumber, currentCharacterIndex;
		StringBuilder tokenBuilder = new StringBuilder();
		// text is how the string literal appears in the input, string value is the
		// representation of the string after escape sequence has been computed
		StringBuilder stringTextBuilder = new StringBuilder();
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
							}
						}
						currentCharacterIndex += 1;
						continue;
					}

					// Check single char tokens
					else if (Set.of('.', ',', ';', '(', ')', '+', '-', '*', '%', '?', '!', '=', '#')
							.contains(currentCharacter)) {
						tokenList.add(new Token(singleToken.get(Character.toString(currentCharacter)), lineNumber,
								columnNumber,
								Character.toString(currentCharacter)));
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for COLON
					else if (currentCharacter == ':') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_COLON;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for >
					else if (currentCharacter == '>') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_GREATER_THAN;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for <
					else if (currentCharacter == '<') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_LESS_THAN;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// check for identifier
					else if ((currentCharacter >= 'a' && currentCharacter <= 'z')
							|| (currentCharacter >= 'A' && currentCharacter <= 'Z') || (currentCharacter == '_')
							|| (currentCharacter == '$')) {
						currentColumnNumber = columnNumber;
						currentState = State.IN_IDENT;
						columnNumber += 1;
						currentCharacterIndex += 1;
						tokenBuilder.append(currentCharacter);
						continue;
					}

					// Check for forward slash
					else if (currentCharacter == '/') {
						currentColumnNumber = columnNumber;
						currentState = State.HAS_FOWRWARDSHLASH;
						columnNumber += 1;
						currentCharacterIndex += 1;
						continue;
					}

					// Check for open quote
					else if (currentCharacter == '"') {
						currentColumnNumber = columnNumber;
						columnNumber += 1;
						currentState = State.HAS_STRINGLIT;
						stringTextBuilder.append(currentCharacter);
						currentCharacterIndex += 1;
					}

					else if (currentCharacter == '0') {
						tokenBuilder.append(currentCharacter);
						tokenList.add(new Token(Kind.NUM_LIT, lineNumber, columnNumber, tokenBuilder.toString()));
						tokenBuilder = new StringBuilder();
						columnNumber += 1;
						currentCharacterIndex += 1;
					}

					else if (currentCharacter >= '1' && currentCharacter <= '9') {
						currentState = State.IN_NUM;
						currentColumnNumber = columnNumber;
						tokenBuilder.append(currentCharacter);
						columnNumber += 1;
						currentCharacterIndex += 1;
					}

					// Check EOF
					else if (currentCharacter == 0) {
						currentColumnNumber = columnNumber;
						tokenList.add(new Token(Kind.EOF, lineNumber, currentColumnNumber,
								Character.toString(currentCharacter)));
						currentCharacterIndex += 1;
						continue;
					}

					else {
						currentColumnNumber = columnNumber;
						tokenList.add(new Token(Kind.ERROR, lineNumber, currentColumnNumber,
								Character.toString(currentCharacter)));
						currentCharacterIndex = inputLength;
						break;
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

				case HAS_FOWRWARDSHLASH -> {
					if (currentCharacter == '/') {
						currentState = State.HAS_COMMENT;
						currentCharacterIndex += 1;
					} else {
						tokenList.add(new Token(Kind.DIV, lineNumber, currentColumnNumber, "/"));
						currentState = State.START;
					}
				}

				case HAS_COMMENT -> {
					if (currentCharacter != '\n' && currentCharacter != '\r' && currentCharacter != 0) {
						currentCharacterIndex += 1;
						continue;
					} else {
						currentState = State.START;
					}
				}

				case HAS_STRINGLIT -> {
					stringTextBuilder.append(currentCharacter);
					if (!Set.of('\\', '"').contains(currentCharacter)) {
						tokenBuilder.append(currentCharacter);
						currentCharacterIndex += 1;
						columnNumber += 1;
					} else if (currentCharacter == '\\') {
						// escape sequence start
						currentCharacterIndex += 1;
						columnNumber += 1;
						currentState = State.HAS_BACKSLASH;
					} else if (currentCharacter == '"') {
						// string literal end
						currentCharacterIndex += 1;
						columnNumber += 1;
						currentState = State.START;
						tokenList.add(new Token(Kind.STRING_LIT, lineNumber, currentColumnNumber,
								stringTextBuilder.toString(), tokenBuilder.toString()));
						stringTextBuilder = new StringBuilder();
						tokenBuilder = new StringBuilder(); // reset token builder
					} else {
						// error! cannot parse input further, store token formed till here
						tokenList.add(new Token(Kind.ERROR, lineNumber, columnNumber, tokenBuilder.toString()));
						break;
					}
				}

				case HAS_BACKSLASH -> {
					// valid escape sequence check
					if (Set.of('"', '\'', '\\').contains(currentCharacter)) {
						// if valid escape sequence, continue to search for string literal end
						tokenBuilder.append(currentCharacter);
						stringTextBuilder.append(currentCharacter);
						currentCharacterIndex += 1;
						columnNumber += 1;
						currentState = State.HAS_STRINGLIT;
					} else if (Set.of('b', 't', 'n', 'f', 'r').contains(currentCharacter)) {
						tokenBuilder.append(escSeqReplacement.get(currentCharacter));
						stringTextBuilder.append(currentCharacter);
						currentCharacterIndex += 1;
						columnNumber += 1;
						currentState = State.HAS_STRINGLIT;
					} else {
						// error! cannot parse input further, store token formed till here
						tokenList.add(new Token(Kind.ERROR, lineNumber, columnNumber, tokenBuilder.toString()));
						break;
					}
				}

				case IN_NUM -> {
					if (currentCharacter >= '0' && currentCharacter <= '9') {
						currentCharacterIndex += 1;
						columnNumber += 1;
						tokenBuilder.append(currentCharacter);
					} else {
						// numeric literal has ended one character before this, store in token
						tokenList
								.add(new Token(Kind.NUM_LIT, lineNumber, currentColumnNumber, tokenBuilder.toString()));
						tokenBuilder = new StringBuilder();
						// don't increment index to allow START state to process line and column number
						// and the current character
						currentState = State.START;
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
				throw new LexicalException("Lexer encountered an error", rToken.getSourceLocation());
			}
		} else {
			throw new LexicalException("No more tokens");
		}
	}

	@Override
	public IToken peek() throws LexicalException {
		if (tokenIndex < tokenList.size()) {
			IToken rToken = tokenList.get(tokenIndex);
			if (rToken.getKind() != Kind.ERROR) {
				return rToken;
			} else {
				throw new LexicalException("Lexer encountered an error", rToken.getSourceLocation());
			}
		} else {
			throw new LexicalException("No more tokens");
		}
	}
}
