package com.tyfanch.dynamicjs.parser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.tyfanch.dynamicjs.annotation.ScriptMethod;
import com.tyfanch.dynamicjs.annotation.ScriptRunner;
import com.tyfanch.dynamicjs.config.DefaultScriptConfig;
import com.tyfanch.dynamicjs.model.MethodConfig;
import com.tyfanch.dynamicjs.model.ScriptConfig;

/**
 * 从配置类中读取Script配置
 */
public class ClassConfigParser implements ConfigParser {
    private Class<?> configClass;
    private ScriptConfig scriptConfig;

    /***
     * 必须指定配置类
     * @param configClass 配置类
     */
    public ClassConfigParser(Class<?> configClass) {
        this.configClass = configClass;
    }

    @Override
    public ScriptConfig parseScriptConfig() {
        Map<String, MethodConfig> methodConfigMap;

        this.scriptConfig = this.parseBaseScriptConfig();
        methodConfigMap = this.parseMethodConfigMap();
        this.scriptConfig.setMethods(methodConfigMap);

        return this.scriptConfig;
    }

    /**
     * 获取基本配置（没有方法配置）
     * @return 基本配置
     */
    private ScriptConfig parseBaseScriptConfig() {
        ScriptConfig scriptConfig;

        if (this.configClass.isAnnotationPresent(ScriptRunner.class)) {
            // 有配置注解，获取注解中的配置
            scriptConfig = this.getScriptConfigByAnnotation();
        } else {
            // 没有配置注解，获取默认配置
            scriptConfig = this.getScriptConfigByDefault();
        }

        return scriptConfig;
    }

    /**
     * 从注解中获取配置
     * @return Script配置
     */
    private ScriptConfig getScriptConfigByAnnotation() {
        ScriptConfig scriptConfig = new ScriptConfig();
        ScriptRunner scriptRunner = this.configClass.getAnnotation(ScriptRunner.class);
        String value = scriptRunner.value().trim();
        String engine = scriptRunner.engine().trim();

        // 如果引擎名称都为空则抛出异常
        if (value.isEmpty() && engine.isEmpty()) {
            scriptConfig.setEngine(DefaultScriptConfig.DEFAULT_ENGINE);
            //String errorMsg = String.format(
            //    "Invalid engine name: %s and %s",
            //    value, engine);
            //
            //throw new RuntimeException(errorMsg);
        } else {
            // 取非空的值
            scriptConfig.setEngine(value.isEmpty() ? engine : value);
        }

        // 设置命名空间
        scriptConfig.setNamespace(this.configClass.getName());

        return scriptConfig;
    }

    /**
     * 获取默认配置
     * @return Script配置
     */
    private ScriptConfig getScriptConfigByDefault() {
        ScriptConfig scriptConfig = new ScriptConfig();

        scriptConfig.setEngine(DefaultScriptConfig.DEFAULT_ENGINE);
        scriptConfig.setNamespace(this.configClass.getName());

        return scriptConfig;
    }

    /**
     * 解析方法，封装成Map
     * @return 方法Map
     */
    private Map<String, MethodConfig> parseMethodConfigMap() {
        Map<String, MethodConfig> methodConfigMap = new HashMap<>();
        Method[] methods = this.configClass.getMethods();

        for (Method method : methods) {
            MethodConfig methodConfig;

            // 如果有注解则解析配置
            if (method.isAnnotationPresent(ScriptMethod.class)) {
                methodConfig = this.getMethodConfigByAnnotation(method);
                methodConfigMap.put(method.getName(), methodConfig);
            }
        }

        return methodConfigMap;
    }

    /**
     * 从注解中解析方法配置
     * @param method 要解析的方法
     * @return 方法配置
     */
    private MethodConfig getMethodConfigByAnnotation(Method method) {
        MethodConfig methodConfig = new MethodConfig();
        ScriptMethod scriptMethod = method.getAnnotation(ScriptMethod.class);
        String value = scriptMethod.value().trim();
        String engine = scriptMethod.engine().trim();
        String script = scriptMethod.script();

        if (value.isEmpty() && engine.isEmpty()) {
            // 都为空则继承Script配置中的引擎
            methodConfig.setEngine(this.scriptConfig.getEngine());
        } else {
            // 不为空则设置独有的引擎
            methodConfig.setEngine(value.isEmpty() ? engine : value);
        }

        // 设置脚本内容
        methodConfig.setScript(script);

        return methodConfig;
    }
}