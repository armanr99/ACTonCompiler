package main.symbolTable;

import main.ast.type.Type;
import main.ast.node.declaration.handler.*;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.Identifier;

// import java.lang.reflect.Method;
import java.util.ArrayList;

public class SymbolTableHandlerItem extends SymbolTableItem {

    protected ArrayList<Type> argTypes = new ArrayList<>();
    protected SymbolTable handlerSymbolTable;
    protected HandlerDeclaration handlerDeclaration;
    public static final String STARTKEY = "Handler_";

    public SymbolTableHandlerItem(String name, ArrayList<Type> argTypes) {
        this.name = name;
        this.argTypes = argTypes;
    }

    public SymbolTableHandlerItem(HandlerDeclaration handlerDeclaration)
    {
        this.name = handlerDeclaration.getName().getName();
        this.argTypes = new ArrayList<>();
        for(VarDeclaration arg: handlerDeclaration.getArgs())
            argTypes.add(arg.getType());
        this.handlerDeclaration = handlerDeclaration;
    }

    public void setHandlerSymbolTable(SymbolTable symbolTable)
    {
        this.handlerSymbolTable = symbolTable;
    }

    public SymbolTable getHandlerSymbolTable()
    {
        return this.handlerSymbolTable;
    }

    public void setHandlerDeclaration(HandlerDeclaration handlerDeclaration)
    {
        this.handlerDeclaration = handlerDeclaration;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName( String name )
    {
        this.name = name;
        handlerDeclaration.getName().setName( name );
    }
    
    public HandlerDeclaration getHandlerDeclaration()
    {
        return handlerDeclaration;
    }
    
    @Override
    public String getKey() {
        //todo
        return SymbolTableHandlerItem.STARTKEY + this.name;
    }
}
