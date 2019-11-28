package main.symbolTable;

import main.ast.type.Type;

public abstract class SymbolTableItem {
	protected String name;

	public SymbolTableItem() {
	}

	public abstract String getKey();
}