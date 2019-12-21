grammar acton;

@header{
    import main.ast.node.*;
    import main.ast.node.declaration.*;
    import main.ast.node.declaration.handler.*;
    import main.ast.node.statement.*;
    import main.ast.node.expression.*;
    import main.ast.node.expression.operators.*;
    import main.ast.node.expression.values.*;
    import main.ast.type.primitiveType.*;
    import main.ast.type.arrayType.*;
    import main.ast.type.actorType.*;
    import main.ast.type.*;
}

program returns [Program p]
    :   {$p = new Program();}
        (actordec = actorDeclaration {$p.addActor($actordec.actordec);})+
        programMain = mainDeclaration {$p.setMain($programMain.main);}
    ;

actorDeclaration returns [ActorDeclaration actordec]
    :   a = ACTOR name = identifier {$actordec = new ActorDeclaration($name.id); $actordec.setLine($a.getLine());}
        (EXTENDS pname = identifier {$actordec.setParentName($pname.id);})?
        LPAREN queue = INTVAL RPAREN {$actordec.setQueueSize($queue.int);}

        LBRACE

        (KNOWNACTORS
        LBRACE
            (actor = identifier name = identifier SEMICOLON
            {
                VarDeclaration knownActor = new VarDeclaration($name.id, new ActorType($actor.id));
                knownActor.setLine($actor.id.getLine());
                $actordec.addKnownActor(knownActor);
            }
            )*
        RBRACE)

        (ACTORVARS
        LBRACE
            vardecs = varDeclarations {$actordec.setActorVars($vardecs.vardecs);}
        RBRACE)

        (inithandler = initHandlerDeclaration {$actordec.setInitHandler($inithandler.inithandler);})?
        (msghandler = msgHandlerDeclaration {$actordec.addMsgHandler($msghandler.msghandler);})*

        RBRACE
    ;

mainDeclaration returns [Main main]
    :   {$main = new Main();} m = MAIN {$main.setLine($m.getLine());} LBRACE
        (
        actorInst = actorInstantiation
        {$main.addActorInstantiation($actorInst.actorInst);}
        )*
        RBRACE
    ;

actorInstantiation returns [ActorInstantiation actorInst]
    :
     actor = identifier name = identifier {$actorInst = new ActorInstantiation(new ActorType($actor.id), $name.id); $actorInst.setLine($actor.id.getLine());}
     LPAREN (id = identifier {$actorInst.addKnownActor($id.id);} (COMMA id = identifier {$actorInst.addKnownActor($id.id);})* | ) RPAREN
     COLON LPAREN el = expressionList RPAREN SEMICOLON { $actorInst.setInitArgs($el.expressions);}
     ;

initHandlerDeclaration returns [InitHandlerDeclaration inithandler]
    :   msg = MSGHANDLER INITIAL {$inithandler = new InitHandlerDeclaration(new Identifier("initial")); $inithandler.setLine($msg.getLine());}
        LPAREN argdecs = argDeclarations RPAREN {$inithandler.setArgs($argdecs.argdecs);}
        LBRACE
        vardecs = varDeclarations {$inithandler.setLocalVars($vardecs.vardecs);}
        (s = statement {$inithandler.addStatement($s.s);})*
        RBRACE
    ;

msgHandlerDeclaration returns [MsgHandlerDeclaration msghandler]
    :   msg = MSGHANDLER id = identifier {$msghandler = new MsgHandlerDeclaration($id.id); $msghandler.setLine($msg.getLine());}
        LPAREN argdecs = argDeclarations RPAREN {$msghandler.setArgs($argdecs.argdecs);}
        LBRACE
        vardecs = varDeclarations {$msghandler.setLocalVars($vardecs.vardecs);}
        (s = statement {$msghandler.addStatement($s.s);})*
        RBRACE
    ;

argDeclarations returns [ArrayList<VarDeclaration> argdecs]
    :   {$argdecs = new ArrayList<>();}
        (vardec = varDeclaration {$argdecs.add($vardec.vardec);}
        (COMMA vardec = varDeclaration {$argdecs.add($vardec.vardec);})* | )
    ;

varDeclarations returns [ArrayList<VarDeclaration> vardecs]
    :   {$vardecs = new ArrayList<>();}
        (vardec = varDeclaration SEMICOLON {$vardecs.add($vardec.vardec);})*
    ;

varDeclaration returns [VarDeclaration vardec]
    :	{Type t = null;}
    (   intType = INT id = identifier {t = new IntType(); t.setLine($intType.getLine());}
    |   stringType = STRING id = identifier {t = new StringType(); t.setLine($stringType.getLine());}
    |   booleanType = BOOLEAN id = identifier {t = new BooleanType(); t.setLine($booleanType.getLine());}
    |   arrayType = INT id = identifier LBRACKET size = INTVAL RBRACKET {t = new ArrayType($size.int); t.setLine($arrayType.getLine());}
    )   {$vardec = new VarDeclaration($id.id, t); $vardec.setLine(t.getLine());}
    ;

