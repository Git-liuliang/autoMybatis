package com.automybatis.automybatis.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.automybatis.automybatis.entity.ClassInfo;
import com.automybatis.automybatis.entity.FieldInfo;
import com.automybatis.automybatis.entity.ParamInfo;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TableParseUtil {


    public static ClassInfo processTableIntoClassInfo(ParamInfo paramInfo)
            throws IOException {
        //process the param
        String tableSql = paramInfo.getTableSql();
        String nameCaseType = MapUtil.getString(paramInfo.getOptions(),"nameCaseType");
        Boolean isPackageType = MapUtil.getBoolean(paramInfo.getOptions(),"isPackageType");

        if (tableSql == null || tableSql.trim().length() == 0) {
            throw new CodeGenerateException("Table structure can not be empty.");
        }
        //deal with special character
        tableSql = tableSql.trim().replaceAll("'", "`").replaceAll("\"", "`").replaceAll("，", ",").toLowerCase();
        //deal with java string copy \n"
        tableSql = tableSql.trim().replaceAll("\\\\n`", "").replaceAll("\\+", "").replaceAll("``", "`").replaceAll("\\\\", "");
        // table Name
        String tableName = null;
        if (tableSql.contains("TABLE") && tableSql.contains("(")) {
            tableName = tableSql.substring(tableSql.indexOf("TABLE") + 5, tableSql.indexOf("("));
        } else if (tableSql.contains("table") && tableSql.contains("(")) {
            tableName = tableSql.substring(tableSql.indexOf("table") + 5, tableSql.indexOf("("));
        } else {
            throw new CodeGenerateException("Table structure incorrect");
        }


        if (tableName.contains("if not exists")) {
            tableName = tableName.replaceAll("if not exists", "");
        }

        if (tableName.contains("`")) {
            tableName = tableName.substring(tableName.indexOf("`") + 1, tableName.lastIndexOf("`"));
        } else {

            tableName = tableName.replaceAll(" ", "").replaceAll("\n", "").replaceAll("\t", "");
        }

        if (tableName.contains("`.`")) {
            tableName = tableName.substring(tableName.indexOf("`.`") + 3);
        } else if (tableName.contains(".")) {

            tableName = tableName.substring(tableName.indexOf(".") + 1);
        }
        String originTableName = tableName;

        if(tableName!=null && StringUtils.isNotNull(MapUtil.getString(paramInfo.getOptions(),"ignorePrefix"))){
            tableName = tableName.replaceAll(MapUtil.getString(paramInfo.getOptions(),"ignorePrefix"),"");
        }

        String className = StringUtils.upperCaseFirst(StringUtils.underlineToCamelCase(tableName));
        if (className.contains("_")) {
            className = className.replaceAll("_", "");
        }


        String classComment = null;

        if (tableSql.contains("comment=") || tableSql.contains("comment on table")) {
            String classCommentTmp = (tableSql.contains("comment=")) ?
                    tableSql.substring(tableSql.lastIndexOf("comment=") + 8).trim() : tableSql.substring(tableSql.lastIndexOf("comment on table") + 17).trim();
            if (classCommentTmp.contains("`")) {
                classCommentTmp = classCommentTmp.substring(classCommentTmp.indexOf("`") + 1);
                classCommentTmp = classCommentTmp.substring(0, classCommentTmp.indexOf("`"));
                classComment = classCommentTmp;
            } else {

                classComment = className;
            }
        } else {

            classComment = tableName;
        }

        classComment = classComment.replaceAll(";", "");

        List<FieldInfo> fieldList = new ArrayList<FieldInfo>();

        String fieldListTmp = tableSql.substring(tableSql.indexOf("(") + 1, tableSql.lastIndexOf(")"));


        String commentPattenStr1 = "comment `(.*?)\\`";
        Matcher matcher1 = Pattern.compile(commentPattenStr1).matcher(fieldListTmp);
        while (matcher1.find()) {

            String commentTmp = matcher1.group();


            if (commentTmp.contains(",")) {
                String commentTmpFinal = commentTmp.replaceAll(",", "，");
                fieldListTmp = fieldListTmp.replace(matcher1.group(), commentTmpFinal);
            }
        }

        String commentPattenStr2 = "\\`(.*?)\\`";
        Matcher matcher2 = Pattern.compile(commentPattenStr2).matcher(fieldListTmp);
        while (matcher2.find()) {
            String commentTmp2 = matcher2.group();
            if (commentTmp2.contains(",")) {
                String commentTmpFinal = commentTmp2.replaceAll(",", "，").replaceAll("\\(", "（").replaceAll("\\)", "）");
                fieldListTmp = fieldListTmp.replace(matcher2.group(), commentTmpFinal);
            }
        }

        String commentPattenStr3 = "\\((.*?)\\)";
        Matcher matcher3 = Pattern.compile(commentPattenStr3).matcher(fieldListTmp);
        while (matcher3.find()) {
            String commentTmp3 = matcher3.group();
            if (commentTmp3.contains(",")) {
                String commentTmpFinal = commentTmp3.replaceAll(",", "，");
                fieldListTmp = fieldListTmp.replace(matcher3.group(), commentTmpFinal);
            }
        }
        String[] fieldLineList = fieldListTmp.split(",");
        if (fieldLineList.length > 0) {
            int i = 0;

            for (String columnLine : fieldLineList) {
                i++;
                columnLine = columnLine.replaceAll("\n", "").replaceAll("\t", "").trim();

                boolean specialFlag = (!columnLine.contains("key ") && !columnLine.contains("constraint") && !columnLine.contains("using") && !columnLine.contains("unique ")
                        && !(columnLine.contains("primary ") && columnLine.indexOf("storage") + 3 > columnLine.indexOf("("))
                        && !columnLine.contains("fulltext ") && !columnLine.contains("index ")
                        && !columnLine.contains("pctincrease")
                        && !columnLine.contains("buffer_pool") && !columnLine.contains("tablespace")
                        && !(columnLine.contains("primary ") && i > 3));
                if (specialFlag) {

                    if (columnLine.length() < 5) {
                        continue;
                    }

                    String columnName = "";
                    columnLine = columnLine.replaceAll("`", " ").replaceAll("\"", " ").replaceAll("'", "").replaceAll("  ", " ").trim();

                    try {
                        columnName = columnLine.substring(0, columnLine.indexOf(" "));
                    } catch (StringIndexOutOfBoundsException e) {
                        System.out.println("err happened: " + columnLine);
                        throw e;
                    }

                    String fieldName = null;
                    if (ParamInfo.NAME_CASE_TYPE.CAMEL_CASE.equals(nameCaseType)) {
                        fieldName = StringUtils.lowerCaseFirst(StringUtils.underlineToCamelCase(columnName));
                        if (fieldName.contains("_")) {
                            fieldName = fieldName.replaceAll("_", "");
                        }
                    } else if (ParamInfo.NAME_CASE_TYPE.UNDER_SCORE_CASE.equals(nameCaseType)) {
                        fieldName = StringUtils.lowerCaseFirst(columnName);
                    } else if (ParamInfo.NAME_CASE_TYPE.UPPER_UNDER_SCORE_CASE.equals(nameCaseType)) {
                        fieldName = StringUtils.lowerCaseFirst(columnName.toUpperCase());
                    } else {
                        fieldName = columnName;
                    }
                    columnLine = columnLine.substring(columnLine.indexOf("`") + 1).trim();
                    String mysqlType = columnLine.split("\\s+")[1];
                    if(mysqlType.contains("(")){
                        mysqlType = mysqlType.substring(0, mysqlType.indexOf("("));
                    }

                    String swaggerClass = "string" ;
                    if(mysqlJavaTypeUtil.getMysqlSwaggerTypeMap().containsKey(mysqlType)){
                        swaggerClass = mysqlJavaTypeUtil.getMysqlSwaggerTypeMap().get(mysqlType);
                    }

                    String fieldClass = "String";

                    if(mysqlJavaTypeUtil.getMysqlJavaTypeMap().containsKey(mysqlType)){
                        fieldClass = mysqlJavaTypeUtil.getMysqlJavaTypeMap().get(mysqlType);
                    }

                    String fieldComment = null;
                    if (tableSql.contains("comment on column") && (tableSql.contains("." + columnName + " is ") || tableSql.contains(".`" + columnName + "` is"))) {

                        tableSql = tableSql.replaceAll(".`" + columnName + "` is", "." + columnName + " is");
                        Matcher columnCommentMatcher = Pattern.compile("\\." + columnName + " is `").matcher(tableSql);
                        fieldComment = columnName;
                        while (columnCommentMatcher.find()) {
                            String columnCommentTmp = columnCommentMatcher.group();

                            fieldComment = tableSql.substring(tableSql.indexOf(columnCommentTmp) + columnCommentTmp.length()).trim();
                            fieldComment = fieldComment.substring(0, fieldComment.indexOf("`")).trim();
                        }
                    } else if (columnLine.contains(" comment")) {

                        String commentTmp = columnLine.substring(columnLine.lastIndexOf("comment") + 7).trim();

                        if (commentTmp.contains("`") || commentTmp.indexOf("`") != commentTmp.lastIndexOf("`")) {
                            commentTmp = commentTmp.substring(commentTmp.indexOf("`") + 1, commentTmp.lastIndexOf("`"));
                        }

                        if (commentTmp.contains(")")) {
                            commentTmp = commentTmp.substring(0, commentTmp.lastIndexOf(")") + 1);
                        }
                        fieldComment = commentTmp;
                    } else {

                        fieldComment = columnName;
                    }

                    FieldInfo fieldInfo = new FieldInfo();
                    //
                    fieldInfo.setColumnName(columnName);
                    fieldInfo.setFieldName(fieldName);
                    fieldInfo.setFieldClass(fieldClass);
                    fieldInfo.setSwaggerClass(swaggerClass);
                    fieldInfo.setFieldComment(fieldComment);

                    fieldList.add(fieldInfo);
                }
            }
        }

        if (fieldList.size() < 1) {
            throw new CodeGenerateException(".....");
        }

        ClassInfo codeJavaInfo = new ClassInfo();
        codeJavaInfo.setTableName(tableName);
        codeJavaInfo.setClassName(className);
        codeJavaInfo.setClassComment(classComment);
        codeJavaInfo.setFieldList(fieldList);
        codeJavaInfo.setOriginTableName(originTableName);

        return codeJavaInfo;
    }


    public static ClassInfo processJsonToClassInfo(ParamInfo paramInfo) {
        ClassInfo codeJavaInfo = new ClassInfo();
        codeJavaInfo.setTableName("JsonDto");
        codeJavaInfo.setClassName("JsonDto");
        codeJavaInfo.setClassComment("JsonDto");


        if (paramInfo.getTableSql().trim().startsWith("\"")) {
            paramInfo.setTableSql("{" + paramInfo.getTableSql());
        }
        if (JSON.isValid(paramInfo.getTableSql())) {
            if (paramInfo.getTableSql().trim().startsWith("{")) {
                JSONObject jsonObject = JSONObject.parseObject(paramInfo.getTableSql().trim());

                codeJavaInfo.setFieldList(processJsonObjectToFieldList(jsonObject));
            } else if (paramInfo.getTableSql().trim().startsWith("[")) {
                JSONArray jsonArray = JSONArray.parseArray(paramInfo.getTableSql().trim());

                codeJavaInfo.setFieldList(processJsonObjectToFieldList(jsonArray.getJSONObject(0)));
            }
        }

        return codeJavaInfo;
    }


    public static ClassInfo processTableToClassInfoByRegex(ParamInfo paramInfo) {

        List<FieldInfo> fieldList = new ArrayList<FieldInfo>();

        ClassInfo codeJavaInfo = new ClassInfo();


        String DDL_PATTEN_STR = "\\s*create\\s+table\\s+(?<tableName>\\S+)[^\\(]*\\((?<columnsSQL>[\\s\\S]+)\\)[^\\)]+?(comment\\s*(=|on\\s+table)\\s*'(?<tableComment>.*?)'\\s*;?)?$";

        Pattern DDL_PATTERN = Pattern.compile(DDL_PATTEN_STR, Pattern.CASE_INSENSITIVE);


        String COL_PATTERN_STR = "\\s*(?<fieldName>\\S+)\\s+(?<fieldType>\\w+)\\s*(?:\\([\\s\\d,]+\\))?((?!comment).)*(comment\\s*'(?<fieldComment>.*?)')?\\s*(,|$)";

        Pattern COL_PATTERN = Pattern.compile(COL_PATTERN_STR, Pattern.CASE_INSENSITIVE);

        Matcher matcher = DDL_PATTERN.matcher(paramInfo.getTableSql().trim());
        if (matcher.find()) {
            String tableName = matcher.group("tableName");
            String tableComment = matcher.group("tableComment");
            codeJavaInfo.setTableName(tableName.replaceAll("'", ""));
            codeJavaInfo.setClassName(tableName.replaceAll("'", ""));
            codeJavaInfo.setClassComment(tableComment.replaceAll("'", ""));
            String columnsSQL = matcher.group("columnsSQL");
            if (columnsSQL != null && columnsSQL.length() > 0) {
                Matcher colMatcher = COL_PATTERN.matcher(columnsSQL);
                while (colMatcher.find()) {
                    String fieldName = colMatcher.group("fieldName");
                    String fieldType = colMatcher.group("fieldType");
                    String fieldComment = colMatcher.group("fieldComment");
                    if (!"key".equalsIgnoreCase(fieldType)) {
                        FieldInfo fieldInfo = new FieldInfo();
                        fieldInfo.setFieldName(fieldName.replaceAll("'", ""));
                        fieldInfo.setColumnName(fieldName.replaceAll("'", ""));
                        fieldInfo.setFieldClass(fieldType.replaceAll("'", ""));
                        fieldInfo.setFieldComment(fieldComment.replaceAll("'", ""));
                        fieldList.add(fieldInfo);
                    }
                }
            }
            codeJavaInfo.setFieldList(fieldList);
        }
        return codeJavaInfo;
    }

    public static List<FieldInfo> processJsonObjectToFieldList(JSONObject jsonObject) {
        // field List
        List<FieldInfo> fieldList = new ArrayList<FieldInfo>();
        jsonObject.keySet().stream().forEach(jsonField -> {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.setFieldName(jsonField);
            fieldInfo.setColumnName(jsonField);
            fieldInfo.setFieldClass(String.class.getSimpleName());
            fieldInfo.setFieldComment("father:" + jsonField);
            fieldList.add(fieldInfo);
            if (jsonObject.get(jsonField) instanceof JSONArray) {
                jsonObject.getJSONArray(jsonField).stream().forEach(arrayObject -> {
                    FieldInfo fieldInfo2 = new FieldInfo();
                    fieldInfo2.setFieldName(arrayObject.toString());
                    fieldInfo2.setColumnName(arrayObject.toString());
                    fieldInfo2.setFieldClass(String.class.getSimpleName());
                    fieldInfo2.setFieldComment("children:" + arrayObject.toString());
                    fieldList.add(fieldInfo2);
                });
            } else if (jsonObject.get(jsonField) instanceof JSONObject) {
                jsonObject.getJSONObject(jsonField).keySet().stream().forEach(arrayObject -> {
                    FieldInfo fieldInfo2 = new FieldInfo();
                    fieldInfo2.setFieldName(arrayObject.toString());
                    fieldInfo2.setColumnName(arrayObject.toString());
                    fieldInfo2.setFieldClass(String.class.getSimpleName());
                    fieldInfo2.setFieldComment("children:" + arrayObject.toString());
                    fieldList.add(fieldInfo2);
                });
            }
        });
        if (fieldList.size() < 1) {
            throw new CodeGenerateException("......");
        }
        return fieldList;
    }

    public static ClassInfo processInsertSqlToClassInfo(ParamInfo paramInfo) {
        // field List
        List<FieldInfo> fieldList = new ArrayList<FieldInfo>();
        //return classInfo
        ClassInfo codeJavaInfo = new ClassInfo();

        //get origin sql
        String fieldSqlStr = paramInfo.getTableSql().toLowerCase().trim();
        fieldSqlStr = fieldSqlStr.replaceAll("  ", " ").replaceAll("\\\\n`", "")
                .replaceAll("\\+", "").replaceAll("``", "`").replaceAll("\\\\", "");
        String valueStr = fieldSqlStr.substring(fieldSqlStr.lastIndexOf("values") + 6).replaceAll(" ", "").replaceAll("\\(", "").replaceAll("\\)", "");
        //get the string between insert into and values
        fieldSqlStr = fieldSqlStr.substring(0, fieldSqlStr.lastIndexOf("values"));

        System.out.println(fieldSqlStr);

        String insertSqlPattenStr = "insert into (?<tableName>.*) \\((?<columnsSQL>.*)\\)";
        //String DDL_PATTEN_STR="\\s*create\\s+table\\s+(?<tableName>\\S+)[^\\(]*\\((?<columnsSQL>[\\s\\S]+)\\)[^\\)]+?(comment\\s*(=|on\\s+table)\\s*'(?<tableComment>.*?)'\\s*;?)?$";

        Matcher matcher1 = Pattern.compile(insertSqlPattenStr).matcher(fieldSqlStr);
        while (matcher1.find()) {

            String tableName = matcher1.group("tableName");
            //System.out.println("tableName:"+tableName);
            codeJavaInfo.setClassName(tableName);
            codeJavaInfo.setTableName(tableName);

            String columnsSQL = matcher1.group("columnsSQL");
            //System.out.println("columnsSQL:"+columnsSQL);

            List<String> valueList = new ArrayList<>();
            //add values as comment
            Arrays.stream(valueStr.split(",")).forEach(column -> {
                valueList.add(column);
            });
            AtomicInteger n = new AtomicInteger(0);
            //add column to fleldList
            Arrays.stream(columnsSQL.replaceAll(" ", "").split(",")).forEach(column -> {
                FieldInfo fieldInfo2 = new FieldInfo();
                fieldInfo2.setFieldName(column);
                fieldInfo2.setColumnName(column);
                fieldInfo2.setFieldClass(String.class.getSimpleName());
                if (n.get() < valueList.size()) {
                    fieldInfo2.setFieldComment(column + " , eg." + valueList.get(n.get()));
                }
                fieldList.add(fieldInfo2);
                n.getAndIncrement();
            });

        }
        if (fieldList.size() < 1) {
            throw new CodeGenerateException("INSERT SQL....ERROR");
        }
        codeJavaInfo.setFieldList(fieldList);
        return codeJavaInfo;
    }

}
