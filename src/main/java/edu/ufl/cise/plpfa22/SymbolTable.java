package edu.ufl.cise.plpfa22;

import java.util.Collections;
import java.util.Map;

import edu.ufl.cise.plpfa22.ast.Declaration;

public class SymbolTable {
	Map<String, Map<Integer, Declaration>> table;

	public Declaration lookup(String iden, Integer nest) {
		if (table.get(iden) != null) {
			for (Integer nest : Collections.sort(table.get(iden).keySet().to)) {

			}
			return table.get(iden).get(nest);
		}

	}
}