statement returns [Statement s]
    :   block = blockStmt {$s = $block.block;}
    |   print = printStmt {$s = $print.print;}
    |   assignstmt = assignStmt {$s = $assignstmt.assign;}
    |   forstmt = forStmt {$s = $forstmt.forstmt;}
    |   ifstmt = ifStmt {$s = $ifstmt.ifstmt;}
    |   c = continueStmt {$s = $c.c;}
    |   b = breakStmt {$s = $b.b;}
    |   handlerCall = msgHandlerCall {$s = $handlerCall.handlerCall;}
    ;

blockStmt returns [Block block]
    :   {$block = new Block();} lbrace = LBRACE {$block.setLine($lbrace.getLine());} (s = statement {$block.addStatement($s.s);})* RBRACE
    ;

printStmt returns [Print print]
    :   p = PRINT LPAREN e = expression RPAREN SEMICOLON {$print = new Print($e.e); $print.setLine($p.getLine());}
    ;

assignStmt returns [Assign assign]
    :    asgn = assignment SEMICOLON {$assign = $asgn.assign;}
    ;

assignment returns [Assign assign]
    :   lv = orExpression a = ASSIGN rv = expression {$assign = new Assign($lv.oe, $rv.e); $assign.setLine($a.getLine());}
    ;

forStmt returns [For forstmt]
    :   f = FOR LPAREN {$forstmt = new For(); $forstmt.setLine($f.getLine());}
        (init = assignment {$forstmt.setInitialize($init.assign);})? SEMICOLON
        (cond = expression {$forstmt.setCondition($cond.e);})? SEMICOLON
        (update = assignment {$forstmt.setUpdate($update.assign);})? RPAREN s = statement {$forstmt.setBody($s.s);}
    ;

ifStmt returns [Conditional ifstmt]
    :   i = IF LPAREN e = expression RPAREN s = statement {$ifstmt = new Conditional($e.e, $s.s); $ifstmt.setLine($i.getLine());}
        (ELSE s = statement {$ifstmt.setElseBody($s.s);})?
    ;

continueStmt returns [Continue c]
    :   con = CONTINUE SEMICOLON {$c = new Continue(); $c.setLine($con.getLine());}
    ;

breakStmt returns [Break b]
    :   br = BREAK SEMICOLON {$b = new Break(); $b.setLine($br.getLine());}
    ;

msgHandlerCall returns [MsgHandlerCall handlerCall]
    :   {Expression instance;}
        (id = identifier {instance = $id.id;} |
        SELF {instance = new Self();} |
        SENDER {instance = new Sender();}) dot = DOT
        {instance.setLine($dot.getLine());}
        name = identifier {$handlerCall = new MsgHandlerCall(instance, $name.id); $handlerCall.setLine($dot.getLine());}
        LPAREN el = expressionList RPAREN {$handlerCall.setArgs($el.expressions);} SEMICOLON
    ;

expression returns [Expression e]
    :   oe = orExpression {$e = $oe.oe;}
        (assign = ASSIGN exp = expression {$e = new BinaryExpression($oe.oe, $exp.e, BinaryOperator.assign); $e.setLine($assign.getLine());})?
    ;

orExpression returns [Expression oe]
    :   ae = andExpression {$oe = $ae.ae;}
        (or = OR ae = andExpression {$oe = new BinaryExpression($oe, $ae.ae, BinaryOperator.or); $oe.setLine($or.getLine());})*
    ;

andExpression returns [Expression ae]
    :   ee = equalityExpression {$ae = $ee.ee;}
        (and = AND ee = equalityExpression {$ae = new BinaryExpression($ae, $ee.ee, BinaryOperator.and); $ae.setLine($and.getLine());})*
    ;

equalityExpression returns [Expression ee]
    :   re = relationalExpression {$ee = $re.re;}
        ({BinaryOperator op; int lineNum;} (eq = EQ {op = BinaryOperator.eq; lineNum = $eq.getLine();} |
                               neq = NEQ {op = BinaryOperator.neq; lineNum = $neq.getLine();})
        re = relationalExpression {$ee = new BinaryExpression($ee, $re.re, op); $ee.setLine(lineNum);})*
    ;

relationalExpression returns [Expression re]
    :   ae = additiveExpression {$re = $ae.ae;}
        ({BinaryOperator op; int lineNum;} (lt = LT {op = BinaryOperator.lt; lineNum = $lt.getLine();} |
                                            gt = GT {op = BinaryOperator.gt; lineNum = $gt.getLine();})
        ae = additiveExpression {$re = new BinaryExpression($re, $ae.ae, op); $re.setLine(lineNum);})*
    ;

