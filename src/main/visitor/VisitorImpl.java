package main.visitor;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.*;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.symbolTable.*;
import main.symbolTable.itemException.*;
import main.symbolTable.symbolTableVariableItem.*;

import java.util.*;
import java.util.logging.Handler;

import main.ast.type.*;
import main.ast.type.arrayType.*;

public class VisitorImpl implements Visitor {

    ArrayList<String> preOrder = new ArrayList<String>();
    ArrayList<String> errors = new ArrayList<String>();
    HashSet<String> cyclers = new HashSet<String>();

    boolean secondPass = false;

    public void printPreOrder() {
        for(String node : preOrder) {
            System.out.println(node);
        }
    }

    public void check(Program program) {
        SymbolTable globalSymbolTable = new SymbolTable();
        SymbolTable.push(globalSymbolTable);
        SymbolTable.root = globalSymbolTable;

        visit(program);
        secondPass = true;
        SymbolTable.push(new SymbolTable());
        visit(program);
        secondPass = false;

        if(hasErrors()) {
            printErrors();
        }
        else {
            printPreOrder();
        }

    }

    public void printErrors() {
        for(String error : errors) {
            System.out.println(error);
        }
    }

    public boolean hasErrors() {
        return (errors.size() > 0);
    }

    public void addToPreOrder(String str) {
        if(secondPass) {
            preOrder.add(str);
        }
    }

    private void addActorRedefinitionError(ActorDeclaration actorDeclaration) {
        String error = "Line:";
        error += actorDeclaration.getLine();
        error += ":";
        error += "Redefinition of actor ";
        error += actorDeclaration.getName().getName();
        errors.add(error);
    }

    private void addQueueSizeError(ActorDeclaration actorDeclaration) {
        String error = "Line:";
        error += actorDeclaration.getLine();
        error += ":Queue size must be positive";
        errors.add(error);
    }

    private void addArraySizeError(VarDeclaration varDeclaration) {
        String error = "Line:";
        error += varDeclaration.getLine();
        error += ":Array size must be positive";
        errors.add(error);
    }

    private void addVarRedefinitionError(VarDeclaration varDeclaration) {
        String error = "Line:";
        error += varDeclaration.getLine();
        error += ":Redefinition of variable ";
        error += varDeclaration.getIdentifier().getName();
        errors.add(error);
    }

    private void addHandlerRedefinitionError(HandlerDeclaration handlerDeclaration) {
        String error = "Line:";
        error += handlerDeclaration.getLine();
        error += ":Redefinition of msghandler ";
        error += handlerDeclaration.getName().getName();
        errors.add(error);
    }

    private void addCyclicInheritanceError(ActorDeclaration actorDeclaration) {
        String error = "Line:";
        error += actorDeclaration.getLine();
        error += ":Cyclic inheritance involving actor ";
        error += actorDeclaration.getName().getName();
        errors.add(error);
    }

    private String getActorKey(String actorName) {
        return (SymbolTableActorItem.STARTKEY + actorName);
    }

    private void handleVariableItemFirstPass(SymbolTableVariableItem variableItem, VarDeclaration varDeclaration) {
        String variableItemKey = variableItem.getKey();

        try {
            SymbolTable.top.put(variableItem);

        } catch(ItemAlreadyExistsException e) {
            if(secondPass) {
                addVarRedefinitionError(varDeclaration);
                int count = 1;

                while(true) {
                    try {
                        String newName = variableItemKey + count;
                        variableItem.setName(newName);
                        SymbolTable.top.put(variableItem);
                    } catch(ItemAlreadyExistsException e2) {
                        count++;
                        continue;
                    }
                    break;
                }
            }
        }
    }

    private void handleVariableItemSecondPass(SymbolTableVariableItem variableItem, VarDeclaration varDeclaration, String actorName) {
        String actorKey = getActorKey(actorName);
        SymbolTableItem actorItem;

        try {
            actorItem = SymbolTable.root.get(actorKey);
            HashSet<String> visited = new HashSet<String>();

            visited.add(actorName);
            String parent = ((SymbolTableActorItem) actorItem).getParentName();

            while(parent != null && !visited.contains(parent)) {
                String parentKey = getActorKey(parent);
                SymbolTableItem parentItem;

                try {
                    parentItem = SymbolTable.root.get(parentKey);
                    SymbolTable parentSymbolTable = ((SymbolTableActorItem) parentItem).getActorSymbolTable();
                    try {
                        parentSymbolTable.get(variableItem.getKey());
                        addVarRedefinitionError(varDeclaration);
                        break;
                    } catch(ItemNotFoundException e2) {}

                    visited.add(parent);
                    parent = ((SymbolTableActorItem) parentItem).getParentName();
                } catch (ItemNotFoundException e1) {
                    break;
                    // Parent Not Found
                }
            }

        } catch(ItemNotFoundException e) {
            // Actor Not Found
        }
    }

