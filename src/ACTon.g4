grammar ACTon;

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