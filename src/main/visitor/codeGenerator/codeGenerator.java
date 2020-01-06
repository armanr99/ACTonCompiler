package main.visitor.codeGenerator;

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

public class codeGenerator extends VisitorImpl {
    private void pushMainSymbolTable(){
        try{
            SymbolTableMainItem mainItem = (SymbolTableMainItem) SymbolTable.root.getInCurrentScope(SymbolTableMainItem.STARTKEY + "main");
            SymbolTable next = mainItem.getMainSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing Main symbol table");
        }
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
            System.out.println("There is an error in pushing ActorDeclaration symbol table");
        }
    }

    private void pushHandlerDeclarationSymbolTable(HandlerDeclaration handlerDeclaration) {
        try
        {
            String name = handlerDeclaration.getName().getName();
            SymbolTableHandlerItem handlerItem = (SymbolTableHandlerItem) SymbolTable.top.getInCurrentScope(SymbolTableHandlerItem.STARTKEY + name);
            SymbolTable next = handlerItem.getHandlerSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing HandlerDeclaration symbol table");
        }
    }

    @Override
    public void visit(Program program){
        for(ActorDeclaration actorDeclaration : program.getActors())
            actorDeclaration.accept(this);
        program.getMain().accept(this);
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        pushActorDeclarationSymbolTable(actorDeclaration);

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

        pushHandlerDeclarationSymbolTable(handlerDeclaration);

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

        visitExpr(varDeclaration.getIdentifier());
    }

    @Override
    public void visit(Main programMain) {
        if(programMain == null)
            return;

        pushMainSymbolTable();

        for(ActorInstantiation mainActor : programMain.getMainActors())
            mainActor.accept(this);

        SymbolTable.pop();
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if(actorInstantiation == null)
            return;

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