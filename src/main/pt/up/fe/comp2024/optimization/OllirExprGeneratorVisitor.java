package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(METHOD_EXPR, this::visitMethodExpr);
        // array access expr
        // class instance
        // array init
        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type("bool", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        String ollirType = "";
        Type type = TypeUtils.getExprType(node, table);
        if(type == null){
            if(table.getImports().contains(id)){
                ollirType = id;
            }

        }else{
            ollirType = OptUtils.toOllirType(type);
        }

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitMethodExpr(JmmNode node, Void unused) {
        node = node.getChild(1);
        var code = new StringBuilder();
        String caller = node.getParent().getChild(0).get("name");
        String method_name = node.get("name");
        String caller_type;
        String invoke_type = "";
        Boolean isAssigning;

        if(caller.equals("this")){
            caller_type = table.getClassName();
        }
        if(table.getImports().contains(caller)){
            invoke_type = "invokestatic";
            code.append(invoke_type).append("(").append(caller).append(", ").append("\"").append(method_name).append("\"");
        }else{
            //fazer para virtual
        }

        //get caller type

        //handle params visit params
        if(node.getChild(0).getChildren().size() > 0){
            for(var param : node.getChild(0).getChildren()) {
                code.append(", ");
                Type param_type = TypeUtils.getExprType(param,table);
                code.append(param.get("name")).append(OptUtils.toOllirType(param_type));
            }
        }
        code.append(")");

        //place return
        if(invoke_type.equals("invokestatic")){
            code.append(".V");
        }

        // fazer atribuicao

        code.append(";");
        return new OllirExprResult(code.toString());
    }



    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
