package edu.ufl.cise.plpfa22;

import java.io.IOException;

import org.objectweb.asm.util.ASMifier;

public class Test {

	public static void main(String[] args) throws IOException {
//		Map<Integer, String> procNames = new TreeMap<>(Collections.reverseOrder());
		ASMifier.main(new String[]{"-nodebug", "target/classes/edu/ufl/cise/plpfa22/Var$l.class"});
//		Stack<String> stack = new Stack<>();
//		stack.push("a");
//		stack.push("b");
//		stack.push("c");
//		for(int i=0;i<stack.size();i++) {
//			System.out.println(stack.get(i));
//		}
	}
}
