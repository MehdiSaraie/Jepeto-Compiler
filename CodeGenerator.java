package main.visitor.codeGenerator;

import main.ast.nodes.*;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.*;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.types.*;
import main.ast.types.functionPointer.*;
import main.ast.types.list.*;
import main.ast.types.single.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.FunctionSymbolTableItem;
import main.visitor.Visitor;
import main.visitor.type.ExpressionTypeChecker;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeGenerator extends Visitor<String> {
    private final String outputPath;
    private FileWriter mainFile;
    private final ExpressionTypeChecker expressionTypeChecker;
    private FunctionDeclaration curFuncDec;

    public CodeGenerator(ExpressionTypeChecker expressionTypeChecker) {
        this.expressionTypeChecker = expressionTypeChecker;
        outputPath = "./output/";
        prepareOutputFolder();
    }

    private void prepareOutputFolder() {
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String listClassPath = "utilities/codeGenerationUtilityClasses/List.j";
        String fptrClassPath = "utilities/codeGenerationUtilityClasses/Fptr.j";
        try{
            File directory = new File(this.outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
             directory.mkdir();
        }
        catch(SecurityException e) {//unreachable
        }
        copyFile(jasminPath, this.outputPath + "jasmin.jar");
        copyFile(listClassPath, this.outputPath + "List.j");
        copyFile(fptrClassPath, this.outputPath + "Fptr.j");

        try {
            String path = outputPath + "Main.j";
            File file = new File(path);
            file.createNewFile();
            mainFile = new FileWriter(path);
        } catch (IOException e) {//unreachable
        }
    }

    private void copyFile(String toBeCopied, String toBePasted) {
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0)
                writingFileStream.write(buffer, 0, readLength);
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e) {//never reached
        }
    }

    private void addCommand(String command) {
        try {
            command = String.join("\n\t\t", command.split("\n"));
            if(command.startsWith("Label_"))
                mainFile.write("\t" + command + "\n");
            else if(command.startsWith("."))
                mainFile.write(command + "\n");
            else
                mainFile.write("\t\t" + command + "\n");
            mainFile.flush();
        } catch (IOException e) {//unreachable

        }
    }

    private void addStaticMainMethod() {
        //todo
        String command = ".method public static main([Ljava/lang/String;)V\n";
        command += ".limit stack 128\n";
        command += ".limit locals 128\n";
        command += "new Main\n";
        command += "invokespecial Main/<init>()V\n";
        command += ".end method\n";
        addCommand(command);
    }

    private int slotOf(String identifier) {
        //todo
        if (identifier == "")
            return curFuncDec.getArgs().size()+1;
        int slot;
        for(int i = 0; i < curFuncDec.getArgs().size(); i++){
            if (curFuncDec.getArgs().get(i).getName().equals(identifier))
                return i+1;
        }
        return -1;
    }

    @Override
    public String visit(Program program) {
        //todo
        return null;
    }

    @Override
        public String visit(FunctionDeclaration funcDeclaration) {
        //todo
        String command = "";

        FunctionSymbolTableItem func_symbol_table;
        try {
            func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + funcDeclaration.getFunctionName().getName()));
        }catch(ItemNotFoundException e){
            func_symbol_table = null;
        }

        command += ".method " + funcDeclaration.getFunctionName().getName() + "(";
        //Todo add arg types
        ArrayList<Identifier> func_args = funcDeclaration.getArgs();
        Map < String , Type > func_args_type = func_symbol_table.getArgTypes();

        for(Identifier cur_arg : func_args){
            Type cur_arg_type = func_args_type.get(cur_arg.getName());
            if(cur_arg_type instanceof IntType)
                command += "I";
            else if(cur_arg_type instanceof BoolType)
                command += "Z";
            else if(cur_arg_type instanceof StringType)
                command += "Ljava/lang/String;";
            else if(cur_arg_type instanceof FptrType)
                command += "LFptr;";
            else if(cur_arg_type instanceof ListType)
                command += "LList;";
        }
        command += ")";

        //Todo add return type \n
        Type return_type = func_symbol_table.getReturnType();
        if(return_type instanceof IntType)
            command += "I";
        else if(return_type instanceof BoolType)
            command += "Z";
        else if(return_type instanceof StringType)
            command += "Ljava/lang/String;";
        else if(return_type instanceof FptrType)
            command += "LFptr;";
        else if(return_type instanceof ListType)
            command += "LList;";
        else if(return_type instanceof VoidType)
            command += "V";
        command += "\n";
        //Body
        command += ".limit stack 128\n";
        command += ".limit locals 128\n";

        //TODO Set curFuncDec
        command += funcDeclaration.getBody().accept(this);
        command += ".end method";
        return null;
    }

    @Override
    public String visit(MainDeclaration mainDeclaration) {
        //todo
        String command = ".method public <init>()V\n";
        command += "  .limit stack 128\n";
        command += "  .limit locals 128\n";
        command += "  aload_0\n";
        command += "  invokespecial java/lang/Object/<init>()V\n";
        command += mainDeclaration.getBody().accept(this);
        command += "  return\n";
        command += ".end method\n";

        addCommand(command);
        return null;
    }


    @Override
    public String visit(BlockStmt blockStmt) {
        //todo
        String command = "";
        for (Statement stmt : blockStmt.getStatements())
            command += stmt.accept(this);
        addCommand(command);
        return null;
    }

    @Override
    public String visit(ConditionalStmt conditionalStmt) {
        //todo
        String command = "";
        command += conditionalStmt.getCondition().accept(this);
        command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
        command += "ifeq Label" + "else" + "\n";

        //TODO age then nadasht chi?
        command += conditionalStmt.getThenBody().accept(this);
        command += "goto Label" + "endif" + "\n";

        command += "Label" + "else" + ":\n";
        command += conditionalStmt.getElseBody().accept(this);
        command += "Label" + "endif" + ":\n";

        addCommand(command);
        return null;
    }

    @Override
    public String visit(FunctionCallStmt funcCallStmt) {
        //todo
        expressionTypeChecker.setFunctioncallStmt(true);
        String command = funcCallStmt.getFunctionCall().accept(this);
        command += "pop\n";
        expressionTypeChecker.setFunctioncallStmt(false);
        addCommand(command);
        return null;
    }

    @Override
    public String visit(PrintStmt print) {
        //todo

        //Todo add printstream
        String command = "getstatic java/lang/System/out Ljava/io/PrintStream\n";

        //Todo add value
        command += print.getArg().accept(this);
        Type arg_type = print.getArg().accept(expressionTypeChecker);

        if(arg_type instanceof IntType)
            command += "invokevirtual java/lang/Integer/intValue()I\n";

        //Todo invoke matching print function

        if(arg_type instanceof IntType)
            command += "invokevirtual java/io/PrintStream/println(I)V\n";
        else if(arg_type instanceof BoolType)
            command += "invokevirtual java/io/PrintStream/println(Ljava/lang/Object;)V\n";
        else if(arg_type instanceof StringType)
            command += "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
        //else if(arg_type instanceof ListType) TODO emtiazi

        addCommand(command);
        return null;
    }

    @Override
    public String visit(ReturnStmt returnStmt) {
        //todo
        String command = returnStmt.getReturnedExpr().accept(this);
        Type expr_type = returnStmt.getReturnedExpr().accept(expressionTypeChecker);

        if(expr_type instanceof VoidType)
            command += "return\n";
        else
            command += "areturn\n";

        addCommand(command);
        return null;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        //todo
        String command = "";
        Expression left = binaryExpression.getFirstOperand();
        Expression right = binaryExpression.getSecondOperand();
        BinaryOperator operator =  binaryExpression.getBinaryOperator();
        if (operator.equals(BinaryOperator.add)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "iadd\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.sub)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "isub\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.mult)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "imul\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.div)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "idiv\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.eq)) { //fptr is void should be checked
            command += left.accept(this);
            command += right.accept(this);
            command += "if_acmpne Label" + "false" + "\n";

            command += "ldc 1\n";
            command += "goto Label" + "after" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 0\n";

            command += "Label" + "after" + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.neq)) { //fptr not void should be checked
            command += left.accept(this);
            command += right.accept(this);
            command += "if_acmpeq Label" + "false" + "\n";

            command += "ldc 1\n";
            command += "goto Label" + "after" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 0\n";

            command += "Label" + "after" + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.and)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label" + "false" + "\n";

            command += right.accept(this);
            command += "goto Label" + "endif" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 0\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
            command += "Label" + "endif" + ":\n";
        }
        else if (operator.equals(BinaryOperator.or)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifne Label" + "true" + "\n";

            command += right.accept(this);
            command += "goto Label" + "endif" + "\n";

            command += "Label" + "true" + ":\n";
            command += "ldc 1\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
            command += "Label" + "endif" + ":\n";
        }
        else if(operator.equals(BinaryOperator.lt)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "if_cmpge Label" + "false" + "\n";

            command += "ldc 1\n";
            command += "goto Label" + "endif" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 0\n";
            command += "Label" + "endif" + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if(operator.equals(BinaryOperator.gt)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "if_cmple Label" + "false" + "\n";

            command += "ldc 1\n";
            command += "goto Label" + "endif" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 0\n";
            command += "Label" + "endif" + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.append)){
            command = left.accept(this);
            command += "dup\n";
            command += right.accept(this);
            command += "invokevirtual List/addElement(Ljava/lang/Object;)V\n";
        }
        return command;
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        //todo
        Expression operand = unaryExpression.getOperand();
        UnaryOperator operator = unaryExpression.getOperator();
        String command = "";
        if (operator.equals(UnaryOperator.not)){
            command += operand.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label" + "false" + "\n";

            command += "ldc 0\n";
            command += "goto Label" + "after" + "\n";

            command += "Label" + "false" + ":\n";
            command += "ldc 1\n";
            command += "Label" + "after" + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(UnaryOperator.minus)){
            command += operand.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "ineg\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        return command;
    }

    @Override
    public String visit(AnonymousFunction anonymousFunction) {
        //todo
        return null;
    }

    @Override
    public String visit(Identifier identifier) {
        //todo
        String command = "";
        try{
            FunctionSymbolTableItem func = (FunctionSymbolTableItem) SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + identifier.getName());
            command += "new Fptr\n";
            command += "dup\n";
            command += "aload_0\n";
            command += "ldc " + "\"" + identifier.getName() + "\"\n";
            command += "invokespecial Fptr/<init>(Ljava/lang/Object;Ljava/lang/String;)V\n";
        }catch (ItemNotFoundException e){
            int slot_number = slotOf(identifier.getName());
            command += "aload " + String.valueOf(slot_number) + "\n";
        }
        return command;
    }

    @Override
    public String visit(ListAccessByIndex listAccessByIndex) {
        //todo
        String command = "";
        command += listAccessByIndex.getInstance().accept(this);
        command += listAccessByIndex.getIndex().accept(this);
        command += "invokevirtual java/lang/Integer/intValue()I\n";
        command += "invokevirtual List/getElement(I)Ljava/lang/Object;\n";
        command += "checkcast java/lang/Integer\n";
        return command;
    }

    @Override
    public String visit(ListSize listSize) {
        //todo
        String command = "";
        command += listSize.getInstance().accept(this);
        command += "invokevirtual List/getSize()I\n";
        command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        return command;
    }

    @Override
    public String visit(FunctionCall funcCall) {
        //todo
        String command = funcCall.getInstance().accept(this);

        ArrayList<Expression> args = funcCall.getArgs();
        Map<Identifier , Expression> args_with_keys = funcCall.getArgsWithKey();

        command += "new java/util/ArrayList\n";
        command += "dup\n";
        command += "invokespecial java/util/ArrayList/<init>()V\n";

        int arrayList_slot = slotOf("");
        if (arrayList_slot > 3)
            command += "astore " + arrayList_slot + "\n";
        else
            command += "astore_" + arrayList_slot + "\n";

        if(!args.isEmpty()){
            for(Expression cur_arg : args){
                if (arrayList_slot > 3)
                    command += "aload " + arrayList_slot + "\n";
                else
                    command += "aload_" + arrayList_slot + "\n";
                command += cur_arg.accept(this);
                command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
                command += "pop\n";
            }
        }else if(!args_with_keys.isEmpty()){
            FptrType fptr = (FptrType) funcCall.getInstance().accept(expressionTypeChecker);
            FunctionSymbolTableItem func_symbol_table;
            try {
                func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + fptr.getFunctionName()));
            }catch(ItemNotFoundException e){
                func_symbol_table = null;
            }

            Map<String, Type> arg_types = func_symbol_table.getArgTypes();
            java.util.Set<String> arg_names = arg_types.keySet();
            for(String arg_name : arg_names){
                if (arrayList_slot > 3)
                    command += "aload " + arrayList_slot + "\n";
                else
                    command += "aload_" + arrayList_slot + "\n";
                Expression cur_arg = args_with_keys.get(arg_name);
                command += cur_arg.accept(this);
                command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
                command += "pop\n";
            }
        }

        if (arrayList_slot > 3)
            command += "aload " + arrayList_slot + "\n";
        else
            command += "aload_" + arrayList_slot + "\n";

        command += "invokevirtual Fptr/invoke(Ljava/util/ArrayList;)Ljava/lang/Object;\n";
        return command;
    }

    @Override
    public String visit(ListValue listValue) {
        //todo
        String command = "";
        command += "new java/util/ArrayList\n";
        command += "dup\n";
        command += "invokespecial java/util/ArrayList/<init>()V\n";
        int arrayList_slot = slotOf("");
        if (arrayList_slot > 3)
            command += "astore " + arrayList_slot + "\n";
        else
            command += "astore_" + arrayList_slot + "\n";

        for (Expression element: listValue.getElements()){
            if (arrayList_slot > 3)
                command += "aload " + arrayList_slot + "\n";
            else
                command += "aload_" + arrayList_slot + "\n";
            command += element.accept(this);
            command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
            command += "pop\n";
        }
        command += "new List\n";
        command += "dup\n";
        if (arrayList_slot > 3)
            command += "aload " + arrayList_slot + "\n";
        else
            command += "aload_" + arrayList_slot + "\n";
        command += "invokespecial List/<init>(Ljava/util/ArrayList;)V\n";
        return command;
    }

    @Override
    public String visit(IntValue intValue) {
        String command = "ldc " + intValue.getConstant() + "\n";
        command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        return command;
    }

    @Override
    public String visit(BoolValue boolValue) {
        String command = "ldc " + (boolValue.getConstant() ? 1 : 0) + "\n";
        command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        return command;
    }

    @Override
    public String visit(StringValue stringValue) {
        //todo
        //TODO string ha bayad ba " vared shavand
        String command = "ldc " + stringValue.getConstant() + "\n";
        return command;
    }

    @Override
    public String visit(VoidValue voidValue) {
        //todo
        return null;
    }
}