    private void handleHandlerItemFirstPass(SymbolTableHandlerItem handlerItem, HandlerDeclaration handlerDeclaration) {
        String handlerItemKey = handlerItem.getKey();

        try {
            SymbolTable.top.put(handlerItem);
        } catch(ItemAlreadyExistsException e) {
            if(secondPass) {
                addHandlerRedefinitionError(handlerDeclaration);
                int count = 1;

                while(true) {
                    try {
                        String newName = handlerItemKey + count;
                        handlerItem.setName(newName);
                        SymbolTable.top.put(handlerItem);
                    } catch(ItemAlreadyExistsException e2) {
                        count++;
                        continue;
                    }
                    break;
                }
            }
        }
    }

    private void handleHandlerItemSecondPass(SymbolTableHandlerItem handlerItem, HandlerDeclaration handlerDeclaration, String actorName) {
        String actorKey = getActorKey(actorName);
        SymbolTableItem actorItem;

        try {
            actorItem = SymbolTable.root.get(actorKey);
            HashSet<String> visited = new HashSet<String>();

            visited.add(actorName);
            String parent = ((SymbolTableActorItem) actorItem).getParentName();

            while(parent != null && !visited.contains(parent)) {
                String parentKey = getActorKey(parent);
                SymbolTableItem parentItem;

                try {
                    parentItem = SymbolTable.root.get(parentKey);
                    SymbolTable parentSymbolTable = ((SymbolTableActorItem) parentItem).getActorSymbolTable();
                    try {
                        parentSymbolTable.get(handlerItem.getKey());
                        addHandlerRedefinitionError(handlerDeclaration);
                        break;
                    } catch(ItemNotFoundException e2) {}

                    visited.add(parent);
                    parent = ((SymbolTableActorItem) parentItem).getParentName();
                } catch (ItemNotFoundException e1) {
                    break;
                    // Parent Not Found
                }
            }

        } catch(ItemNotFoundException e) {
            // Actor Not Found
        }
    }

    private void checkCyclicInheritance(ActorDeclaration actorDeclaration) {
        String actorName = actorDeclaration.getName().getName();
        if(cyclers.contains(actorName)) {
            return;
        }

        String actorKey = getActorKey(actorName);
        SymbolTableItem actorItem;

        try {
            actorItem = SymbolTable.root.get(actorKey);
            HashSet<String> visited = new HashSet<String>();

            visited.add(actorName);
            String parent = ((SymbolTableActorItem) actorItem).getParentName();

            while(parent != null) {
                if(visited.contains(parent)) {
                    addCyclicInheritanceError(actorDeclaration);
                    cyclers.addAll(visited);
                    break;
                }

                String parentKey = getActorKey(parent);
                SymbolTableItem parentItem;

                try {
                    parentItem = SymbolTable.root.get(parentKey);
                    visited.add(parent);
                    parent = ((SymbolTableActorItem) parentItem).getParentName();
                } catch (ItemNotFoundException e1) {
                    break;
                    // Parent Not Found
                }
            }
        } catch(ItemNotFoundException e) {
            // Actor Not Found
        }
    }

