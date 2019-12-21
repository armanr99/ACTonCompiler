package main.visitor.semanticAnalyser;

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

public class SemanticAnalyser extends VisitorImpl {
    private SymbolTableConstructor symbolTableConstructor;
    private TraverseState traverseState;
    private SymbolTableActorParentLinker symbolTableActorLinker;
    private ArrayList<String> semanticErrors;
    private boolean inHandler = false;
    private boolean inMsgHandlerCall = false;

    public SemanticAnalyser()
    {
        symbolTableConstructor = new SymbolTableConstructor();
        symbolTableActorLinker = new SymbolTableActorParentLinker();
        semanticErrors = new ArrayList<>();
        setState(TraverseState.symbolTableConstruction);
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

    private void addVariableNotDeclaredError(Identifier identifier) {
        String error = "Line:";
        error += identifier.getLine();
        error += ":variable ";
        error += identifier.getName();
        error += " is not declared";
        semanticErrors.add(error);
    }

    private void checkVariableDeclared(Identifier identifier)
    {
        String name = SymbolTableVariableItem.STARTKEY + identifier.getName();
        try {
            SymbolTable.top.getInCurrentScope(name);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            try {
                checkVariableDeclaredInParents(identifier);
            }
            catch(ItemNotFoundException itemNotFoundParent) {
                addVariableNotDeclaredError(identifier);
            }
        }
        catch(NullPointerException e) {
            System.out.println("there is an error in pushing class symbol table");
        }
    }

    private void checkVariableDeclaredInParents(Identifier identifier) throws ItemNotFoundException
    {
        String name = SymbolTableVariableItem.STARTKEY  + identifier.getName();
        SymbolTable current = SymbolTable.top.getPreSymbolTable();
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        visitedSymbolTables.add(SymbolTable.top);
        while(current != null && !visitedSymbolTables.contains(current))
        {
            visitedSymbolTables.add(current);
            try {
                current.getInCurrentScope(name);
                return;
            }
            catch(ItemNotFoundException itemNotFound)
            {
                current = current.getPreSymbolTable();
            }
        }
        throw new ItemNotFoundException();
    }

    private void pushActorDeclarationSymbolTable(ActorDeclaration actorDeclaration) {
        try
        {
            String name = actorDeclaration.getName().getName();
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name);
            SymbolTable next = actorItem.getActorSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("there is an error in pushing class symbol table");
        }
    }

    private void pushHandlerDeclarationSymbolTable(HandlerDeclaration handlerDeclaration) {
        String name = handlerDeclaration.getName().getName();
        SymbolTable next;
        String methodKey = SymbolTableHandlerItem.STARTKEY + name;
        try
        {
            next = ((SymbolTableHandlerItem)SymbolTable.top.getInCurrentScope(methodKey)).getHandlerSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("an error occurred in pushing method symbol table " + handlerDeclaration.getName().getName());
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
                for (String error : semanticErrors)
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
            pushActorDeclarationSymbolTable(actorDeclaration);
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
        else if (traverseState == TraverseState.errorCatching) {
            pushHandlerDeclarationSymbolTable(handlerDeclaration);
        }

        visitExpr(handlerDeclaration.getName());
        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);
        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);
        inHandler = true;
        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);
        inHandler = false;
        SymbolTable.pop();
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if(varDeclaration == null)
            return;
        if(traverseState == TraverseState.errorCatching) {
            //TODO
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

        if (traverseState == TraverseState.errorCatching) {
            //TODO
        }

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

        if (traverseState == TraverseState.errorCatching) {
            if(inHandler && !inMsgHandlerCall) {
                checkVariableDeclared(identifier);
            }
        }
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
            inMsgHandlerCall = true;
            visitExpr(msgHandlerCall.getMsgHandlerName());
            inMsgHandlerCall = false;
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
