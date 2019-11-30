grammar acton;

@header
{
    package parsers;
    import main.ast.node.*;
    import main.ast.node.declaration.*;
    import main.ast.node.declaration.handler.*;
    import main.ast.node.statement.*;
    import main.ast.node.expression.*;
    import main.ast.node.expression.operators.*;
    import main.ast.node.expression.values.*;
    import main.ast.type.*;
    import main.ast.type.actorType.*;
    import main.ast.type.arrayType.*;
    import main.ast.type.primitiveType.*;
    import java.util.ArrayList;
}

@members
{
    void setNodeLine(Node node, int line) {
        node.setLine(line);
    }
}

program returns [Program p]
    :
        { $p = new Program(); }
        (actorDeclaration { $p.addActor($actorDeclaration.synDec); })+
        mainDeclaration { $p.setMain($mainDeclaration.synNode); }
    ;

actorDeclaration returns [ActorDeclaration synDec]
    :   ACTOR actorName = identifier
        {
            $synDec = new ActorDeclaration($actorName.synExpr);
            setNodeLine($synDec, $ctx.start.getLine());
        }

        (EXTENDS parentName = identifier { $synDec.setParentName($parentName.synExpr); })?
        LPAREN queueSize = INTVAL RPAREN { $synDec.setQueueSize($queueSize.int); }

        LBRACE

        (
            KNOWNACTORS
            LBRACE
            (
                knownTypeName = identifier knownName = identifier SEMICOLON
                {
                    Type knownType = new ActorType($knownTypeName.synExpr);
                    VarDeclaration knownVarDec = new VarDeclaration($knownName.synExpr, knownType);
                    setNodeLine(knownVarDec, $knownTypeName.start.getLine());
                    $synDec.addKnownActor(knownVarDec);
                }
            )*
            RBRACE
        )

        (
            ACTORVARS
            LBRACE
                varDeclarations { $synDec.setActorVars($varDeclarations.varDecs); }
            RBRACE
        )

        (initHandlerDeclaration { $synDec.setInitHandler($initHandlerDeclaration.synHandlerDec); })?
        (msgHandlerDeclaration { $synDec.addMsgHandler($msgHandlerDeclaration.synHandlerDec); })*

        RBRACE
    ;

mainDeclaration returns [Main synNode]
    @init
    {
        $synNode = new Main();
        setNodeLine($synNode, $ctx.start.getLine());
    }
    :   MAIN
    	LBRACE
        (actorInstantiation { $synNode.addActorInstantiation($actorInstantiation.synVarDec); })*
    	RBRACE
    ;

actorInstantiation returns [ActorInstantiation synVarDec]
    :	type = identifier name = identifier
        {
            $synVarDec = new ActorInstantiation(new ActorType($type.synExpr), $name.synExpr);
            setNodeLine($synVarDec, $ctx.start.getLine());
        }
     	LPAREN
     	(
            actor1 = identifier { $synVarDec.addKnownActor($actor1.synExpr); }
            (COMMA actor2 = identifier { $synVarDec.addKnownActor($actor2.synExpr); })*
        |
     	)
     	RPAREN
     	COLON
     	LPAREN expressionList { $synVarDec.setInitArgs($expressionList.argExprs); } RPAREN
     	SEMICOLON
    ;

initHandlerDeclaration returns [InitHandlerDeclaration synHandlerDec]
    :	MSGHANDLER name = INITIAL
        {
            $synHandlerDec = new InitHandlerDeclaration(new Identifier($name.text));
            setNodeLine($synHandlerDec, $ctx.start.getLine());
        }
        LPAREN argDeclarations RPAREN { $synHandlerDec.setArgs($argDeclarations.argDecs); }
     	LBRACE
     	varDeclarations { $synHandlerDec.setLocalVars($varDeclarations.varDecs); }
     	(statement { $synHandlerDec.addStatement($statement.synStmt); })*
     	RBRACE
    ;

msgHandlerDeclaration returns [MsgHandlerDeclaration synHandlerDec]
    :	MSGHANDLER name = identifier
        {
            $synHandlerDec = new MsgHandlerDeclaration($name.synExpr);
            setNodeLine($synHandlerDec, $ctx.start.getLine());
        }
        LPAREN argDeclarations RPAREN { $synHandlerDec.setArgs($argDeclarations.argDecs); }
       	LBRACE
       	varDeclarations { $synHandlerDec.setLocalVars($varDeclarations.varDecs); }
       	(statement { $synHandlerDec.addStatement($statement.synStmt); })*
       	RBRACE
    ;

