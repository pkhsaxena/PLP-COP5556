/**  This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized.
 */

package edu.ufl.cise.plpfa22;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plpfa22.IToken.Kind;

class LexerTest {

	/*** Useful functions ***/
	ILexer getLexer(String input) {
		return CompilerComponentFactory.getLexer(input);
	}

	// makes it easy to turn output on and off (and less typing than
	// System.out.println)
	static final boolean VERBOSE = true;

	void show(Object obj) {
		if (VERBOSE) {
			System.out.println(obj);
		}
	}

	// check that this token has the expected kind
	void checkToken(IToken t, Kind expectedKind) {
		assertEquals(expectedKind, t.getKind());
	}

	// check that the token has the expected kind and position
	void checkToken(IToken t, Kind expectedKind, int expectedLine, int expectedColumn) {
		assertEquals(expectedKind, t.getKind());
		assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
	}

	// check that this token is an IDENT and has the expected name
	void checkIdent(IToken t, String expectedName) {
		assertEquals(Kind.IDENT, t.getKind());
		assertEquals(expectedName, String.valueOf(t.getText()));
	}

	// check that this token is an IDENT, has the expected name, and has the
	// expected position
	void checkIdent(IToken t, String expectedName, int expectedLine, int expectedColumn) {
		checkIdent(t, expectedName);
		assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
	}

	// check that this token is an NUM_LIT with expected int value
	void checkInt(IToken t, int expectedValue) {
		assertEquals(Kind.NUM_LIT, t.getKind());
		assertEquals(expectedValue, t.getIntValue());
	}

	// check that this token is an NUM_LIT with expected int value and position
	void checkInt(IToken t, int expectedValue, int expectedLine, int expectedColumn) {
		checkInt(t, expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
	}

	// check that this token is the EOF token
	void checkEOF(IToken t) {
		checkToken(t, Kind.EOF);
	}

	/*** Tests ****/

	// The lexer should add an EOF token to the end.
	@Test
	void testEmpty() throws LexicalException {
		String input = "";
		show(input);
		show("testEmpty");
		ILexer lexer = getLexer(input);
		show(lexer);
		checkEOF(lexer.next());
	}

	// A couple of single character tokens
	@Test
	void testSingleChar0() throws LexicalException {
		String input = """
				+
				-
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.PLUS, 1, 1);
		checkToken(lexer.next(), Kind.MINUS, 2, 1);
		checkEOF(lexer.next());
	}

	// comments should be skipped
	@Test
	void testComment0() throws LexicalException {
		// Note that the quotes around "This is a string" are passed to the lexer.
		String input = """
				"This is a string"
				// this is a comment
				*
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 1, 1);
		checkToken(lexer.next(), Kind.TIMES, 3, 1);
		checkEOF(lexer.next());
	}

	// Example for testing input with an illegal character
	@Test
	void testError0() throws LexicalException {
		String input = """
				abc
				@
				""";
		show(input);
		ILexer lexer = getLexer(input);
		// this check should succeed
		checkIdent(lexer.next(), "abc");
		// this is expected to throw an exception since @ is not a legal
		// character unless it is part of a string or comment
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	// Several identifiers to test positions
	@Test
	public void testIdent0() throws LexicalException {
		String input = """
				abc
				  def
				     ghi

				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "abc", 1, 1);
		checkIdent(lexer.next(), "def", 2, 3);
		checkIdent(lexer.next(), "ghi", 3, 6);
		checkEOF(lexer.next());
	}

	@Test
	public void testIdenInt() throws LexicalException {
		String input = """
				a123 456b
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 1, 1);
		checkInt(lexer.next(), 456, 1, 6);
		checkIdent(lexer.next(), "b", 1, 9);
		checkEOF(lexer.next());
	}

