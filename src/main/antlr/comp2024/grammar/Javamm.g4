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
LBRACK : '[' ;
RBRACK : ']' ;
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
SUB : '-' ;
NOT : '!' ;
TRUE : 'true' ;
FALSE : 'false' ;
AND : '&&' ;
OR : '||' ;
LT : '<' ;
GT : '>' ;
ACCESS : '.' ;

CLASS : 'class' ;
NEW : 'new' ;
INTARRAY : 'int[]' ;
INTVARARG: 'int...' ;
INT : 'int' ;
FLOAT : 'float' ;
CHAR : 'char' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0] | ([1-9][0-9]*);
ID : [a-zA-Z][0-9a-zA-Z]* ;
//CHARACTER : . ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : importStmt* classDecl EOF
    ;

importStmt
    : 'import' value+=ID(ACCESS value+=ID)* SEMI
    ;

mainDecl
    : 'public static void main(String[] args)' LCURLY expr* RCURLY
    | 'static void main(String[] args)' LCURLY expr* RCURLY
    ;

classDecl
    : CLASS name=ID ('extends' extendedClass= ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INTVARARG
    | name= INTARRAY
    | name= INT
    | name= FLOAT
    | name= BOOLEAN
    | name= CHAR
    | name= STRING
    | name= ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN params+=param(',' params+=param)* RPAREN
        LCURLY varDecl* stmt* RCURLY
    | mainDecl
    ;

methodCall
    : LPAREN ((expr)(',' expr)*)? RPAREN ;

param
    : type name=ID
    | type LBRACK RBRACK name= ID
    ;



stmt
    : expr SEMI #Something //
    | expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt //
    | LCURLY (stmt)* RCURLY #Tobereplaced //
    | ifStmt (elseIfStmt)* (elseStmt)? #IfChainStatement //
    | 'while' LPAREN expr RPAREN stmt #WhileStatement //
    ;


ifStmt
    : 'if' LPAREN expr RPAREN stmt #IfStatement //
    ;

elseIfStmt
    : 'else' ifStmt #ElseIfStatement //
    ;

elseStmt
    : 'else' stmt #ElseStatement //
    ;


expr
    : LPAREN expr RPAREN #Parenthesis //
    | expr LBRACK expr RBRACK #ArrayAccessExpre //
    | expr op= ACCESS expr #MethodExpr //
    | op= NOT expr #UnaryExpr //
    | NEW name= ID LPAREN RPAREN #ClassInstance //
    | NEW type expr #NewArray //
    | expr op= MUL expr #ArithmeticExpr //
    | expr op= DIV expr #ArithmeticExpr //
    | expr op= ADD expr #ArithmeticExpr //
    | expr op= SUB expr #ArithmeticExpr //
    | expr op= LT expr #BooleanExpr //
    | expr op= GT expr #BooleanExpr //
    | expr op= AND expr #BooleanExpr //
    | expr op= OR expr #BooleanExpr //
    | value=INTEGER #IntegerLiteral //
    | value=(TRUE|FALSE) #BooleanLiteral //
    | name= ID #VarRefExpr //
    | name= ID methodCall #CallMethod //
    | expr methodCall #CallMethod //
    | LBRACK ((expr)(',' expr)*)? RBRACK #ArrayInitialization
    ;





