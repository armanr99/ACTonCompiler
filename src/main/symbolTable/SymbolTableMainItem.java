package main.symbolTable;

import main.ast.node.Main;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.expression.Identifier;

public class SymbolTableMainItem extends SymbolTableItem {
        
    protected SymbolTable mainSymbolTable;
    protected Main programMain;
    public static final String STARTKEY = "Main_";

    public SymbolTableMainItem(Main programMain)
    {
        this.name = "main";
        this.programMain = programMain;
    }
    
    public void setMainSymbolTable(SymbolTable mainSymbolTable)
    {
        this.mainSymbolTable = mainSymbolTable;
    }

    public SymbolTable getMainSymbolTable()
    {
        return mainSymbolTable;
    }

    public Main getProgramMain()
    {
        return programMain;
    }

    @Override
    public String getKey()
    {
        return SymbolTableMainItem.STARTKEY + this.name;
    }
}
