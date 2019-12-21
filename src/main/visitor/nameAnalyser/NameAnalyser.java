package main.visitor.nameAnalyser;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.statement.*;
import main.ast.node.expression.*;
import main.ast.node.expression.values.*;
import main.ast.type.arrayType.ArrayType;
import main.symbolTable.*;
import main.symbolTable.symbolTableVariableItem.*;
import main.symbolTable.itemException.*;
import main.visitor.VisitorImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NameAnalyser extends VisitorImpl {
    private SymbolTableConstructor symbolTableConstructor;
    private TraverseState traverseState;
    private SymbolTableActorParentLinker symbolTableActorLinker;
    private ArrayList<String> nameErrors;
    private int lastIndexOfVariable;

    public NameAnalyser()
    {
        symbolTableConstructor = new SymbolTableConstructor();
        symbolTableActorLinker = new SymbolTableActorParentLinker();
        nameErrors = new ArrayList<>();
        lastIndexOfVariable = 0;
        setState(TraverseState.symbolTableConstruction);
    }

    public int numOfErrors()
    {
        return nameErrors.size();
    }

    private void switchState()
    {
        if(traverseState == TraverseState.symbolTableConstruction)
            setState(TraverseState.errorCatching);
        else if(traverseState == TraverseState.errorCatching)
            setState(TraverseState.PrintError);
        else
            setState(TraverseState.Exit);
    }

    private void setState(TraverseState traverseState)
    {
        this.traverseState = traverseState;
    }

    private void checkForPropertyRedefinition(ActorDeclaration actorDeclaration)
    {
        String name = actorDeclaration.getName().getName();
        if(name.indexOf('$') != -1)
            nameErrors.add("Line:" + actorDeclaration.getName().getLine() + ":Redefinition of actor " + name.substring(name.indexOf('$') + 1));
        try
        {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name);
            SymbolTable next = actorItem.getActorSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("there is an error in pushing class symbol table");
        }
    }

    private void checkForPropertyRedefinitionInParentScopes(VarDeclaration varDeclaration)
    {
        String name = varDeclaration.getIdentifier().getName();
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        String variableKey = SymbolTableVariableItem.STARTKEY + name;
        SymbolTable current = SymbolTable.top.getPreSymbolTable();
        visitedSymbolTables.add(SymbolTable.top);
        while(current != null &&  !visitedSymbolTables.contains(current))
        {
            visitedSymbolTables.add(current);
            try {
                current.getInCurrentScope(variableKey);
                SymbolTableVariableItem variable = (SymbolTableVariableItem) SymbolTable.top.getInCurrentScope(variableKey);
//                variable.setName("$" + variable.getName());
//                SymbolTable.top.updateInCurrentScope(variableKey , variable);
                nameErrors.add("Line:" + varDeclaration.getIdentifier().getLine() + ":Redefinition of variable " + name.substring(name.indexOf('$') + 1));
                return;
            }
            catch(ItemNotFoundException itemNotFound)
            {
                current = current.getPreSymbolTable();
            }
        }
    }

    private void checkForPropertyRedefinition(VarDeclaration varDeclaration)
    {
        String name = varDeclaration.getIdentifier().getName();
        if(name.indexOf('$') != -1) {
            nameErrors.add("Line:" + varDeclaration.getIdentifier().getLine() + ":Redefinition of variable " + name.substring(name.indexOf('$') + 1));
            return;
        }
        try {
            SymbolTableVariableItem varItem = (SymbolTableVariableItem) SymbolTable.top.getInCurrentScope(SymbolTableVariableItem.STARTKEY + name);
            varItem.setIndex(lastIndexOfVariable++);
            SymbolTable.top.updateInCurrentScope(SymbolTableVariableItem.STARTKEY + name , varItem);
            if(varItem instanceof SymbolTableActorVariableItem || varItem instanceof SymbolTableKnownActorItem)
                checkForPropertyRedefinitionInParentScopes(varDeclaration);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("an error occurred");
        }
    }

    private void checkForPropertyRedefinitionInParentScope(MsgHandlerDeclaration msgHandlerDeclaration)
    {
        String name = msgHandlerDeclaration.getName().getName();
        String methodKey = SymbolTableHandlerItem.STARTKEY + name;
        SymbolTable current = SymbolTable.top.getPreSymbolTable();
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        visitedSymbolTables.add(SymbolTable.top);
        while(current != null && !visitedSymbolTables.contains(current))
        {
            visitedSymbolTables.add(current);
            try {
                current.getInCurrentScope(methodKey);
                SymbolTableHandlerItem method = (SymbolTableHandlerItem) SymbolTable.top.getInCurrentScope(methodKey);
//                method.setName("$" + method.getName());
//                SymbolTable.top.updateInCurrentScope(methodKey , method);
                nameErrors.add("Line:" + msgHandlerDeclaration.getName().getLine() + ":Redefinition of msghandler " + name.substring(name.indexOf('$') + 1));
                return;
            }
            catch(ItemNotFoundException itemNotFound)
            {
                current = current.getPreSymbolTable();
            }
        }
    }

    private void checkForPropertyRedefinition(HandlerDeclaration handlerDeclaration)
    {
        String name = handlerDeclaration.getName().getName();
        SymbolTable next;
        String methodKey = SymbolTableHandlerItem.STARTKEY + name;
        try
        {
            next = ((SymbolTableHandlerItem)SymbolTable.top.getInCurrentScope(methodKey)).getHandlerSymbolTable();
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("an error occurred in pushing method symbol table " + handlerDeclaration.getName().getName());
            return;
        }
        if(name.indexOf('$') != -1) {
            nameErrors.add("Line:" + handlerDeclaration.getName().getLine() + ":Redefinition of msghandler " + name.substring(name.indexOf('$') + 1));
            SymbolTable.push(next);
            return;
        }
        if(handlerDeclaration instanceof MsgHandlerDeclaration)
            checkForPropertyRedefinitionInParentScope((MsgHandlerDeclaration) handlerDeclaration);

        SymbolTable.push(next);
    }

    private void pushMainSymbolTable(){
        try{
            SymbolTableMainItem mainItem = (SymbolTableMainItem) SymbolTable.root.getInCurrentScope(SymbolTableMainItem.STARTKEY + "main");
            SymbolTable next = mainItem.getMainSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("there is an error in pushing class symbol table");
        }
    }

    @Override
    public void visit(Program program){

        while(traverseState != TraverseState.Exit) {
            if (traverseState == TraverseState.symbolTableConstruction)
                symbolTableConstructor.constructProgramSymbolTable();
            else if (traverseState == TraverseState.errorCatching)
                symbolTableActorLinker.findActorsParents(program);
            else if(traverseState == TraverseState.PrintError) {
                for (String error : nameErrors)
                    System.out.println(error);
                return;
            }

            for(ActorDeclaration actorDeclaration : program.getActors())
                actorDeclaration.accept(this);
            program.getMain().accept(this);
            switchState();
        }
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        if (traverseState == TraverseState.symbolTableConstruction)
            symbolTableConstructor.construct(actorDeclaration);
        else if (traverseState == TraverseState.errorCatching) {
            checkForPropertyRedefinition(actorDeclaration);
            //TODO: check cyclic inheritance
            if(symbolTableActorLinker.hasCyclicInheritance(actorDeclaration)){
                nameErrors.add("Line:" + actorDeclaration.getLine() + ":Cyclic inheritance involving actor " + actorDeclaration.getName().getName());
            }
            if (actorDeclaration.getQueueSize() <= 0) {
                nameErrors.add("Line:" + actorDeclaration.getLine() + ":Queue size must be positive");
            }
        }

        visitExpr(actorDeclaration.getName());
        visitExpr(actorDeclaration.getParentName());
        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);
        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);
        if(actorDeclaration.getInitHandler() != null)
            actorDeclaration.getInitHandler().accept(this);
        for(MsgHandlerDeclaration msgHandlerDeclaration: actorDeclaration.getMsgHandlers())
            msgHandlerDeclaration.accept(this);
        SymbolTable.pop();
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        if(handlerDeclaration == null)
            return;
        if (traverseState == TraverseState.symbolTableConstruction)
            symbolTableConstructor.construct(handlerDeclaration);
        else if (traverseState == TraverseState.errorCatching)
            checkForPropertyRedefinition(handlerDeclaration);

        visitExpr(handlerDeclaration.getName());
        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);
        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);
        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);
        SymbolTable.pop();
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if(varDeclaration == null)
            return;
        if(traverseState == TraverseState.errorCatching) {
            checkForPropertyRedefinition(varDeclaration);
            if(varDeclaration.getType() instanceof ArrayType){
                if (((ArrayType)varDeclaration.getType()).getSize() <= 0){
                    nameErrors.add("Line:" + varDeclaration.getType().getLine() + ":Array size must be positive");
                }
            }
        }

        visitExpr(varDeclaration.getIdentifier());
    }

    @Override
    public void visit(Main programMain) {
        if(programMain == null)
            return;

        if (traverseState == TraverseState.symbolTableConstruction)
            symbolTableConstructor.construct(programMain);
        else if (traverseState == TraverseState.errorCatching)
            pushMainSymbolTable();

        for(ActorInstantiation mainActor : programMain.getMainActors())
            mainActor.accept(this);
        SymbolTable.pop();
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if(actorInstantiation == null)
            return;

        if (traverseState == TraverseState.errorCatching)
            checkForPropertyRedefinition(actorInstantiation);

        visitExpr(actorInstantiation.getIdentifier());
        for(Identifier knownActor : actorInstantiation.getKnownActors())
            visitExpr(knownActor);
        for(Expression initArg : actorInstantiation.getInitArgs())
            visitExpr(initArg);
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        if(unaryExpression == null)
            return;

        visitExpr(unaryExpression.getOperand());
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        if(binaryExpression == null)
            return;

        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());
    }

    @Override
    public void visit(ArrayCall arrayCall) {

        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if(actorVarAccess == null)
            return;

        visitExpr(actorVarAccess.getVariable());
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;
    }

    @Override
    public void visit(Self self) {
    }

    @Override
    public void visit(Sender sender) {
    }

    @Override
    public void visit(BooleanValue value) {
    }

    @Override
    public void visit(IntValue value) {
    }

    @Override
    public void visit(StringValue value) {
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        if(msgHandlerCall == null) {
            return;
        }
        try {
            visitExpr(msgHandlerCall.getInstance());
            visitExpr(msgHandlerCall.getMsgHandlerName());
            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);
        }
        catch(NullPointerException npe) {
            System.out.println("null pointer exception occurred");
        }
    }

    @Override
    public void visit(Block block) {
        if(block == null)
            return;
        for(Statement statement : block.getStatements())
            visitStatement(statement);
    }

    @Override
    public void visit(Conditional conditional) {
        visitExpr(conditional.getExpression());
        visitStatement(conditional.getThenBody());
        visitStatement(conditional.getElseBody());
    }

    @Override
    public void visit(For loop) {
        visitStatement(loop.getInitialize());
        visitExpr(loop.getCondition());
        visitStatement(loop.getUpdate());
        visitStatement(loop.getBody());
    }

    @Override
    public void visit(Break b) {
    }

    @Override
    public void visit(Continue c) {
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;
        visitExpr(print.getArg());
    }

    @Override
    public void visit(Assign assign) {
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());
    }
}