    @Override
    public void visit(Program program) {
        addToPreOrder(program.toString());

        ArrayList<ActorDeclaration> programActors = program.getActors();
        for(ActorDeclaration actorDec : programActors) {
            actorDec.accept(this);
        }

        Main programMain = program.getMain();
        programMain.accept(this);

        SymbolTable.pop();
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        addToPreOrder(actorDeclaration.toString());

        if(secondPass) {
            checkCyclicInheritance(actorDeclaration);
        }

        SymbolTableActorItem symbolTableActorItem = new SymbolTableActorItem(actorDeclaration);

        try {
            SymbolTable.top.put(symbolTableActorItem);
        } catch(ItemAlreadyExistsException e) {
            if(secondPass) {
                addActorRedefinitionError(actorDeclaration);
                int count = 1;
                while(true) {
                    try {
                        String newName = symbolTableActorItem.getKey() + count;
                        symbolTableActorItem.setName(newName);
                        SymbolTable.top.put(symbolTableActorItem);
                    } catch(ItemAlreadyExistsException e1) {
                        count++;
                        continue;
                    }
                    break;
                }
            }
        }

        SymbolTable actorSymbolTable = new SymbolTable(SymbolTable.top, actorDeclaration.getName().getName());
        symbolTableActorItem.setActorSymbolTable(actorSymbolTable);
        SymbolTable.push(actorSymbolTable);

        Identifier actorName = actorDeclaration.getName();
        actorName.accept(this);

        Identifier parentName = actorDeclaration.getParentName();
        if(parentName != null) {
            parentName.accept(this); //TODO: check if null is valid
        }

        if(secondPass) {
            int actorQueueSize = actorDeclaration.getQueueSize();
            if(actorQueueSize <= 0) {
                addQueueSizeError(actorDeclaration);
            }
        }

        ArrayList<VarDeclaration> actorKnownActors = actorDeclaration.getKnownActors();
        for(VarDeclaration knownActor : actorKnownActors) {
            SymbolTableKnownActorItem symbolTableKnownActorItem = new SymbolTableKnownActorItem(knownActor);


            handleVariableItemFirstPass(symbolTableKnownActorItem, knownActor);
            if(secondPass) {
                handleVariableItemSecondPass(symbolTableKnownActorItem, knownActor, actorDeclaration.getName().getName());
            }

            knownActor.accept(this);
        }

        ArrayList<VarDeclaration> actorVars = actorDeclaration.getActorVars();
        for(VarDeclaration actorVar : actorVars) {
            SymbolTableActorVariableItem symbolTableActorVariableItem = new SymbolTableActorVariableItem(actorVar);

            handleVariableItemFirstPass(symbolTableActorVariableItem, actorVar);
            if (secondPass) {
                handleVariableItemSecondPass(symbolTableActorVariableItem, actorVar, actorDeclaration.getName().getName());
            }
            actorVar.accept(this);
        }

        InitHandlerDeclaration actorInit = actorDeclaration.getInitHandler();
        if(actorInit != null) {
            actorInit.accept(this);
        }

        ArrayList<MsgHandlerDeclaration> actorMsgHandlers = actorDeclaration.getMsgHandlers(); //TODO: check duplicate
        for(MsgHandlerDeclaration msgHandler : actorMsgHandlers) {
            msgHandler.accept(this);
        }

        SymbolTable.pop();
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        addToPreOrder(handlerDeclaration.toString());

        SymbolTableHandlerItem symbolTableHandlerItem = new SymbolTableHandlerItem(handlerDeclaration);

        handleHandlerItemFirstPass(symbolTableHandlerItem, handlerDeclaration);
        if(secondPass) {
            handleHandlerItemSecondPass(symbolTableHandlerItem, handlerDeclaration, SymbolTable.top.getName());
        }

        SymbolTable handlerSymbolTable = new SymbolTable(SymbolTable.top, SymbolTable.top.getName());
        symbolTableHandlerItem.setHandlerSymbolTable(handlerSymbolTable);
        SymbolTable.push(handlerSymbolTable);

        Identifier handlerName = handlerDeclaration.getName();
        handlerName.accept(this);

        ArrayList<VarDeclaration> handlerArgs = handlerDeclaration.getArgs();
        for(VarDeclaration arg : handlerArgs) {
            SymbolTableHandlerArgumentItem symbolTableHandlerArgumentItem = new SymbolTableHandlerArgumentItem(arg);

            handleVariableItemFirstPass(symbolTableHandlerArgumentItem, arg);
            arg.accept(this);
        }

        ArrayList<VarDeclaration> handlerLocalVars = handlerDeclaration.getLocalVars();
        for(VarDeclaration localVar : handlerLocalVars) {
            SymbolTableLocalVariableItem symbolTableLocalVariableItem = new SymbolTableLocalVariableItem(localVar);
            handleVariableItemFirstPass(symbolTableLocalVariableItem, localVar);
            localVar.accept(this);
        }

        ArrayList<Statement> handlerStatements = handlerDeclaration.getBody();
        for(Statement statement : handlerStatements) {
            statement.accept(this);
        }

        SymbolTable.pop();
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        addToPreOrder(varDeclaration.toString());

        if(secondPass) {
            Type varType = varDeclaration.getType();
            if(varType instanceof ArrayType) {
                if(((ArrayType) varType).getSize() <= 0) {
                    addArraySizeError(varDeclaration);
                }
            }
        }

        Identifier varIdentifier = varDeclaration.getIdentifier();
        varIdentifier.accept(this);
    }