	// Example showing how to handle number that are too big.
	@Test
	public void testIntTooBig() throws LexicalException {
		String input = """
				42
				99999999999999999999999999999999999999999999999999999999999999999999999
				""";
		ILexer lexer = getLexer(input);
		checkInt(lexer.next(), 42);
		assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	public void testEscapeSequences0() throws LexicalException {
		String input = "\"\\b \\t \\n \\f \\r \"";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "\b \t \n \f \r ";
		assertEquals(expectedStringValue, val);
		String text = String.valueOf(t.getText());
		String expectedText = "\"\\b \\t \\n \\f \\r \"";
		assertEquals(expectedText, text);
	}

	@Test
	public void testEscapeSequences1() throws LexicalException {
		String input = "   \" ...  \\\"  \\\'  \\\\  \"";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = " ...  \"  \'  \\  ";
		assertEquals(expectedStringValue, val);
		String text = String.valueOf(t.getText());
		String expectedText = "\" ...  \\\"  \\\'  \\\\  \""; // almost the same as input, but white space is omitted
		assertEquals(expectedText, text);
	}

	@Test
	public void testSingleTokens() throws LexicalException {
		String input = """
				.+ *?! %
				-""";
		show("testSingleTokens");
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.DOT, 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 2);
		checkToken(lexer.next(), Kind.TIMES, 1, 4);
		checkToken(lexer.next(), Kind.QUESTION, 1, 5);
		checkToken(lexer.next(), Kind.BANG, 1, 6);
		checkToken(lexer.next(), Kind.MOD, 1, 8);
		checkToken(lexer.next(), Kind.MINUS, 2, 1);
	}

