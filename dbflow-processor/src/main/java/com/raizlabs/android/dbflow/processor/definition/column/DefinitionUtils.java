package com.raizlabs.android.dbflow.processor.definition.column;

import com.raizlabs.android.dbflow.processor.SQLiteType;
import com.raizlabs.android.dbflow.processor.definition.method.BindToContentValuesMethod;
import com.raizlabs.android.dbflow.processor.definition.method.BindToStatementMethod;
import com.raizlabs.android.dbflow.processor.definition.method.LoadFromCursorMethod;
import com.raizlabs.android.dbflow.processor.utils.ModelUtils;
import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:
 */
public class DefinitionUtils {

    public static CodeBlock.Builder getContentValuesStatement(String elementName, String fullElementName, String columnName, TypeName elementTypeName, boolean isModelContainerAdapter, BaseColumnAccess columnAccess) {
        String statement = columnAccess.getColumnAccessString(elementTypeName, elementName, fullElementName, ModelUtils.getVariable(isModelContainerAdapter), isModelContainerAdapter);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        String finalAccessStatement = statement;
        if (columnAccess instanceof TypeConverterAccess || isModelContainerAdapter) {
            finalAccessStatement = "ref" + fullElementName;

            TypeName typeName;
            if (columnAccess instanceof TypeConverterAccess) {
                typeName = ((TypeConverterAccess) columnAccess).typeConverterDefinition.getDbTypeName();
            } else {
                typeName = elementTypeName;
            }

            codeBuilder.addStatement("$T $L = $L", typeName,
                    finalAccessStatement, statement);
        }

        if (!elementTypeName.isPrimitive()) {
            codeBuilder.beginControlFlow("if ($L != null)", finalAccessStatement);
        }

        codeBuilder.addStatement("$L.put($S, $L)",
                BindToContentValuesMethod.PARAM_CONTENT_VALUES,
                QueryBuilder.quote(columnName), finalAccessStatement);

        if (!elementTypeName.isPrimitive()) {
            codeBuilder.nextControlFlow("else")
                    .addStatement("$L.putNull($S)", BindToContentValuesMethod.PARAM_CONTENT_VALUES, QueryBuilder.quote(columnName))
                    .endControlFlow();
        }
        return codeBuilder;
    }

    public static CodeBlock.Builder getSQLiteStatementMethod(AtomicInteger index, String elementName, String fullElementName, TypeName elementTypeName, boolean isModelContainerAdapter, BaseColumnAccess columnAccess) {
        String statement = columnAccess.getColumnAccessString(elementTypeName, elementName, fullElementName, ModelUtils.getVariable(isModelContainerAdapter), isModelContainerAdapter);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        String finalAccessStatement = statement;
        if (columnAccess instanceof TypeConverterAccess || isModelContainerAdapter) {
            finalAccessStatement = "ref" + fullElementName;

            TypeName typeName;
            if (columnAccess instanceof TypeConverterAccess) {
                typeName = ((TypeConverterAccess) columnAccess).typeConverterDefinition.getDbTypeName();
            } else {
                typeName = elementTypeName;
            }

            codeBuilder.addStatement("$T $L = $L", typeName,
                    finalAccessStatement, statement);
        }

        if (!elementTypeName.isPrimitive()) {
            codeBuilder.beginControlFlow("if ($L != null)", finalAccessStatement);
        }
        codeBuilder.addStatement("$L.bind$L($L, $L)",
                BindToStatementMethod.PARAM_STATEMENT,
                columnAccess.getSqliteTypeForTypeName(elementTypeName, isModelContainerAdapter).getSQLiteStatementMethod(),
                index.intValue(), finalAccessStatement);
        if (!elementTypeName.isPrimitive()) {
            codeBuilder.nextControlFlow("else")
                    .addStatement("$L.bindNull($L)", BindToStatementMethod.PARAM_STATEMENT, index.intValue())
                    .endControlFlow();
        }

        return codeBuilder;
    }

    public static CodeBlock.Builder getLoadFromCursorMethod(String elementName, String fullElementName, TypeName elementTypeName, String columnName, boolean isModelContainerAdapter, BaseColumnAccess columnAccess) {
        String method = "";
        if (SQLiteType.containsMethod(elementTypeName)) {
            method = SQLiteType.getMethod(elementTypeName);
        } else if (columnAccess instanceof TypeConverterAccess) {
            method = SQLiteType.getMethod(((TypeConverterAccess) columnAccess).typeConverterDefinition.getDbTypeName());
        }

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        String indexName = "index" + columnName;
        codeBuilder.addStatement("int $L = $L.getColumnIndex($S)", indexName, LoadFromCursorMethod.PARAM_CURSOR, columnName);
        codeBuilder.beginControlFlow("if ($L != -1 && !$L.isNull($L))", indexName, LoadFromCursorMethod.PARAM_CURSOR, indexName);

        codeBuilder.addStatement(columnAccess.setColumnAccessString(elementTypeName, elementName, fullElementName,
                isModelContainerAdapter, ModelUtils.getVariable(isModelContainerAdapter), CodeBlock.builder().add("$L.$L($L)", LoadFromCursorMethod.PARAM_CURSOR, method, indexName).build().toString()));

        codeBuilder.endControlFlow();

        return codeBuilder;
    }

    public static CodeBlock.Builder getCreationStatement(TypeName elementTypeName, BaseColumnAccess columnAccess, String columnName) {
        String statement = null;

        if (SQLiteType.containsType(elementTypeName)) {
            statement = SQLiteType.get(elementTypeName).toString();
        } else if (columnAccess instanceof TypeConverterAccess) {
            statement = SQLiteType.get(((TypeConverterAccess) columnAccess).typeConverterDefinition.getDbTypeName()).toString();
        }


        return CodeBlock.builder()
                .add("$L $L", QueryBuilder.quote(columnName), statement);

    }
}
