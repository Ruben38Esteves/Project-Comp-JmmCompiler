package pt.up.fe.comp2024.symboltable;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import javax.lang.model.type.NullType;
import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;
import static pt.up.fe.comp2024.ast.Kind.IMPORT_STMT;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getChildren(Kind.CLASS_DECL).get(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var superClass = buildSuperClass(classDecl);
        var imports = buildImports(root);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, superClass, imports);
    }

    private static String buildSuperClass(JmmNode classDecl) {
        if(classDecl.hasAttribute("extendedClass")){
            return classDecl.get("extendedClass");
        }
        return null;
    }

    private static List<String> buildImports(JmmNode root){
        return root.getChildren(Kind.IMPORT_STMT).stream().map(import_statement -> import_statement.get("value")).toList();

    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;


    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getMethodParams(method)));

        return map;
    }
    public static List<Symbol> getMethodParams(JmmNode methodDecl){
        if(methodDecl.getChild(1).isInstance(Kind.PARAM)){
            return methodDecl.getChildren(Kind.PARAM).stream()
                    .map(param -> new Symbol(new Type(param.getChild(0).get("name"), checkIfArray(param.getChild(0).get("name"))), param.get("name")))
                    .toList();
        }
        System.out.println("NAO ENTROU");
        return new ArrayList<>();
    }
    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

    private static boolean checkIfArray(String varType){
        return varType.contains("[]");
    }

    private static List<Symbol> buildFields(JmmNode classDecl)  {

        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(new Type(varDecl.getChild(0).get("name"), checkIfArray(varDecl.getChild(0).get("name"))), varDecl.get("name"))).toList();
    }

}
