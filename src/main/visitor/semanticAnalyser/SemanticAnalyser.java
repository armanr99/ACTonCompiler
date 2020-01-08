package main.visitor.semanticAnalyser;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.statement.*;
import main.ast.node.expression.*;
import main.ast.node.expression.values.*;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.*;
import main.symbolTable.symbolTableVariableItem.*;
import main.symbolTable.itemException.*;
import main.visitor.VisitorImpl;
import org.antlr.v4.runtime.atn.NotSetTransition;
import java.util.ArrayList;

public class SemanticAnalyser extends VisitorImpl {
    private ArrayList<String> semanticErrors;
    private boolean inHandler = false;
    private boolean inMsgHandlerCall = false;
    private boolean inKnownActors = false;
    private boolean inInitial = false;
    private boolean inActorVarAccess = false;
    private int inFor = 0;
    private boolean inActorInstantiationKnownActors = false;
    private boolean inActorInstantiationInits = false;

    ActorDeclaration currentActor = null;

    public SemanticAnalyser()
    {
        semanticErrors = new ArrayList<>();
    }

    public int numOfErrors()
    {
        return semanticErrors.size();
    }

    private void pushMainSymbolTable(){
        try{
            SymbolTableMainItem mainItem = (SymbolTableMainItem) SymbolTable.root.getInCurrentScope(SymbolTableMainItem.STARTKEY + "main");
            SymbolTable next = mainItem.getMainSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("1: there is an error in pushing class symbol table");
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
            System.out.println("2: there is an error in pushing class symbol table");
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
            System.out.println("3: there is an error in pushing class symbol table");
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

    private void addActorNotDeclaredError(Identifier identifier) {
        String error = "Line:";
        error += identifier.getLine();
        error += ":actor ";
        error += identifier.getName();
        error += " is not declared";
        semanticErrors.add(error);
    }

    private void addSenderInInitialError(Sender sender) {
        String error = "Line:";
        error += sender.getLine();
        error += ":no sender in initial msghandler";
        semanticErrors.add(error);
    }

    private void addBreakContinueError(Statement stmt) {
        String error = "Line:";
        error += stmt.getLine();
        if(stmt instanceof Break)
            error += ":break";
        else
            error += ":continue";
        error += " statement not within loop";
        semanticErrors.add(error);
    }

    private void addBadArrayInstanceError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":Array instance must be array type";
        semanticErrors.add(error);
    }

    private void addBadArrayIndexError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":Array index must be int";
        semanticErrors.add(error);
    }

    private void addUnsupportedOperandForBinaryOperatorError(BinaryExpression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":unsupported operand type for ";
        error += expr.getBinaryOperator().name();
        semanticErrors.add(error);
    }

