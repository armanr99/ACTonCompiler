package main.symbolTable.symbolTableVariableItem;

import main.ast.type.Type;
import main.ast.node.declaration.VarDeclaration;

public class SymbolTableHandlerArgumentItem extends SymbolTableVariableItem {
    
    public SymbolTableHandlerArgumentItem(String name, Type type, int index)
    {
        super(name, type, index);
    }

    public SymbolTableHandlerArgumentItem(VarDeclaration argDeclaration)
    {
        super(argDeclaration);
    }
}
