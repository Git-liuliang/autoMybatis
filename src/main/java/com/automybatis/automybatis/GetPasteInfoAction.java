package com.automybatis.automybatis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.automybatis.automybatis.entity.ClassInfo;
import com.automybatis.automybatis.entity.ParamInfo;
import com.automybatis.automybatis.util.FreemarkerUtil;
import com.automybatis.automybatis.util.MapUtil;
import com.automybatis.automybatis.util.TableParseUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import freemarker.template.TemplateException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GetPasteInfoAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here

        // 获取选中的内容
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText =  selectionModel.getSelectedText();

        //获取选中内容文件的当前路径并打印
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE); //获取当前类文件的路径
        VirtualFile vFile = psiFile.getOriginalFile().getVirtualFile();
        String path = vFile.getPath();

        int index = path.lastIndexOf("/");

        String parentPath = path.substring(0, index);

        Messages.showMessageDialog(project, parentPath, "title", Messages.getInformationIcon());

        NotificationGroup notificationGroup = new NotificationGroup("s", NotificationDisplayType.BALLOON,true);
        Notification notification =  notificationGroup.createNotification(selectedText, MessageType.INFO);
        Notifications.Bus.notify(notification);


        ParamInfo paramInfo = new ParamInfo();
        paramInfo.setTableSql(selectedText);
        Map<String,Object> option = new HashMap<>();

        option.put("dataType","sql");
        option.put("authorName","autoMybatis");
        option.put("packageName","com.sitech.boss.dao");
        option.put("nameCaseType","CamelCase");
        option.put("path",parentPath);
        paramInfo.setOptions(option);

        ClassInfo classInfo = null;
        String dataType = MapUtil.getString(paramInfo.getOptions(),"dataType");
        if ("sql".equals(dataType)||dataType==null) {
            try {
                classInfo = TableParseUtil.processTableIntoClassInfo(paramInfo);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }else if ("json".equals(dataType)) {

            classInfo = TableParseUtil.processJsonToClassInfo(paramInfo);

        } else if ("insert-sql".equals(dataType)) {
            classInfo = TableParseUtil.processInsertSqlToClassInfo(paramInfo);

        } else if ("sql-regex".equals(dataType)) {
            classInfo = TableParseUtil.processTableToClassInfoByRegex(paramInfo);

        }

        //2.Set the params 设置表格参数
        paramInfo.getOptions().put("classInfo", classInfo);
        paramInfo.getOptions().put("tableName", classInfo == null ? System.currentTimeMillis() : classInfo.getTableName());
        try {

            Map<String, String> result = new HashMap<>(32);
            result.put("tableName", MapUtil.getString(paramInfo.getOptions(),"tableName"));
            JSONArray parentTemplates = JSONArray.parseArray(getTemplateConfig());
            for (int i = 0; i <parentTemplates.size() ; i++) {
                JSONObject parentTemplateObj = parentTemplates.getJSONObject(i);
                for (int x = 0; x <parentTemplateObj.getJSONArray("templates").size() ; x++) {
                    JSONObject childTemplate = parentTemplateObj.getJSONArray("templates").getJSONObject(x);
                    result.put(childTemplate.getString("name"), FreemarkerUtil.processString(parentTemplateObj.getString("group") + "/" +childTemplate.getString("name")+ ".ftl", paramInfo.getOptions()));

                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (TemplateException ex) {
            throw new RuntimeException(ex);
        }

    }

    public String getTemplateConfig() throws IOException {
        String templateCpnfig = null;
        if (templateCpnfig != null) {
        } else {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template.json");
            templateCpnfig = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
            inputStream.close();
        }
        return templateCpnfig;
    }



}