	@Test
	public void testWhiteSpaceWithSingleTokens() throws LexicalException {
		String input = """
				.+ *?! %\n
				-\r\n
				+""";
		show("testWhiteSpaceWithSingleTokens");
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.DOT, 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 2);
		checkToken(lexer.next(), Kind.TIMES, 1, 4);
		checkToken(lexer.next(), Kind.QUESTION, 1, 5);
		checkToken(lexer.next(), Kind.BANG, 1, 6);
		checkToken(lexer.next(), Kind.MOD, 1, 8);
		checkToken(lexer.next(), Kind.MINUS, 3, 1);
		checkToken(lexer.next(), Kind.PLUS, 5, 1);
	}

	@Test
	public void test_GT_LT_AS_Tokens() throws LexicalException {
		String input = """
				>=
				>
				< <<
				<=
				:=<>>=""";
		show("test_GT_LT_AS_Tokens");
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.GE, 1, 1);
		checkToken(lexer.next(), Kind.GT, 2, 1);
		checkToken(lexer.next(), Kind.LT, 3, 1);
		checkToken(lexer.next(), Kind.LT, 3, 3);
		checkToken(lexer.next(), Kind.LT, 3, 4);
		checkToken(lexer.next(), Kind.LE, 4, 1);
		checkToken(lexer.next(), Kind.ASSIGN, 5, 1);
		checkToken(lexer.next(), Kind.LT, 5, 3);
		checkToken(lexer.next(), Kind.GT, 5, 4);
		checkToken(lexer.next(), Kind.GE, 5, 5);
	}

	@Test
	public void test_IDENT_KW_Tokens() throws LexicalException {
		String input = """
				ABCD1234
				$12bc TRUE DO FALSE WHILE
				_TRUE $FALSE help
				true KEEPME VAR""";
		show("test_IDENT_KW_Tokens");
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 1, 1);
		checkToken(lexer.next(), Kind.IDENT, 2, 1);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 2, 7);
		checkToken(lexer.next(), Kind.KW_DO, 2, 12);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 2, 15);
		checkToken(lexer.next(), Kind.KW_WHILE, 2, 21);
		checkToken(lexer.next(), Kind.IDENT, 3, 1);
		checkToken(lexer.next(), Kind.IDENT, 3, 7);
		checkToken(lexer.next(), Kind.IDENT, 3, 14);
		checkToken(lexer.next(), Kind.IDENT, 4, 1);
		checkToken(lexer.next(), Kind.IDENT, 4, 6);
		checkToken(lexer.next(), Kind.KW_VAR, 4, 13);
	}

	@Test
	public void test_Comments_and_DIV_Tokens() throws LexicalException {
		String input = """
				A23 / TRUE //Hello world this all ***
				Can HeAr ? //The end""";
		show("test_Comments_and_DIV_Tokens");
		show(input);
		ILexer lexer = getLexer(input);

		IToken t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkToken(t, Kind.IDENT, 1, 1);

		t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkToken(t, Kind.DIV, 1, 5);

		t = lexer.next();

		t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkToken(t, Kind.IDENT, 2, 1);

		t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkToken(t, Kind.IDENT, 2, 5);

		t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkToken(t, Kind.QUESTION, 2, 10);

		t = lexer.next();
		// show(t.getKind());
		// show(t.getSourceLocation());
		checkEOF(t);
	}

	@Test
	public void testIdenInt1() throws LexicalException {
		String input = """
				a123 456b 657ABCD""";
		show(input);
		show("********************");
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 1, 1);
		checkInt(lexer.next(), 456, 1, 6);
		checkIdent(lexer.next(), "b", 1, 9);
		checkInt(lexer.next(), 657, 1, 11);
		checkIdent(lexer.next(), "ABCD", 1, 14);
		checkEOF(lexer.next());
	}

	@Test
	public void testIdenInt2() throws LexicalException {
		String input = "a123 456b 657";
		show(input);
		show("---------------");
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 1, 1);
		checkInt(lexer.next(), 456, 1, 6);
		checkIdent(lexer.next(), "b", 1, 9);
		checkInt(lexer.next(), 657, 1, 11);
		checkEOF(lexer.next());
	}

	@Test
	public void testString1() throws LexicalException {
		String input = """
				"abcd \\r \\n \\b \\\\ \\" \\\\ \\b \\n \\' \\" \\\\ "
				""";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "abcd \r \n \b \\ \" \\ \b \n \' \" \\ ";
		String text = String.valueOf(t.getText());
		String expectedText = "\"abcd \\r \\n \\b \\\\ \\\" \\\\ \\b \\n \\' \\\" \\\\ \"";
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
	}

	@Test
	public void testString2() throws LexicalException {
		String input = """
				"abcd \\r \\n \\b \\\\ \\" \\\\ \\b \\n \\' \\" \\\\ "ABCD
				""";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "abcd \r \n \b \\ \" \\ \b \n \' \" \\ ";
		String text = String.valueOf(t.getText());
		String expectedText = "\"abcd \\r \\n \\b \\\\ \\\" \\\\ \\b \\n \\' \\\" \\\\ \"";
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		t = lexer.next();
		checkIdent(t, "ABCD");
	}

	@Test
	public void testString3() throws LexicalException {
		String input = """
				"abcd \\r \\n \\b \\\\ \\" \\\\ hello \\b \\n \\' \\" \\\\ "ABCD
				""";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "abcd \r \n \b \\ \" \\ hello \b \n \' \" \\ ";
		String text = String.valueOf(t.getText());
		String expectedText = "\"abcd \\r \\n \\b \\\\ \\\" \\\\ hello \\b \\n \\' \\\" \\\\ \"";
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		t = lexer.next();
		checkIdent(t, "ABCD");
	}

	@Test
	public void testString4() throws LexicalException {
		String input = """
				"abcd \\r \\n \\b \\\\ \\" \\\\ hello
				world \\b \\n \\' \\" \\\\ "ABCD
				""";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "abcd \r \n \b \\ \" \\ hello\nworld \b \n \' \" \\ ";
		String text = String.valueOf(t.getText());
		String expectedText = "\"abcd \\r \\n \\b \\\\ \\\" \\\\ hello\nworld \\b \\n \\' \\\" \\\\ \"";
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		t = lexer.next();
		checkIdent(t, "ABCD");
	}

	@Test
	public void testString5() throws LexicalException {
		String input = """
				"Hello \\k world"
				""";
		show(input);
		ILexer lexer = getLexer(input);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	// Test 1
	// identifier.
	@Test
	void testID() throws LexicalException {
		String input = "ad23";
		show(input);
		ILexer lexer = getLexer(input);
		show(lexer);
		checkToken(lexer.next(), Kind.IDENT, 1, 1);
		checkEOF(lexer.next());
	}

	// Test 2
	// booleans
	// check that this token is an BOOLEAN_LIT with expected value
	void checkBoolean(IToken t, boolean expectedValue) {
		assertEquals(Kind.BOOLEAN_LIT, t.getKind());
		assertEquals(expectedValue, t.getBooleanValue());
	}

	@Test
	public void testBooleans() throws LexicalException {
		String input = """
				TRUE
				FALSE
				""";
		ILexer lexer = getLexer(input);
		checkBoolean(lexer.next(), true);
		checkBoolean(lexer.next(), false);
		checkEOF(lexer.next());
	}

	// Test 3
	// Mix of ID's, Num_lit, comments, string_lit's
	@Test
	public void testIDNNUM() throws LexicalException {
		String input = """
				df123 345 g546 IF
				//next is string

				 "Hello, World"
				""";
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "df123", 1, 1);
		checkInt(lexer.next(), 345, 1, 7);
		checkIdent(lexer.next(), "g546", 1, 11);
		checkToken(lexer.next(), Kind.KW_IF, 1, 16);
		checkToken(lexer.next(), Kind.STRING_LIT, 4, 2);
		checkEOF(lexer.next());
	}

	// Test 4
	// . , ; ( ) + - * / % ? ! := = # < <= > >=
	@Test
	public void testAllSymmbols() throws LexicalException {
		String input = """
				. , ; ( ) + - * / %
				//next is line 3
				? ! := = # < <= > >=
				""";

		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.DOT, 1, 1);
		checkToken(lexer.next(), Kind.COMMA, 1, 3);
		checkToken(lexer.next(), Kind.SEMI, 1, 5);
		checkToken(lexer.next(), Kind.LPAREN, 1, 7);
		checkToken(lexer.next(), Kind.RPAREN, 1, 9);
		checkToken(lexer.next(), Kind.PLUS, 1, 11);
		checkToken(lexer.next(), Kind.MINUS, 1, 13);
		checkToken(lexer.next(), Kind.TIMES, 1, 15);
		checkToken(lexer.next(), Kind.DIV, 1, 17);
		checkToken(lexer.next(), Kind.MOD, 1, 19);
		checkToken(lexer.next(), Kind.QUESTION, 3, 1);
		checkToken(lexer.next(), Kind.BANG, 3, 3);
		checkToken(lexer.next(), Kind.ASSIGN, 3, 5);
		checkToken(lexer.next(), Kind.EQ, 3, 8);
		checkToken(lexer.next(), Kind.NEQ, 3, 10);
		checkToken(lexer.next(), Kind.LT, 3, 12);
		checkToken(lexer.next(), Kind.LE, 3, 14);
		checkToken(lexer.next(), Kind.GT, 3, 17);
		checkToken(lexer.next(), Kind.GE, 3, 19);
		checkEOF(lexer.next());
	}

	// Test 5
	// reserved words
	@Test
	public void testAllreserved1() throws LexicalException {
		String input = """
				CONST VAR PROCEDURE
				     CALL BEGIN END
				        //next is line 3
				        IF THEN WHILE DO

				""";

		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.KW_CONST, 1, 1);
		checkToken(lexer.next(), Kind.KW_VAR, 1, 7);
		checkToken(lexer.next(), Kind.KW_PROCEDURE, 1, 11);
		checkToken(lexer.next(), Kind.KW_CALL, 2, 6);
		checkToken(lexer.next(), Kind.KW_BEGIN, 2, 11);
		checkToken(lexer.next(), Kind.KW_END, 2, 17);
		checkToken(lexer.next(), Kind.KW_IF, 4, 9);
		checkToken(lexer.next(), Kind.KW_THEN, 4, 12);
		checkToken(lexer.next(), Kind.KW_WHILE, 4, 17);
		checkToken(lexer.next(), Kind.KW_DO, 4, 23);
		checkEOF(lexer.next());
	}

	// Test 6
	// 12+3
	@Test
	public void testNoSpace() throws LexicalException {
		String input = """
				12+3
				""";

		ILexer lexer = getLexer(input);
		checkInt(lexer.next(), 12, 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 3);
		checkInt(lexer.next(), 3, 1, 4);
		checkEOF(lexer.next());
	}

	@Test
	public void testSpaceInString() throws LexicalException {
		String input = """
				ABCD "Hello World
				Bye bye"
				123 TRUE!
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "ABCD", 1, 1);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "Hello World\nBye bye";
		String text = String.valueOf(t.getText());
		String expectedText = "\"Hello World\nBye bye\"";
		show("TEXT:");
		show(text);
		show("EXPECTED:");
		show(expectedText);
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		checkInt(lexer.next(), 123, 3, 1);
		checkBoolean(lexer.next(), true);
		checkToken(lexer.next(), Kind.BANG, 3, 9);
		checkEOF(lexer.next());
	}

	@Test
	public void testSpaceInString2() throws LexicalException {
		String input = """
				ABCD "Hello World\\n
				Bye bye"
				123 TRUE!
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "ABCD", 1, 1);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "Hello World\n\nBye bye";
		String text = String.valueOf(t.getText());
		String expectedText = "\"Hello World\\n\nBye bye\"";
		show("TEXT:");
		show(text);
		show("EXPECTED:");
		show(expectedText);
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		checkInt(lexer.next(), 123, 3, 1);
		checkBoolean(lexer.next(), true);
		checkToken(lexer.next(), Kind.BANG, 3, 9);
		checkEOF(lexer.next());
	}

	@Test
	public void testSpaceInString3() throws LexicalException {
		String input = """
				ABCD "Hello World\n
				Bye bye"
				123 TRUE!
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "ABCD", 1, 1);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "Hello World\n\nBye bye";
		String text = String.valueOf(t.getText());
		String expectedText = "\"Hello World\n\nBye bye\"";
		show("TEXT:");
		show(text);
		show("EXPECTED:");
		show(expectedText);
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		checkInt(lexer.next(), 123, 4, 1);
		checkBoolean(lexer.next(), true);
		checkToken(lexer.next(), Kind.BANG, 4, 9);
		checkEOF(lexer.next());
	}

	@Test
	public void testSpaceInString4() throws LexicalException {
		String input = """
				ABCD "Hello World\r\n
				Bye bye"
				123 TRUE!
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "ABCD", 1, 1);
		IToken t = lexer.next();
		String val = t.getStringValue();
		String expectedStringValue = "Hello World\r\n\nBye bye";
		String text = String.valueOf(t.getText());
		String expectedText = "\"Hello World\r\n\nBye bye\"";
		show("TEXT:");
		show(text);
		show("EXPECTED:");
		show(expectedText);
		assertEquals(expectedText, text);
		assertEquals(expectedStringValue, val);
		checkInt(lexer.next(), 123, 4, 1);
		checkBoolean(lexer.next(), true);
		checkToken(lexer.next(), Kind.BANG, 4, 9);
		checkEOF(lexer.next());
	}

	// Example showing how to handle number that are too big.
	@Test
	public void testIncompleteString() throws LexicalException {
		String input = """
				"Hello world
				today was a
				good day
				""";
		ILexer lexer = getLexer(input);
		assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}


	@Test
	public void test0Two0s() throws LexicalException {
		String input = """
				00
				""";
		ILexer lexer = getLexer(input);
		IToken token = lexer.next();
		assertEquals(token.getKind(), Kind.NUM_LIT);
		assertEquals(token.getKind(), Kind.NUM_LIT);
	}

	@Test
	public void testIdentFollowedByToken() throws LexicalException {
		String input = """
				identifier+
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "identifier", 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 11);
	}

	@Test
	public void testIdentFollowedByInvalidChar() throws LexicalException {
		String input = """

				l@
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "l", 2, 1);
		assertThrows(LexicalException.class, () -> lexer.next());
	}
	
	@Test
	public void testMultipleEOF() throws LexicalException {
		String input = """
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
		checkEOF(lexer.next());
	}
	
	@Test
	public void testMultipleEOFPeek() throws LexicalException {
		String input = """
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.peek());
		checkEOF(lexer.next());
		checkEOF(lexer.next());
		checkEOF(lexer.peek());
	}

}
