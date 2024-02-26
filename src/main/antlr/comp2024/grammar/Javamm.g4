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
COMMA: ',';
DOT: '.';

MUL : '*' ;
ADD : '+' ;
SUB : '-';
DIV : '/';

// NOTE OR?
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
LENGTH: 'length';


CLASS : 'class' ;
THIS: 'this';
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTEND: 'extends';

IMPORT : 'import';

STATIC: 'static';
MAIN: 'main';

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;


WS : [ \t\n\r\f]+ -> skip ;

importDecl
    : IMPORT ID ('.' ID)* ';'
    ;

program
    : importDecl * classDecl EOF
    ;


// NOTE - suportar Implements
// Public e private em vars

classDecl
    : CLASS name=ID
      (EXTEND ID)?
      LCURLY
      varDecl*
      methodDecl*
      RCURLY
    ;


varDecl
    : type name=ID SEMI
    ;

// NOTE Quando meter nome dos nos ou anotações dos nos
type
    : INT array = ('[]' | '...') # TypeIntArray
    | INT # TypeInt
    | BOOL # TypeBool
    | ID # TypeVariable
    ;

//NOTE alternado?
methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (',' param)*)? RPAREN
        LCURLY
        varDecl* stmt*
        RETURN expr SEMI
        RCURLY # Method
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN
        STRING  LSQUARE RSQUARE ID
        RPAREN
        LCURLY
        varDecl* stmt*
        RCURLY # MainMethod
    ;

param
    : type name=ID
    ;

//Note expr EQUALS expr SEMI //#AssignStmt // devia ser ID?
// Return? RETURN expr SEMI //#ReturnStmt
// IF sem Else?
stmt
    : LCURLY stmt* RCURLY # ScopeStmt
    | IF LPAREN expr RPAREN stmt
      ELSE stmt # IfStmt
    | WHILE LPAREN expr RPAREN stmt # WhileStmt
    | expr SEMI # ExprStmt
    | ID EQUALS expr SEMI # AssignStmt
    | ID LSQUARE expr RSQUARE EQUALS  expr SEMI # ListAssignStmt
    | RETURN expr SEMI # ReturnStmt
    ;

//NOTE o resto dos operadores
expr
    // Binary expressions
    : NOT expr # NegExpr
    | expr op= (MUL | DIV) expr # BinaryExpr
    | expr op= (ADD | SUB) expr # BinaryExpr
    | expr op= LT expr # BinaryExpr
    | expr op= AND expr # BinaryExpr
    // others
    | expr LSQUARE expr RSQUARE # ArrayExpr
    | expr DOT LENGTH # LengthAttrExpr// ID?
    | expr DOT ID LPAREN (expr (COMMA expr)*)? RPAREN # MethodExpr
    | NEW INT LSQUARE expr RSQUARE # NewArrayExpr
    | NEW ID LPAREN RPAREN #NewObjExpr// Dafult?
    | LPAREN expr RPAREN # ParenthExpr
    | LSQUARE (expr (COMMA expr)*)? RSQUARE # InitArrayExpr
    | value= (TRUE | FALSE) # BoolLiteral
    | THIS # This
    | value=INTEGER # IntegerLiteral
    | name=ID # VarRefExpr
    ;



