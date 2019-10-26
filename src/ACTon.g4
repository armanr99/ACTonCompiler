grammar ACTon;

@members{
    void print(String str){
       System.out.println(str);
    }
}

program:
    actorDefinition+ main
;

actorDefinition:
    ACTOR ID (EXTENDS ID)? LPAR CONST_INT RPAR
    LBRACE
        actorBody
    RBRACE
;

actorBody:
    knownActors
    actorVars
    msghandlerDefinition*
;

knownActors:
    KNOWNACTORS
    LBRACE
        (ID ID SEMI)*
    RBRACE
;

actorVars:
    ACTORVARS
    LBRACE
        (varDeclaration  SEMI)*
    RBRACE
;

varDeclaration:
    arrayDeclaration |
    primitiveDeclaration
;

varType:
    STRING |
    INT |
    BOOLEAN
;

arrayDeclaration:
    INT ID LBRACK CONST_INT RBRACK
;

primitiveDeclaration:
    varType ID
;

msghandlerDefinition:
    MSGHANDLER
    ID LPAR (varDeclaration (COMMA varDeclaration)*)? RPAR
    LBRACE
    (varDeclaration SEMI)*
    statements
    RBRACE
;

statements:
    statement*
;

statement:
    matchedStatement |
    openStatement
;

otherStatement:
    blockStatement |
    expressionStatement SEMI |
    loopStatement |
    SEMI
;

matchedStatement:
    IF LPAR expressionStatement RPAR matchedStatement ELSE matchedStatement |
    otherStatement
;

openStatement:
    IF LPAR expressionStatement RPAR statement |
    IF LPAR expressionStatement RPAR matchedStatement ELSE openStatement
;

loopStatement:
    FOR LPAR assignmentStatement? SEMI expressionStatement? SEMI assignmentStatement? RPAR
    LBRACE
        statements
    RBRACE
;

blockStatement:
    LBRACE
    statements
    RBRACE
;

expressionStatement:
    normalStatement |
    assignmentStatement
;

normalStatement:
    expressionOr
;

//expressionTernary:
//    expressionStatement Q_MARK expressionStatement COLON expressionStatement
//;

assignmentStatement:
    ID '=' expressionOr
;

expression:
    expressionOr
;

expressionOr:
    expressionAnd (OR expressionAnd)*
;

expressionAnd:
    expressionEq (AND expressionEq)*
;

expressionEq:
    expressionCmp ((EQUAL | NOT_EQUAL)expressionCmp)*
;

expressionCmp:
    expressionAdd ((LT | GT)expressionAdd)*
;

expressionAdd:
    expressionMult ((PLUS | MINUS)expressionMult)*
;

expressionMult:
    expressionUnary ((STAR | SLASH)expressionUnary)*
;

expressionUnary:
    (NOT | MINUS | MINUSMINUS | PLUSPLUS)* expressionMem
;

expressionMem:
    expressionMethods (MINUSMINUS | PLUSPLUS)*
;

expressionMethods:
    (SELF | SENDER) expressionMethodsTemp | expressionOther
;

expressionMethodsTemp:
    DOT ID LPAR (expression (COMMA expression)*)? RPAR
;

expressionOther:
    CONST_INT |
    CONST_STRING |
    TRUE |
    FALSE |
    ID |
    ID LBRACK expression RBRACK |
    LPAR expression RPAR
;

main:
    MAIN
    LBRACE
        actorDeclarations
    RBRACE
;

actorDeclarations:
    'y = 3' SEMI
;

//Reserved keywords
MSGHANDLER:
    'msghandler'
;

INITIAL:
    'initial'
;

EXTENDS:
    'extends'
;

ACTORVARS:
    'actorvars'
;

KNOWNACTORS:
    'knownactors'
;

ACTOR:
    'actor'
;

PRINT:
    'print'
;

FOR:
    'for'
;

ELSE:
    'else'
;

IF:
    'if'
;

SENDER:
    'sender'
;

SELF:
    'self'
;

MAIN:
    'main'
;

STRING:
    'string'
;

BOOLEAN:
    'boolean'
;

INT:
    'int'
;

TRUE:
    'true'
;

FALSE:
    'false'
;

CONTINUE:
    'continue'
;

BREAK:
    'break'
;

//Seperators
LBRACE:
    '{'
;

RBRACE:
    '}'
;

SEMI:
    ';'
;

DOT:
    '.'
;

//Operators
LPAR:
    '('
;

RPAR:
    ')'
;

LBRACK:
    '['
;

RBRACK:
    ']'
;

PLUSPLUS:
    '++'
;

MINUSMINUS:
    '--'
;

NOT:
    '!'
;

MINUS:
    '-'
;

MODULO:
    '%'
;

SLASH:
    '/'
;

STAR:
    '*'
;

PLUS:
    '+'
;

LT:
    '<'
;

GT:
    '>'
;

EQUAL:
    '=='
;

NOT_EQUAL:
    '!='
;

AND:
    '&&'
;

OR:
    '||'
;

Q_MARK:
    '?'
;

COLON:
    ':'
;

ASSIGN:
    '='
;

COMMA:
    ','
;

//Literals
CONST_INT:
    [0-9]+
;

CONST_STRING:
    '"' ~('\r' | '\n' | '"')* '"'
;

ID:
    [a-zA-Z_][a-zA-Z0-9_]*
;

//Whitespaces
COMMENT:
    '//'(~[\r\n])* -> skip
;

NEW_LINE:
    '\r'? '\n' -> skip
;

WHITE_SPACE:
    [ \t] -> skip
;