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
	private Kind tokenKind;
	private int lineNumber;
	private int columnNumber;
	private String rawText;
	private Integer intValue;
	private Boolean boolValue;
	private String strValue;

	public Token(Kind tokenKind, int lineNumber, int columnNumber, String rawText) {
		this.tokenKind = tokenKind;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.rawText = rawText;

		if (tokenKind == Kind.NUM_LIT) {
			try {
				this.intValue = Integer.parseInt(rawText);
			} catch (NumberFormatException e) {
				// suppress
				this.tokenKind = Kind.ERROR;
				this.intValue = null;
			}
			this.boolValue = Boolean.FALSE;
			this.strValue = "";
		} else if (tokenKind == Kind.BOOLEAN_LIT) {
			this.intValue = Integer.MIN_VALUE;
			this.boolValue = Boolean.parseBoolean(rawText);
			this.strValue = "";
		} else {
			this.intValue = Integer.MIN_VALUE;
			this.boolValue = Boolean.FALSE;
			this.strValue = "";
		}
	}

	public Token(Kind tokenKind, int lineNumber, int columnNumber, String rawText, String strValue) {
		this.tokenKind = tokenKind;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.rawText = rawText;
		if (tokenKind == Kind.STRING_LIT) {
			this.intValue = Integer.MIN_VALUE;
			this.boolValue = Boolean.FALSE;
			this.strValue = strValue;
		} else {
			this.intValue = Integer.MIN_VALUE;
			this.boolValue = Boolean.FALSE;
			this.strValue = "";
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
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean getBooleanValue() {
		// Return boolean value if BOOLEAN_LIT
		if (tokenKind == Kind.BOOLEAN_LIT) {
			return boolValue;
		}
		return Boolean.FALSE;
	}

	@Override
	public String getStringValue() {
		// Return string value if STRING_LIT
		if (tokenKind == Kind.STRING_LIT) {
			return strValue;
		}
		return null;
	}
}
