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
        var fields = buildFieldsFunction(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, superClass, fields, imports);
    }

    private static String buildSuperClass(JmmNode classDecl) {
        if(classDecl.hasAttribute("extendedClass")){
            return classDecl.get("extendedClass");
        }
        return null;
    }

    private static List<String> buildImports(JmmNode root){
        List<String> imports = new ArrayList<>();
        if(root.getChildren(Kind.IMPORT_STMT).size() == 0){
            return imports;
        }
        root.getChildren(IMPORT_STMT).stream().forEach(import_statement -> imports.add(import_statement.get("ID")));
        return imports;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();
        if(classDecl.getChildren(Kind.METHOD_DECL).size() == 0){
            return map;
        }

        classDecl.getChildren(Kind.METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(method.getChild(0).get("name").replace("[]", ""), checkIfArray(method.getChild(0).get("name")))));

        return map;


    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getMethodParams(method)));

        return map;
    }
    public static List<Symbol> getMethodParams(JmmNode methodDecl){
        if(methodDecl.getChildren(Kind.PARAM).size() > 0){
            return methodDecl.getChildren(Kind.PARAM).stream()
                    .map(param -> {
                        if(param != null){
                            return new Symbol(new Type(param.getChild(0).get("name").replace("[]", ""), checkIfArray(param.getChild(0).get("name"))), param.get("name"));
                        }
                        return null;
                    })
                    .toList();
        }
        return new ArrayList<>();
    }
    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

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

        var intType = new Type(TypeUtils.getIntTypeName(), false);
        if(methodDecl.getChildren(Kind.VAR_DECL).size() == 0){
            return new ArrayList<>();
        }
        return methodDecl.getChildren(Kind.VAR_DECL).stream()
                .map(varDecl -> {
                    String nameOfField = varDecl.get("name");
                    String typeOfField = varDecl.getChild(0).get("name");
                    boolean isFieldArray = checkIfArray(typeOfField);
                    Type fieldType = new Type(typeOfField.replace("[]", ""), isFieldArray);
                    Symbol field = new Symbol(fieldType, nameOfField);
                    return field;
                })
                .toList();
    }

    private static boolean checkIfArray(String varType){
        return varType.contains("[]");
    }

    private static List<Symbol> buildFieldsFunction(JmmNode classDecl)  {
        if(classDecl.getChildren(Kind.VAR_DECL).size() == 0){
            return new ArrayList<>();
        }
        return classDecl.getChildren(Kind.VAR_DECL).stream()
                .map(vardecl -> {
                    String nameOfField = vardecl.get("name");
                    String typeOfField = vardecl.getChild(0).get("name");
                    boolean isFieldArray = checkIfArray(typeOfField);
                    Type fieldType = new Type(typeOfField.replace("[]", ""), isFieldArray);
                    Symbol field = new Symbol(fieldType, nameOfField);
                    return field;

                }).toList();
    }

}