    @Override
    public void visit(Main mainActors) {
        addToPreOrder(mainActors.toString());

        ArrayList<ActorInstantiation> actorInstantiations = mainActors.getMainActors();
        for(ActorInstantiation actorInstantiation : actorInstantiations) {
            actorInstantiation.accept(this);
        }
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        addToPreOrder(actorInstantiation.toString());

        Identifier actorName = actorInstantiation.getIdentifier();
        actorName.accept(this);

        ArrayList<Identifier> actorKnownActors = actorInstantiation.getKnownActors();
        for(Identifier knownActor : actorKnownActors) {
            knownActor.accept(this);
        }

        ArrayList<Expression> initArgs = actorInstantiation.getInitArgs();
        for(Expression initArg : initArgs) {
            initArg.accept(this);
        }
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        addToPreOrder(unaryExpression.toString());

        Expression operator = unaryExpression.getOperand();
        operator.accept(this);
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        addToPreOrder(binaryExpression.toString());

        Expression left = binaryExpression.getLeft();
        left.accept(this);

        Expression right = binaryExpression.getRight();
        right.accept(this);
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        addToPreOrder(arrayCall.toString());

        Expression arrayInstance = arrayCall.getArrayInstance();
        arrayInstance.accept(this);

        Expression index = arrayCall.getIndex();
        index.accept(this);
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        addToPreOrder(actorVarAccess.toString());

        Self self = actorVarAccess.getSelf();
        self.accept(this);

        Identifier variableName = actorVarAccess.getVariable();
        variableName.accept(this);
    }

    @Override
    public void visit(Identifier identifier) {
        addToPreOrder(identifier.toString());
    }

    @Override
    public void visit(Self self) {
        addToPreOrder(self.toString());
    }

    @Override
    public void visit(Sender sender) {
        addToPreOrder(sender.toString());
    }

    @Override
    public void visit(BooleanValue value) {
        addToPreOrder(value.toString());
    }

    @Override
    public void visit(IntValue value) {
        addToPreOrder(value.toString());
    }

    @Override
    public void visit(StringValue value) {
        addToPreOrder(value.toString());
    }

    @Override
    public void visit(Block block) {
        addToPreOrder(block.toString());

        ArrayList<Statement> statements = block.getStatements();
        for(Statement statement : statements) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(Conditional conditional) {
        addToPreOrder(conditional.toString());

        Expression expression = conditional.getExpression();
        expression.accept(this);

        Statement thenBody = conditional.getThenBody();
        thenBody.accept(this);

        Statement elseBody = conditional.getElseBody();
        if(elseBody != null) {
            elseBody.accept(this);
        }
    }

    @Override
    public void visit(For loop) {
        addToPreOrder(loop.toString());

        Assign initialize = loop.getInitialize();
        if(initialize != null) {
            initialize.accept(this);
        }

        Expression condition = loop.getCondition();
        if(condition != null) {
            condition.accept(this);
        }

        Assign update = loop.getUpdate();
        if(update != null) {
            update.accept(this);
        }

        Statement body = loop.getBody();
        body.accept(this);
    }

    @Override
    public void visit(Break breakLoop) {
        addToPreOrder(breakLoop.toString());
    }

    @Override
    public void visit(Continue continueLoop) {
        addToPreOrder(continueLoop.toString());
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        addToPreOrder(msgHandlerCall.toString());

        Expression instance = msgHandlerCall.getInstance();
        instance.accept(this);

        Identifier msgHandlerName = msgHandlerCall.getMsgHandlerName();
        msgHandlerName.accept(this);

        ArrayList<Expression> args = msgHandlerCall.getArgs();
        for(Expression arg : args) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(Print print) {
        addToPreOrder(print.toString());

        Expression arg = print.getArg();
        arg.accept(this);
    }

    @Override
    public void visit(Assign assign) {
        addToPreOrder(assign.toString());

        Expression lValue = assign.getlValue();
        lValue.accept(this);

        Expression rValue = assign.getrValue();
        rValue.accept(this);
    }
}