argDeclarations returns [ArrayList<VarDeclaration> argDecs]
    @init
    {
        $argDecs = new ArrayList<>();
    }
    :	varDec1 = varDeclaration { $argDecs.add($varDec1.synVarDec); }
        (COMMA varDec2 = varDeclaration { $argDecs.add($varDec2.synVarDec); })*
    |
    ;

varDeclarations returns [ArrayList<VarDeclaration> varDecs]
    @init
    {
        $varDecs = new ArrayList<>();
    }
    :
        (varDeclaration SEMICOLON { $varDecs.add($varDeclaration.synVarDec); })*
    ;

varDeclaration returns [VarDeclaration synVarDec]
    :	INT identifier
        {
            $synVarDec = new VarDeclaration($identifier.synExpr, new IntType());
            setNodeLine($synVarDec, $ctx.start.getLine());
        }
    |   STRING identifier
        {
            $synVarDec = new VarDeclaration($identifier.synExpr, new StringType());
            setNodeLine($synVarDec, $ctx.start.getLine());
        }
    |   BOOLEAN identifier
        {
            $synVarDec = new VarDeclaration($identifier.synExpr, new BooleanType());
            setNodeLine($synVarDec, $ctx.start.getLine());
        }
    |   INT identifier LBRACKET size = INTVAL RBRACKET
        {
            $synVarDec = new VarDeclaration($identifier.synExpr, new ArrayType($size.int));
            setNodeLine($synVarDec, $ctx.start.getLine());
        }
    ;

statement returns [Statement synStmt]
    :	blockStmt { $synStmt = $blockStmt.synStmt; }
    | 	printStmt { $synStmt = $printStmt.synStmt; }
    |  	assignStmt { $synStmt = $assignStmt.synStmt; }
    |  	forStmt { $synStmt = $forStmt.synStmt; }
    |  	ifStmt { $synStmt = $ifStmt.synStmt; }
    |  	continueStmt { $synStmt = $continueStmt.synStmt; }
    |  	breakStmt { $synStmt = $breakStmt.synStmt; }
    |  	msgHandlerCall { $synStmt = $msgHandlerCall.synStmt; }
    ;

blockStmt returns [Block synStmt]
    @init
    {
        $synStmt = new Block();
        setNodeLine($synStmt, $ctx.start.getLine());
    }
    : 	LBRACE
        (statement { $synStmt.addStatement($statement.synStmt); })*
        RBRACE
    ;

printStmt returns [Print synStmt]
    : 	PRINT LPAREN expression RPAREN SEMICOLON
        {
            $synStmt = new Print($expression.synExpr);
            setNodeLine($synStmt, $ctx.start.getLine());
        }
    ;

assignStmt returns [Assign synStmt]
    :    assignment SEMICOLON { $synStmt = $assignment.synStmt; }
    ;

assignment returns [Assign synStmt]
    :   orExpression ASSIGN expression
        {
            $synStmt = new Assign($orExpression.synExpr, $expression.synExpr);
            setNodeLine($synStmt, $ctx.start.getLine());
        }
    ;

forStmt returns [For synStmt]
    @init
    {
        $synStmt = new For();
        setNodeLine($synStmt, $ctx.start.getLine());
    }
    : 	FOR
        LPAREN
        (assign1 = assignment { $synStmt.setInitialize($assign1.synStmt); })? SEMICOLON
        (expression { $synStmt.setCondition($expression.synExpr); })? SEMICOLON
        (assign2 = assignment { $synStmt.setUpdate($assign2.synStmt); })?
        RPAREN
        statement { $synStmt.setBody($statement.synStmt); }
    ;

ifStmt returns [Conditional synStmt]
    :   IF LPAREN expression RPAREN statement elseStmt
        {
            $synStmt = new Conditional($expression.synExpr, $statement.synStmt);
            setNodeLine($synStmt, $ctx.start.getLine());
            if($elseStmt.synStmt != null)
                $synStmt.setElseBody($elseStmt.synStmt);
        }
    ;

elseStmt returns [Statement synStmt]
    : ELSE statement { $synStmt = $statement.synStmt; }
    | { $synStmt = null; }
    ;

continueStmt returns [Continue synStmt]
    : 	CONTINUE SEMICOLON
        {
            $synStmt = new Continue();
            setNodeLine($synStmt, $ctx.start.getLine());
        }
    ;

