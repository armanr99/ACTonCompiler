package main.symbolTable.symbolTableVariableItem;

import main.ast.type.Type;
import main.ast.node.declaration.VarDeclaration;

public class SymbolTableLocalVariableItem extends SymbolTableVariableItem {
    
    public SymbolTableLocalVariableItem(String name, Type type, int index)
    {
        super(name ,type ,index);
    }
    
    public SymbolTableLocalVariableItem(VarDeclaration localVar)
    {
        super(localVar);
    }
}
