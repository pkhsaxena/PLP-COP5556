/**  This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized.
 */

package edu.ufl.cise.plpfa22.ast;

public class Types {

	public static enum Type {
		NUMBER {
			@Override
			public String getJVMType() {
				return "I";
			}
		},
		BOOLEAN {
			@Override
			public String getJVMType() {
				return "Z";
			}
		},
		STRING {
			@Override
			public String getJVMType() {
				return "Ljava/lang/String;";
			}
		},
		PROCEDURE {
			@Override
			public String getJVMType() {
				return null;
			}
		};

		public abstract String getJVMType();
	};

}
