grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE: '[';
RSQUARE: ']';

ELLIPSIS: '...';
COMMA: ',';
DOT: '.';

MUL : '*' ;
ADD : '+' ;
SUB : '-';
DIV : '/';

AND: '&&';
LT: '<';
NOT: '!';

INT : 'int' ;
BOOL: 'boolean';
VOID: 'void';
STRING: 'String';

TRUE : 'true';
FALSE: 'false';

IF : 'if';
ELSE : 'else';
WHILE: 'while';
NEW : 'new';


CLASS : 'class' ;
THIS: 'this';
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTEND: 'extends';

IMPORT : 'import';

STATIC: 'static';

INTEGER: [1-9] [0-9]* | '0' ;
ID : [a-zA-Z_$]+ [a-zA-Z_0-9$]*  ;

SINGLE_COMMENTS: ('//' ~[\n\r]* '\r'?'\n') -> skip;
MULTI_COMMENTS: ('/*' .*? '*/') -> skip;

WS : [ \t\n\r\f]+ -> skip ;

importDecl
    : IMPORT path+=ID (DOT path+=ID)* SEMI
    ;

program
    : importDecl* classDecl EOF
    ;

classDecl
    : CLASS name=ID
      (EXTEND extendedClass=ID)?
      LCURLY
      varDecl*
      methodDecl*
      RCURLY
    ;


varDecl
    : type name=ID SEMI
    ;

type locals[ boolean isArray= false, boolean isEllipse = false]
    : name = INT (LSQUARE RSQUARE {$isArray = true;})
    | name = INT ( ELLIPSIS {$isEllipse = true;})
    | name = INT
    | name = BOOL
    | name = STRING
    | name = VOID
    | name = ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY
        varDecl* stmt*
        (RETURN expr SEMI)?
        RCURLY # Method
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID name=ID // Adding the name annotation was necessary to pass the test
        LPAREN
        STRING LSQUARE RSQUARE argName=ID
        RPAREN
        LCURLY
        varDecl* stmt*
        RCURLY # MainMethod
    ;

param
    : type name = ID
    ;

stmt
    : LCURLY stmt* RCURLY # ScopeStmt
    | IF LPAREN expr RPAREN stmt
      ELSE stmt # IfStmt
    | WHILE LPAREN expr RPAREN stmt # WhileStmt
    | expr SEMI # ExprStmt
    | name = ID EQUALS expr SEMI # AssignStmt
    | name = ID LSQUARE expr RSQUARE EQUALS  expr SEMI # ListAssignStmt
    | RETURN expr SEMI # ReturnStmt
    ;

expr
    : LPAREN expr RPAREN # ParenthExpr
    | expr LSQUARE expr RSQUARE # ArrayExpr
    // members and methods access
    | expr DOT name=ID # LengthAttrExpr
    | expr DOT name = ID LPAREN (expr (COMMA expr)*)? RPAREN # MethodExpr
    // unary
    | NOT expr # NegExpr
    // new
    | NEW INT LSQUARE expr RSQUARE # NewArrayExpr
    | NEW name = ID LPAREN RPAREN # NewObjExpr
    | LSQUARE (expr (COMMA expr)*)? RSQUARE # InitArrayExpr // qual precedencia?
    // Binary
    | expr op= (MUL | DIV) expr # BinaryExpr
    | expr op= (ADD | SUB) expr # BinaryExpr
    | expr op= LT expr # BinaryExpr
    | expr op= AND expr # BinaryExpr
    // Literals
    | value= (TRUE | FALSE) # BoolLiteral
    | THIS # This
    | value=INTEGER # IntegerLiteral
    | name=ID # VarRefExpr
    ;