breakStmt returns [Break synStmt]
    : 	BREAK SEMICOLON
        {
            $synStmt = new Break();
            setNodeLine($synStmt, $ctx.start.getLine());
        }
    ;

msgHandlerCall returns [MsgHandlerCall synStmt] locals [Expression instance]
    :   (
            identifier { $instance = $identifier.synExpr; }
        |   SELF
            {
                $instance = new Self();
                setNodeLine($instance, $ctx.start.getLine());
            }
        |   SENDER
            {
                $instance = new Sender();
                setNodeLine($instance, $ctx.start.getLine());
            }
        )
        DOT
        name = identifier
        {
            $synStmt = new MsgHandlerCall($instance, $name.synExpr);
            setNodeLine($synStmt, $ctx.start.getLine());
        }
        LPAREN
        expressionList { $synStmt.setArgs($expressionList.argExprs); }
        RPAREN
        SEMICOLON
    ;

expression returns [Expression synExpr]
    :	e1 = orExpression { $synExpr = $e1.synExpr; }
        (
            ASSIGN
            e2 = expression
            {
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, BinaryOperator.assign);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )?
    ;

orExpression returns [Expression synExpr]
    :	e1 = andExpression { $synExpr = $e1.synExpr; }
        (
            OR
            e2 = andExpression
            {
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, BinaryOperator.or);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

andExpression returns [Expression synExpr]
    :	e1 = equalityExpression { $synExpr = $e1.synExpr; }
        (
            AND
            e2 = equalityExpression
            {
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, BinaryOperator.and);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

equalityExpression returns [Expression synExpr]
    :	e1 = relationalExpression { $synExpr = $e1.synExpr; }
        (
            operatorName = (EQ | NEQ)
            e2 = relationalExpression
            {
                BinaryOperator operator = ($operatorName.getType() == EQ ? BinaryOperator.eq : BinaryOperator.neq);
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, operator);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

relationalExpression returns [Expression synExpr]
    :   e1 = additiveExpression { $synExpr = $e1.synExpr; }
        (
            operatorName = (LT | GT)
            e2 = additiveExpression
            {
                BinaryOperator operator = ($operatorName.getType() == LT ? BinaryOperator.lt : BinaryOperator.gt);
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, operator);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

additiveExpression returns [Expression synExpr]
    :   e1 = multiplicativeExpression { $synExpr = $e1.synExpr; }
        (
            operatorName = (PLUS | MINUS)
            e2 = multiplicativeExpression
            {
                BinaryOperator operator = ($operatorName.getType() == PLUS ? BinaryOperator.add : BinaryOperator.sub);
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, operator);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

multiplicativeExpression returns [Expression synExpr]
    :   e1 = preUnaryExpression { $synExpr = $e1.synExpr; }
        (
            operatorName = (MULT | DIV | PERCENT)
            e2 = preUnaryExpression
            {
                BinaryOperator operator = ($operatorName.getType() == MULT ? BinaryOperator.mult : ($operatorName.getType() == DIV ? BinaryOperator.div : BinaryOperator.mod));
                $synExpr = new BinaryExpression($synExpr, $e2.synExpr, operator);
                setNodeLine($synExpr, $e2.start.getLine());
            }
        )*
    ;

preUnaryExpression returns [Expression synExpr]
    :   NOT preUnaryExpression
        {
            $synExpr = new UnaryExpression(UnaryOperator.not, $preUnaryExpression.synExpr);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   MINUS preUnaryExpression
        {
            $synExpr = new UnaryExpression(UnaryOperator.minus, $preUnaryExpression.synExpr);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   PLUSPLUS preUnaryExpression
        {
            $synExpr = new UnaryExpression(UnaryOperator.preinc, $preUnaryExpression.synExpr);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   MINUSMINUS preUnaryExpression
        {
            $synExpr = new UnaryExpression(UnaryOperator.predec, $preUnaryExpression.synExpr);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   postUnaryExpression { $synExpr = $postUnaryExpression.synExpr; }
    ;

postUnaryExpression returns [Expression synExpr]
    :   otherExpression { $synExpr = $otherExpression.synExpr; }
        (
        postUnaryOp
        {
            UnaryOperator operator = ($postUnaryOp.start.getType() == PLUSPLUS ? UnaryOperator.postinc : UnaryOperator.postdec);
            $synExpr = new UnaryExpression(operator, $synExpr);
            setNodeLine($synExpr, $postUnaryOp.start.getLine());
        }
        )?
    ;

postUnaryOp
    :	PLUSPLUS | MINUSMINUS
    ;

otherExpression returns [Expression synExpr]
    :   LPAREN expression RPAREN { $synExpr = $expression.synExpr; }
    |   identifier { $synExpr = $identifier.synExpr; }
    |   arrayCall { $synExpr = $arrayCall.synExpr; }
    |   actorVarAccess { $synExpr = $actorVarAccess.synExpr; }
    |   value { $synExpr = $value.synExpr; }
    |   SENDER
        {
            $synExpr = new Sender();
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    ;

arrayCall returns [ArrayCall synExpr] locals [Expression instance]
    :   (
        identifier { $instance = $identifier.synExpr; } | actorVarAccess { $instance = $actorVarAccess.synExpr; })
        LBRACKET
        expression
        RBRACKET
        {
            $synExpr = new ArrayCall($instance, $expression.synExpr);
            setNodeLine($synExpr, $expression.start.getLine());
        }
    ;

actorVarAccess returns [ActorVarAccess synExpr]
    :   SELF DOT identifier
        {
            $synExpr = new ActorVarAccess($identifier.synExpr);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    ;

expressionList returns [ArrayList argExprs]
    @init
    {
        $argExprs = new ArrayList<>();
    }
    :	(
        expr1 = expression { $argExprs.add($expr1.synExpr); }
        (COMMA expr2 = expression { $argExprs.add($expr2.synExpr); })*
        |
        )
    ;

identifier returns [Identifier synExpr]
    :   id = IDENTIFIER
        {
            $synExpr = new Identifier($id.text);
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    ;

value returns [Value synExpr]
    :   val = INTVAL
        {
            $synExpr = new IntValue($val.int, new IntType());
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   val = STRINGVAL
        {
            $synExpr = new StringValue($val.text, new StringType());
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   val = TRUE
        {
            $synExpr = new BooleanValue(true, new BooleanType());
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    |   val = FALSE
        {
            $synExpr = new BooleanValue(false, new BooleanType());
            setNodeLine($synExpr, $ctx.start.getLine());
        }
    ;

// values
INTVAL
    : [1-9][0-9]* | [0]
    ;

STRINGVAL
    : '"'~["]*'"'
    ;

TRUE
    :   'true'
    ;

FALSE
    :   'false'
    ;

//types
INT
    : 'int'
    ;

BOOLEAN
    : 'boolean'
    ;

STRING
    : 'string'
    ;

//keywords
ACTOR
	:	'actor'
	;

EXTENDS
	:	'extends'
	;

ACTORVARS
	:	'actorvars'
	;

KNOWNACTORS
	:	'knownactors'
	;

INITIAL
    :   'initial'
    ;

MSGHANDLER
	: 	'msghandler'
	;

SENDER
    :   'sender'
    ;

SELF
    :   'self'
    ;

MAIN
	:	'main'
	;

FOR
    :   'for'
    ;

CONTINUE
    :   'continue'
    ;

BREAK
    :   'break'
    ;

IF
    :   'if'
    ;

ELSE
    :   'else'
    ;

PRINT
    :   'print'
    ;

//symbols
LPAREN
    :   '('
    ;

RPAREN
    :   ')'
    ;

LBRACE
    :   '{'
    ;

RBRACE
    :   '}'
    ;

LBRACKET
    :   '['
    ;

RBRACKET
    :   ']'
    ;

COLON
    :   ':'
    ;

SEMICOLON
    :   ';'
    ;

COMMA
    :   ','
    ;

DOT
    :   '.'
    ;

//operators
ASSIGN
    :   '='
    ;

EQ
    :   '=='
    ;

NEQ
    :   '!='
    ;

GT
    :   '>'
    ;

LT
    :   '<'
    ;

PLUSPLUS
    :   '++'
    ;

MINUSMINUS
    :   '--'
    ;

PLUS
    :   '+'
    ;

MINUS
    :   '-'
    ;

MULT
    :   '*'
    ;

DIV
    :   '/'
    ;

PERCENT
    :   '%'
    ;

NOT
    :   '!'
    ;

AND
    :   '&&'
    ;

OR
    :   '||'
    ;

QUES
    :   '?'
    ;

IDENTIFIER
    :   [a-zA-Z_][a-zA-Z0-9_]*
    ;

COMMENT
    :   '//' ~[\n\r]* -> skip
    ;

WHITESPACE
    :   [ \t\r\n] -> skip
    ;