    private void addUnsupportedOperandForAssignError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":unsupported operand type for ";
        error += BinaryOperator.assign;
        semanticErrors.add(error);
    }

    private void addUnsupportedOperandForUnaryOperatorError(UnaryExpression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":unsupported operand type for ";
        error += expr.getUnaryOperator().name();
        semanticErrors.add(error);
    }

    private void addArraySizeError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":operation assign requires equal array sizes";
        semanticErrors.add(error);
    }

    private void addRValueAssignmentError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":left side of assignment must be a valid lvalue";
        semanticErrors.add(error);
    }

    private void addDecrementRValueOperandError(UnaryExpression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":lvalue required as ";
        UnaryOperator operator = expr.getUnaryOperator();
        if(operator == UnaryOperator.postinc || operator == UnaryOperator.preinc)
            error += "increment";
        else
            error += "decrement";
        error += " operand";
        semanticErrors.add(error);
    }

    private void addMsgHandlerNotInActorError(MsgHandlerCall call) {
        if(call.getInstance().getType() instanceof NoType)
            return;

        String error = "Line:";
        error += call.getLine();
        error += ":there is no msghandler name ";
        error += call.getMsgHandlerName().getName();
        error += " in actor ";
        error += ( (ActorType) call.getInstance().getType() ).getName().getName();
        semanticErrors.add(error);
    }

    private void addSenderNotActorError(MsgHandlerCall call) {
        String error = "Line:";
        error += call.getLine();
        error += ":variable ";
        error += ((Identifier)call.getInstance()).getName();
        error += " is not callable";
        semanticErrors.add(error);
    }

    private void addConditionTypeError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":condition type must be Boolean";
        semanticErrors.add(error);
    }

    private void addUnsupportedPrintTypeError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":unsupported type for print";
        semanticErrors.add(error);
    }

    private void addKnownActorError(ActorInstantiation instantiation) {
        String error = "Line:";
        error += instantiation.getLine();
        error += ":knownactors does not match with definition";
        semanticErrors.add(error);
    }

    private void addInitialArgumentError(ActorInstantiation instantiation) {
        String error = "Line:";
        error += instantiation.getLine();
        error += ":arguments do not match with definition";
        semanticErrors.add(error);
    }

    private void addMsgHandlerArgumentError(MsgHandlerCall call) {
        String error = "Line:";
        error += call.getLine();
        error += ":arguments do not match with definition";
        semanticErrors.add(error);
    }
    private void addSelfOutOfActorError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":self doesn't refer to any actor";
        semanticErrors.add(error);
    }

    private void addSenderOutOfActorError(Expression expr) {
        String error = "Line:";
        error += expr.getLine();
        error += ":sender doesn't refer to any actor";
        semanticErrors.add(error);
    }

    private void checkVariableDeclared(Identifier identifier)
    {
        String name = SymbolTableVariableItem.STARTKEY + identifier.getName();
        try {
            if(inActorVarAccess) {
                if(currentActor == null)
                    return;
                SymbolTable.top.getPreSymbolTable().get(SymbolTableVariableItem.STARTKEY + identifier.getName());
            }
            else {
                SymbolTable.top.get(name);
            }
        }
        catch(ItemNotFoundException itemNotFound)
        {
            addVariableNotDeclaredError(identifier);
        }
    }

    private void checkActorDeclared(Identifier identifier) {
        if(identifier == null)
            return;

        String name = SymbolTableActorItem.STARTKEY + identifier.getName();
        try {
            SymbolTable.top.get(name);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            addActorNotDeclaredError(identifier);
        }
    }

    private void checkHandlerExists(MsgHandlerCall msgHandlerCall) {
        Expression instance = msgHandlerCall.getInstance();
        Identifier handlerName = msgHandlerCall.getMsgHandlerName();

        try {
            if(instance instanceof Self) {
                SymbolTable.top.get(SymbolTableHandlerItem.STARTKEY + handlerName.getName());
            } else if(instance instanceof Sender) {
                //sender handler?
            }
            else {
                SymbolTableKnownActorItem knownActorItem = (SymbolTableKnownActorItem) SymbolTable.top.get(SymbolTableKnownActorItem.STARTKEY + ((Identifier)instance).getName());
                SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType)knownActorItem.getType()).getName().getName());
                SymbolTable actorSymbolTable = actorItem.getActorSymbolTable();
                actorSymbolTable.get(SymbolTableHandlerItem.STARTKEY + handlerName.getName());
            }
        } catch(ItemNotFoundException e) {
            addMsgHandlerNotInActorError(msgHandlerCall);
        }
    }

    private void checkConditionExpression(Expression expr) {
        if(expr == null)
            return;

        if(!(typeCheck(expr) instanceof BooleanType) && !(typeCheck(expr) instanceof NoType)) {
            addConditionTypeError(expr);
        }
    }

    private void checkPrintArg(Expression expr) {
        if(typeCheck(expr) instanceof ActorType)
            addUnsupportedPrintTypeError(expr);
    }

    private void checkHandlerArgumentsOrder(MsgHandlerCall msgHandlerCall) {
        Expression instance = msgHandlerCall.getInstance();
        Identifier handlerName = msgHandlerCall.getMsgHandlerName();

        ArrayList<Expression> arguments = msgHandlerCall.getArgs();
        SymbolTableHandlerItem handlerItem;

        try {
            if(instance instanceof Self) {
                handlerItem = (SymbolTableHandlerItem)SymbolTable.top.get(SymbolTableHandlerItem.STARTKEY + handlerName.getName());

            } else if(instance instanceof Sender) {
                //sender handler?
                return;
            }
            else {
                SymbolTableKnownActorItem knownActorItem = (SymbolTableKnownActorItem) SymbolTable.top.get(SymbolTableKnownActorItem.STARTKEY + ((Identifier)instance).getName());
                SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType)knownActorItem.getType()).getName().getName());
                SymbolTable actorSymbolTable = actorItem.getActorSymbolTable();
                handlerItem = (SymbolTableHandlerItem)actorSymbolTable.get(SymbolTableHandlerItem.STARTKEY + handlerName.getName());
            }
            ArrayList<VarDeclaration> argumentDeclarations = handlerItem.getHandlerDeclaration().getArgs();
            if (arguments.size() != argumentDeclarations.size()) {
                addMsgHandlerArgumentError(msgHandlerCall);
                return;
            }
            for (int i = 0; i < arguments.size(); i++) {
                if(typeCheck(arguments.get(i)) instanceof NoType)
                    continue;
                if (!typeCheck(arguments.get(i)).toString().equals(argumentDeclarations.get(i).getType().toString())) {
                    addMsgHandlerArgumentError(msgHandlerCall);
                    return;
                }
                if(typeCheck(arguments.get(i)) instanceof ArrayType) {
                    if(((ArrayType)typeCheck(arguments.get(i))).getSize() != ((ArrayType)(argumentDeclarations.get(i).getType())).getSize()) {
                        addMsgHandlerArgumentError(msgHandlerCall);
                        return;
                    }
                }
            }
        } catch(ItemNotFoundException e) {

        }
    }

    private void checkInitialArguments(ActorInstantiation instantiation) {
        Identifier instance = instantiation.getIdentifier();

        ArrayList<Expression> arguments = instantiation.getInitArgs();
        SymbolTableHandlerItem handlerItem;

        try {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType)instance.getType()).getName().getName());
            SymbolTable actorSymbolTable = actorItem.getActorSymbolTable();
            handlerItem = (SymbolTableHandlerItem)actorSymbolTable.get(SymbolTableHandlerItem.STARTKEY + "initial");
            ArrayList<VarDeclaration> argumentDeclarations = handlerItem.getHandlerDeclaration().getArgs();
            if (arguments.size() != argumentDeclarations.size()) {
                addInitialArgumentError(instantiation);
                return;
            }
            for (int i = 0; i < arguments.size(); i++) {
                if(typeCheck(arguments.get(i)) instanceof NoType)
                    continue;
                if (!typeCheck(arguments.get(i)).toString().equals(argumentDeclarations.get(i).getType().toString())) {
                    addInitialArgumentError(instantiation);
                    return;
                }
                if(typeCheck(arguments.get(i)) instanceof ArrayType) {
                    if(((ArrayType)typeCheck(arguments.get(i))).getSize() != ((ArrayType)(argumentDeclarations.get(i).getType())).getSize()) {
                        addInitialArgumentError(instantiation);
                        return;
                    }
                }
            }
        } catch(ItemNotFoundException e) {
            // No initial
            if(arguments.size() > 0) {
                addInitialArgumentError(instantiation);
            }
        }
    }

    private void checkKnownActorsOrder(ActorInstantiation instantiation) {
        try {
            ArrayList<Identifier> knownActors = instantiation.getKnownActors();
            Identifier actor = ((ActorType) typeCheck((Expression)(instantiation.getIdentifier()))).getName();
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + actor.getName());
            ArrayList<VarDeclaration> knownActorsDeclaration = actorItem.getActorDeclaration().getKnownActors();
            if (knownActors.size() != knownActorsDeclaration.size()) {
                addKnownActorError(instantiation);
                return;
            }
            for (int i = 0; i < knownActors.size(); i++) {
                if((typeCheck((Expression)(knownActors.get(i))) instanceof NoType)) {
                    continue;
                }
                if (!(typeCheck((Expression)(knownActors.get(i))) instanceof ActorType) ||
                        !(hasAncestor((ActorType) typeCheck((Expression)(knownActors.get(i))), (ActorType) knownActorsDeclaration.get(i).getType()))) {
                    addKnownActorError(instantiation);
                    return;
                }
            }
        }
        catch (ItemNotFoundException e) {
            //What?
        }
    }

    private Boolean hasAncestor(ActorType actor1, ActorType actor2) {
        if(actor1.getName().getName().equals(actor2.getName().getName())) {
            return true;
        }
        try {
            SymbolTableActorItem actorItem = ((SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + actor1.getName().getName()));
            String parent = actorItem.getParentName();

            while(parent != null) {
                if(parent.equals(actor2.getName().getName()))
                    return true;

                String parentKey = SymbolTableActorItem.STARTKEY + parent;
                SymbolTableActorItem parentItem;

                try {
                    parentItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(parentKey);
                    parent = parentItem.getParentName();
                } catch (ItemNotFoundException e1) {
                    break;
                    // Parent Not Found
                }
            }
        }
        catch(ItemNotFoundException itemNotFound)
        {
            return false;
        }

        return false;
    }

    private  Boolean isLValue(Expression expr) {
        return (expr instanceof Identifier ||
                expr instanceof ArrayCall ||
                expr instanceof ActorVarAccess);
    }

    private void printErrors() {
        for(String error : semanticErrors)
            System.out.println(error);
    }

    @Override
    public void visit(Program program) {
        for(ActorDeclaration actorDeclaration : program.getActors())
            actorDeclaration.accept(this);
        program.getMain().accept(this);
        printErrors();
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        pushActorDeclarationSymbolTable(actorDeclaration);

        currentActor = actorDeclaration;

        visitExpr(actorDeclaration.getName());

        Identifier parentName = actorDeclaration.getParentName();
        checkActorDeclared(parentName);
        visitExpr(parentName);

        inKnownActors = true;
        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);
        inKnownActors = false;

        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);

        inInitial = true;
        if(actorDeclaration.getInitHandler() != null)
            actorDeclaration.getInitHandler().accept(this);
        inInitial = false;

        for(MsgHandlerDeclaration msgHandlerDeclaration: actorDeclaration.getMsgHandlers())
            msgHandlerDeclaration.accept(this);

        currentActor = null;

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
    if(inKnownActors)
        checkActorDeclared(((ActorType) varDeclaration.getType()).getName());

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

        checkActorDeclared(((ActorType)actorInstantiation.getType()).getName());

        visitExpr(actorInstantiation.getIdentifier());
        inActorInstantiationKnownActors = true;
        for(Identifier knownActor : actorInstantiation.getKnownActors())
            visitExpr(knownActor);
        inActorInstantiationKnownActors = false;
        checkKnownActorsOrder(actorInstantiation);

        inActorInstantiationInits = true;
        for(Expression initArg : actorInstantiation.getInitArgs())
            visitExpr(initArg);
        inActorInstantiationInits = false;

        checkInitialArguments(actorInstantiation);
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        if(unaryExpression == null)
            return;

        typeCheck((Expression)unaryExpression);
        visitExpr(unaryExpression.getOperand());
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        if(binaryExpression == null)
            return;

        typeCheck((Expression)binaryExpression);
        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        typeCheck((Expression)arrayCall);
        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if(actorVarAccess == null)
            return;

        typeCheck((Expression)actorVarAccess);
        visitExpr(actorVarAccess.getSelf());
        inActorVarAccess = true;
        visitExpr(actorVarAccess.getVariable());
        inActorVarAccess = false;
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;

        if((inHandler || inActorInstantiationKnownActors || inActorInstantiationInits) && !inMsgHandlerCall && !inKnownActors) {
            checkVariableDeclared(identifier);
        }
    }

    @Override
    public void visit(Self self) {
        if(currentActor == null)
            addSelfOutOfActorError(self);
    }

    @Override
    public void visit(Sender sender) {
        if(inInitial)
            addSenderInInitialError(sender);
        if(currentActor == null)
            addSenderOutOfActorError(sender);
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
            if(!(typeCheck(msgHandlerCall.getInstance()) instanceof ActorType || typeCheck(msgHandlerCall.getInstance()) instanceof NoType)) {
                addSenderNotActorError(msgHandlerCall);
            } else {
                checkHandlerExists(msgHandlerCall);
                checkHandlerArgumentsOrder(msgHandlerCall);
            }
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
        Expression condition = conditional.getExpression();
        checkConditionExpression(condition);
        visitExpr(condition);
        visitStatement(conditional.getThenBody());
        visitStatement(conditional.getElseBody());
    }

    @Override
    public void visit(For loop) {
        inFor++;
        visitStatement(loop.getInitialize());
        Expression condition = loop.getCondition();
        checkConditionExpression(condition);
        visitExpr(condition);
        visitStatement(loop.getUpdate());
        visitStatement(loop.getBody());
        inFor--;
    }

    @Override
    public void visit(Break b) {
        if(inFor == 0)
            addBreakContinueError(b);
    }

    @Override
    public void visit(Continue c) {
        if(inFor == 0)
            addBreakContinueError(c);
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;

        Expression arg = print.getArg();
        checkPrintArg(arg);
        visitExpr(arg);
    }

    @Override
    public void visit(Assign assign) {
        typeCheckAssignment(assign.getlValue(), assign.getrValue());
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());
    }

    public Type typeCheck(Expression expr) {
        if(expr.getType() != null)
            return expr.getType();
        if( expr instanceof UnaryExpression )
            return typeCheck( ( UnaryExpression ) expr );
        else if( expr instanceof BinaryExpression )
            return typeCheck( ( BinaryExpression ) expr );
        else if( expr instanceof ArrayCall )
            return typeCheck( ( ArrayCall ) expr );
        else if( expr instanceof ActorVarAccess )
            return typeCheck( ( ActorVarAccess ) expr );
        else if( expr instanceof Identifier )
            return typeCheck( ( Identifier ) expr );
        else if( expr instanceof Self )
            return typeCheck( ( Self ) expr );
        else if( expr instanceof Sender )
            return typeCheck( ( Sender ) expr );
        else if( expr instanceof BooleanValue )
            return typeCheck( ( BooleanValue ) expr );
        else if( expr instanceof IntValue )
            return typeCheck( ( IntValue ) expr );
        else if( expr instanceof StringValue )
            return typeCheck( ( StringValue ) expr );
        else
            return (new NoType());
    }

    public Type typeCheck(BooleanValue expr) {
        expr.setType(new BooleanType());
        return expr.getType();
    }

    public Type typeCheck(IntValue expr) {
        expr.setType(new IntType());
        return expr.getType();
    }

    public Type typeCheck(StringValue expr) {
        expr.setType(new StringType());
        return expr.getType();
    }

    public Type typeCheck(ActorVarAccess expr){
        Identifier variable = expr.getVariable();
        expr.setType(typeCheck((Expression)variable));
        return expr.getType();
    }

    public Type typeCheck(ArrayCall expr) {
        Expression instance = expr.getArrayInstance();
        Type instanceType = typeCheck(instance);

        if(!(instanceType instanceof ArrayType)) {
            expr.setType(new NoType());
            if(!(instanceType instanceof NoType))
                addBadArrayInstanceError(expr);
        }

        Expression index = expr.getIndex();
        if(!(typeCheck(index) instanceof IntType)) {
            expr.setType(new NoType());
            if(!(typeCheck(index) instanceof NoType))
                addBadArrayIndexError(expr);
        }
        else
            expr.setType(new IntType());

        return expr.getType();
    }

    public Type typeCheck(Identifier expr) {
        expr.setType(new NoType());
        try {
            if(inActorVarAccess) {
                if(currentActor == null) {
                    return expr.getType();
                }
                expr.setType(((SymbolTableVariableItem) SymbolTable.top.getPreSymbolTable().get(SymbolTableVariableItem.STARTKEY + expr.getName())).getType());
            }
            else {
                expr.setType(((SymbolTableVariableItem) SymbolTable.top.get(SymbolTableVariableItem.STARTKEY + expr.getName())).getType());
            }
        }
        catch (ItemNotFoundException e) {
            expr.setType(new NoType());
        }
        return expr.getType();
    }

    public Type typeCheck(Self expr) {
        expr.setType(new ActorType(currentActor.getName()));
        return expr.getType();
    }

    public static Type typeCheck(Sender expr) {
        expr.setType(new ActorType(new Identifier( "Sender")));
        return expr.getType();
    }

    public Type typeCheck(BinaryExpression expr) {
        BinaryOperator operator = expr.getBinaryOperator();
        Type leftType = typeCheck(expr.getLeft());
        Type rightType = typeCheck(expr.getRight());

        if(operator == BinaryOperator.mult ||
           operator == BinaryOperator.div ||
           operator == BinaryOperator.add ||
           operator == BinaryOperator.sub  ||
           operator == BinaryOperator.mod) {
            if(leftType instanceof IntType && rightType instanceof IntType) {
                expr.setType(new IntType());
            }
            else if((leftType instanceof NoType || leftType instanceof IntType) &&
                    (rightType instanceof NoType || rightType instanceof IntType)) {
                expr.setType(new NoType());
            }
            else {
                expr.setType(new NoType());
                addUnsupportedOperandForBinaryOperatorError(expr);
            }
        }

        else if(operator == BinaryOperator.gt ||
                operator == BinaryOperator.lt) {
            if(leftType instanceof IntType && rightType instanceof IntType) {
                expr.setType(new BooleanType());
            } else if((leftType instanceof NoType || leftType instanceof IntType) &&
                    (rightType instanceof NoType || rightType instanceof IntType)) {
                expr.setType(new NoType());
            }
            else {
                expr.setType(new NoType());
                addUnsupportedOperandForBinaryOperatorError(expr);
            }
        }

        else if(operator == BinaryOperator.and ||
                operator == BinaryOperator.or) {
            if(leftType instanceof BooleanType && rightType instanceof BooleanType) {
                expr.setType(new BooleanType());
            } else if((leftType instanceof NoType || leftType instanceof BooleanType) &&
                    (rightType instanceof NoType || rightType instanceof BooleanType)) {
                expr.setType(new NoType());
            }
            else {
                expr.setType(new NoType());
                addUnsupportedOperandForBinaryOperatorError(expr);
            }
        }

        else if(operator == BinaryOperator.eq ||
                operator == BinaryOperator.neq) {
            if(leftType instanceof IntType && rightType instanceof IntType ||
               leftType instanceof BooleanType && rightType instanceof BooleanType ||
               leftType instanceof StringType && rightType instanceof StringType ||
               leftType instanceof ActorType && rightType instanceof ActorType) {
                expr.setType(new BooleanType());
            }
            else if(leftType instanceof ArrayType && rightType instanceof ArrayType) {
                ArrayType leftArrayType = (ArrayType)leftType;
                ArrayType rightArrayType = (ArrayType)rightType;

                if(leftArrayType.getSize() != rightArrayType.getSize()) {
                    expr.setType(new NoType());
                    addArraySizeError(expr);
                }
                else {
                    expr.setType(new BooleanType());
                }
            }
            else if(leftType instanceof NoType || rightType instanceof NoType) {
                expr.setType((new NoType()));
            }
            else {
                expr.setType(new NoType());
                addUnsupportedOperandForBinaryOperatorError(expr);
            }
        }

        else if(operator == BinaryOperator.assign) {
            expr.setType(typeCheckAssignment(expr.getLeft(), expr.getRight()));
        }

        return expr.getType();
    }

    public Type typeCheck(UnaryExpression expr) {
        UnaryOperator operator = expr.getUnaryOperator();
        Type operandType = typeCheck(expr.getOperand());

        if(operator == UnaryOperator.not) {
            if(operandType instanceof BooleanType)
                expr.setType(new BooleanType());
            else if(operandType instanceof NoType)
                expr.setType((new NoType()));
            else  {
                expr.setType(new NoType());
                addUnsupportedOperandForUnaryOperatorError(expr);
            }
        } else if(operator == UnaryOperator.minus ||
                  operator == UnaryOperator.predec ||
                  operator == UnaryOperator.preinc ||
                  operator == UnaryOperator.postdec ||
                  operator == UnaryOperator.postinc) {
            if(operandType instanceof IntType) {
                expr.setType(new IntType());
            }
            else if(operandType instanceof NoType) {
                expr.setType(new NoType());
            }
            else
            {
                expr.setType(new NoType());
                addUnsupportedOperandForUnaryOperatorError(expr);
            }
        }

        if(operator == UnaryOperator.predec ||
           operator == UnaryOperator.preinc ||
           operator == UnaryOperator.postdec ||
           operator == UnaryOperator.postinc) {
            if(!isLValue(expr.getOperand())) {
                addDecrementRValueOperandError(expr);
                expr.setType(new NoType());
            }
        }

        return expr.getType();
    }

    private Type typeCheckAssignment(Expression lValue, Expression rValue) {
        Type leftType = typeCheck(lValue);
        Type rightType = typeCheck(rValue);
        Type returnType = new NoType();

        if(leftType instanceof IntType  && rightType instanceof IntType ||
           leftType instanceof BooleanType  && rightType instanceof BooleanType ||
           leftType instanceof StringType && rightType instanceof StringType) {
            returnType = leftType;
        }
        else if(leftType instanceof ArrayType && rightType instanceof ArrayType) {
            ArrayType leftArrayType = (ArrayType)leftType;
            ArrayType rightArrayType = (ArrayType)rightType;

            if(leftArrayType.getSize() != rightArrayType.getSize()) {
                returnType = new NoType();
                addArraySizeError(lValue);
            }
            else {
                returnType = leftType;
            }
        } else if(leftType instanceof ActorType && rightType instanceof ActorType) {
            if(hasAncestor((ActorType) rightType, (ActorType) leftType) || rValue instanceof Sender) {
                returnType = leftType;
            }
            else {
                returnType = new NoType();
                addUnsupportedOperandForAssignError(lValue);
            }
        }
        else if(leftType instanceof NoType || rightType instanceof NoType) {
            returnType = new NoType();
        }
        else {
            returnType = new NoType();
            addUnsupportedOperandForAssignError(lValue);
        }

        if(!isLValue(lValue)) {
            returnType = new NoType();
            addRValueAssignmentError(lValue);
        }
        return returnType;
    }
}