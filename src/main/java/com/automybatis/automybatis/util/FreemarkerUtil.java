package com.automybatis.automybatis.util;



import com.automybatis.automybatis.entity.ClassInfo;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Locale;
import java.util.Map;


@Slf4j

public class FreemarkerUtil {



    private static final String TEMPLATE_PATH = "E:\\IdeaProjects\\github\\demo\\src\\main\\resources\\dao\\";
    private static final String CLASS_PATH = "C:\\";


    public static String escapeString(String originStr) {
        return originStr.replaceAll("#", "\\#").replaceAll("$", "\\$");
    }

    private static Configuration freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

    static {
        try {

            freemarkerConfig.setClassForTemplateLoading(FreemarkerUtil.class, "/templates/code-generator");
            freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(FreemarkerUtil.class, "/templates/code-generator"));
            freemarkerConfig.setNumberFormat("#");
            freemarkerConfig.setClassicCompatible(true);
            freemarkerConfig.setDefaultEncoding("UTF-8");
            freemarkerConfig.setLocale(Locale.CHINA);
            freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public static String processTemplateIntoString(Template template, Object model, ClassInfo classInfo, String outFilePath)
            throws IOException, TemplateException {

        String result = "";
        String className = classInfo.getClassName();
        String templateName = template.getName();
        String preFixName = templateName.substring(8,templateName.length()-4);
        if (preFixName.equals("mapper")){
            result = className + "Mapper.java";
        }else if (preFixName.equals("model")){
            result = className + ".java";
        }else if (preFixName.equals("mybatis")){
            result = className + "Mapper.xml";
        }
        if (StringUtils.isNotBlank(outFilePath)){

            File docFile = new File(outFilePath + "\\" + result);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docFile)));
            template.process(model, out);
            out.close();
        }else {
            File docFile = new File(CLASS_PATH + "\\" + result);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docFile)));
            template.process(model, out);
            out.close();
        }


        return result;
    }


    public static String processString(String templateName, Map<String, Object> params)
            throws IOException, TemplateException {
        log.info("templateName=="+templateName);
        Template template = freemarkerConfig.getTemplate(templateName);
        log.info("template.getName()="+template.getName());
        ClassInfo classInfo = (ClassInfo)params.get("classInfo");
        String outFilePath = (String)params.get("path");
        String htmlText = escapeString(processTemplateIntoString(template, params,classInfo,outFilePath));
        return htmlText;
    }

}