additiveExpression returns [Expression ae]
    :   me = multiplicativeExpression {$ae = $me.me;}
        ({BinaryOperator op; int lineNum;} (plus = PLUS {op = BinaryOperator.add; lineNum = $plus.getLine();} |
                                            minus = MINUS {op = BinaryOperator.sub; lineNum = $minus.getLine();})
        me = multiplicativeExpression {$ae = new BinaryExpression($ae, $me.me, op); $ae.setLine(lineNum);})*
    ;

multiplicativeExpression returns [Expression me]
    :   pre = preUnaryExpression {$me = $pre.pue;}
        ({BinaryOperator op; int lineNum;} ( mult = MULT {op = BinaryOperator.mult; lineNum = $mult.getLine();} |
                                div = DIV {op = BinaryOperator.div; lineNum = $div.getLine();} |
                                percent = PERCENT {op = BinaryOperator.mod; lineNum = $percent.getLine();})
        pre = preUnaryExpression {$me = new BinaryExpression($me, $pre.pue, op); $me.setLine(lineNum);})*
    ;

preUnaryExpression returns [Expression pue]
    :   not = NOT pe = preUnaryExpression {$pue = new UnaryExpression(UnaryOperator.not, $pe.pue); $pue.setLine($not.getLine());}
    |   minus = MINUS pe = preUnaryExpression {$pue = new UnaryExpression(UnaryOperator.minus, $pe.pue); $pue.setLine($minus.getLine());}
    |   plusplus = PLUSPLUS pe = preUnaryExpression {$pue = new UnaryExpression(UnaryOperator.preinc, $pe.pue); $pue.setLine($plusplus.getLine());}
    |   minusminus = MINUSMINUS pe = preUnaryExpression {$pue = new UnaryExpression(UnaryOperator.predec, $pe.pue); $pue.setLine($minusminus.getLine());}
    |   post = postUnaryExpression {$pue = $post.pue;}
    ;

postUnaryExpression returns [Expression pue]
    :   oe = otherExpression {$pue = $oe.oe;} (op = postUnaryOp {$pue = new UnaryExpression($op.op, $pue); $pue.setLine($op.lineNum);} )?
    ;

postUnaryOp returns [UnaryOperator op, int lineNum]
    :   plusplus = PLUSPLUS {$op = UnaryOperator.postinc; $lineNum = $plusplus.getLine();} |
        minusminus = MINUSMINUS {$op = UnaryOperator.postdec; $lineNum = $minusminus.getLine();}
    ;

otherExpression returns [Expression oe]
    :   LPAREN e = expression {$oe = $e.e;} RPAREN
    |   id = identifier {$oe = $id.id;}
    |   arrCall = arrayCall {$oe = $arrCall.arrCall;}
    |   av = actorVarAccess {$oe = $av.av;}
    |   v = value {$oe = $v.v;}
    |   sender = SENDER {$oe = new Sender(); $oe.setLine($sender.getLine());}
    ;

arrayCall returns [ArrayCall arrCall]
    :   {Expression instance;}
        (id = identifier {instance = $id.id;} | av = actorVarAccess {instance = $av.av;})
        lbracket = LBRACKET e = expression RBRACKET {$arrCall = new ArrayCall(instance, $e.e); $arrCall.setLine($lbracket.getLine());}
    ;

actorVarAccess returns [ActorVarAccess av]
    :   self = SELF {Self self = new Self(); self.setLine($self.getLine());}
        DOT id = identifier {$av = new ActorVarAccess($id.id); $av.setLine($self.getLine());}
    ;

expressionList returns [ArrayList <Expression> expressions]
    :   {$expressions = new ArrayList<>();}
        ( e = expression {$expressions.add($e.e);} (COMMA e = expression {$expressions.add($e.e);})* |
        )
    ;

identifier returns [Identifier id]
    :   iden = IDENTIFIER {$id = new Identifier($iden.text); $id.setLine($iden.getLine());}
    ;

value returns [Value v]
    :   intVal = INTVAL {$v = new IntValue($intVal.int, new IntType()); $v.setLine($intVal.getLine());} |
        strVal = STRINGVAL {$v = new StringValue($strVal.text, new StringType()); $v.setLine($strVal.getLine());} |
        trueVal = TRUE {$v = new BooleanValue(true, new BooleanType()); $v.setLine($trueVal.getLine());} |
        falseVal = FALSE {$v = new BooleanValue(false, new BooleanType()); $v.setLine($falseVal.getLine());}
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