package edu.ufl.cise.plpfa22;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import edu.ufl.cise.plpfa22.ast.Declaration;

public class SymbolTable {
	Map<String, Map<Integer, Declaration>> table;

	public SymbolTable() {
		table = new HashMap<>();
	}

	public Declaration get(String iden, Stack<Integer> stack) throws ScopeException {
		if (table.containsKey(iden)) {
			for (Integer scope : table.get(iden).keySet()) { // Collections.sort(table.get(iden).keySet(),
																// Collections.reverseOrder())
				if (stack.contains(scope)) {
					return table.get(iden).get(scope);
				}
			}
		}
		throw new ScopeException(("Declaration " + iden + " does not exist"));
	}

	public void put(String iden, Stack<Integer> stack, Declaration dec) throws ScopeException {
		if (table.containsKey(iden)) {
			if (table.get(iden).containsKey(stack.peek())) {
				throw new ScopeException(("Declaration " + iden + " already exists"));
			} else {
				table.get(iden).put(stack.peek(), dec);
			}
		} else {
			table.put(iden, new TreeMap<>(Collections.reverseOrder()));
			table.get(iden).put(stack.peek(), dec);
		}
	}
}
