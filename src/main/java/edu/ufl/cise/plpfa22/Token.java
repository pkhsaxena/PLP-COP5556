/**
 * 
 */
package edu.ufl.cise.plpfa22;

public class Token implements IToken {

	/*
	 * Every token has a kind.
	 * Every token has a source location (line and col).
	 * Raw Text
	 * Int Value if Int
	 * Bool Value if Bool
	 * String Value if STRING_LIT
	 */
	private final Kind tokenKind;
	private final int lineNumber;
	private final int columnNumber;
	private final String rawText;
	private final int intValue;
	private final boolean boolValue;
	private final String strValue;

	public Token(Kind tokenKind, int lineNumber, int columnNumber, String rawText) {
		this.tokenKind = tokenKind;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.rawText = rawText;
		if (tokenKind == Kind.NUM_LIT) {
			intValue = Integer.parseInt(rawText);
			boolValue = Boolean.FALSE;
			strValue = "";
		} else if (tokenKind == Kind.BOOLEAN_LIT) {
			intValue = Integer.MIN_VALUE;
			boolValue = Boolean.parseBoolean(rawText);
			strValue = "";
		} else if (tokenKind == Kind.STRING_LIT) {
			// TODO: Implement based on what raw string is.
			intValue = Integer.MIN_VALUE;
			boolValue = Boolean.FALSE;
			strValue = ""; // TODO: Fix actual string value
		} else {
			intValue = Integer.MIN_VALUE;
			boolValue = Boolean.FALSE;
			strValue = "";
		}
	}

	@Override
	public Kind getKind() {
		// Return token kind
		return tokenKind;
	}

	@Override
	public char[] getText() {
		// Return raw text
		return rawText.toCharArray();
	}

	@Override
	public SourceLocation getSourceLocation() {
		// Return line number and column number
		return new SourceLocation(lineNumber, columnNumber);
	}

	@Override
	public int getIntValue() {
		// Return int value if NUM_LIT
		if (tokenKind == Kind.NUM_LIT) {
			return intValue;
		}
		return Integer.MIN_VALUE; // TODO: Do we need to throw an error?
	}

	@Override
	public boolean getBooleanValue() {
		// Return boolean value if BOOLEAN_LIT
		if (tokenKind == Kind.BOOLEAN_LIT) {
			return boolValue;
		}
		return Boolean.FALSE; // TODO: Do we need to throw an error?
	}

	@Override
	public String getStringValue() {
		// Return string value if STRING_LIT
		if (tokenKind == Kind.STRING_LIT) {
			return strValue;
		}
		return null; // TODO: Do we need to throw an error?
	}
}
