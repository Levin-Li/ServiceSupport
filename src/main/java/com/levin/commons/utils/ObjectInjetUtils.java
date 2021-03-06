package com.levin.commons.utils;

import com.levin.commons.service.domain.VariableResolver;
import com.levin.commons.service.domain.InjectDomain;
import com.levin.commons.service.support.Locker;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ObjectInjetUtils {


    //线程安全的解析器
    private static final ExpressionParser expressionParser = new SpelExpressionParser();


    private static final ConversionService conversionService = new DefaultFormattingConversionService();


    private static final Locker loker = Locker.build();


    /**
     * 根据上下文自动注入变量
     *
     * @param bean
     * @param contexts
     * @throws IllegalAccessException
     */
    public static void autoInject(Object bean, Map<String, Object>... contexts) throws IllegalAccessException {

    }

    /**
     * 根据上下文自动注入变量
     *
     * @param context
     * @param beans
     * @throws IllegalAccessException
     */
    public static void autoInject(Map<String, Object> context, Object... beans) throws IllegalAccessException {

    }

    /**
     * 根据上下文自动注入变量
     *
     * @param bean
     * @param variableResolvers
     * @throws IllegalAccessException
     */
    public static void autoInject(Object bean, VariableResolver... variableResolvers) throws IllegalAccessException {

    }

    /**
     * 根据上下文自动注入变量
     *
     * @param variableResolver
     * @param beans
     * @throws IllegalAccessException
     */
    public static void autoInject(VariableResolver variableResolver, Object... beans) throws IllegalAccessException {

    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 根据 beans 对象字段上的InjectDomain注解，自动注入字段值
     *
     * @param context 变量上下文
     * @param beans   要注入的 bean 对象列表
     * @throws IllegalAccessException
     */
    public static void autoInjectByInjectVar(Map<String, Object> context, Object... beans) throws IllegalAccessException {

        EvaluationContext ctx = new StandardEvaluationContext(null);

        ((StandardEvaluationContext) ctx).setVariables(context);

        for (Object bean : beans) {

            if (bean == null) {
                continue;
            }

            ctx.setVariable("_this", bean);

            Class<?> pvClass = bean.getClass();

            ResolvableType classHolder = ResolvableType.forClass(pvClass);

            //todo bug
            //原子类型的注入将被忽略
            //目前还无法获取参数类型

//            if (ClassUtils.isPrimitiveOrWrapper(pvClass)) {
//                continue;
//            }

            List<Field> fields = getFields(pvClass);

            for (Field field : fields) {

                InjectDomain injectDomain = field.getAnnotation(InjectDomain.class);

                if (injectDomain == null) {
                    continue;
                }

                field.setAccessible(true);

                Object oldValue = field.get(bean);

                ctx.setVariable("_val", oldValue);

                boolean isForceInject = (boolean) expressionParser.parseExpression(injectDomain.overrideExpr()).getValue(ctx);


                VariableResolver variableResolver = null;

                Integer aa = variableResolver.resolve("aa", true, Integer.TYPE, String.class, List.class);

                if (isForceInject) {

                    Object newValue = expressionParser.parseExpression(injectDomain.injectValueExpr()).getValue(ctx);
                    //
                    if (isNullOrEmpty(newValue)) {
                        throw new SecurityException(injectDomain.remark() + " [" + field + "] 值不允许为 null 或 空字符串");
                    }

                    ResolvableType forField = ResolvableType.forField(field, classHolder);

                    //转换数据类型
                    newValue = conversionService.convert(newValue, ResolvableType.forType(field.getGenericType(), forField).resolve(field.getType()));

                    field.set(bean, newValue);

                } else if (isNullOrEmpty(oldValue)
                        && StringUtils.hasText(injectDomain.defaultValueExpr())) {
                    field.set(bean, expressionParser.parseExpression(injectDomain.defaultValueExpr()).getValue(ctx));
                }

            }
        }

    }

    /**
     * 根据 beans 对象字段上的InjectDomain注解，自动注入字段值
     *
     * @param context             变量上下文
     * @param forceInjectOverride 是否强制注入，覆盖 InjectDomain 注解上的 forceInjectExpr 属性
     * @param injectValueOverride 注入值覆盖Fun，覆盖 InjectDomain 注解上的 injectValueExpr 属性
     * @param beans               要注入的 bean 对象列表
     * @throws IllegalAccessException
     */
    public static void autoInjectByInjectVar(Map<String, Object> context,
                                             @Nullable Predicate<InjectDomain> forceInjectOverride,
                                             @Nullable Function<InjectDomain, Object> injectValueOverride,
                                             Object... beans) throws IllegalAccessException {

        EvaluationContext ctx = new StandardEvaluationContext(null);

        ((StandardEvaluationContext) ctx).setVariables(context);

        for (Object bean : beans) {

            if (bean == null) {
                continue;
            }

            ctx.setVariable("_this", bean);

            Class<?> pvClass = bean.getClass();

            ResolvableType classHolder = ResolvableType.forClass(pvClass);

            //todo bug
            //原子类型的注入将被忽略
            //目前还无法获取参数类型

//            if (ClassUtils.isPrimitiveOrWrapper(pvClass)) {
//                continue;
//            }

            List<Field> fields = getFields(pvClass);

            for (Field field : fields) {

                InjectDomain injectDomain = field.getAnnotation(InjectDomain.class);

                if (injectDomain == null) {
                    continue;
                }

                field.setAccessible(true);

                Object oldValue = field.get(bean);

                ctx.setVariable("_val", oldValue);

                boolean isForceInject = true;

                if (forceInjectOverride != null) {
                    isForceInject = forceInjectOverride.test(injectDomain);
                } else {
                    isForceInject = (boolean) expressionParser.parseExpression(injectDomain.overrideExpr()).getValue(ctx);
                }

                if (isForceInject) {

                    Object newValue = null;

                    if (injectValueOverride != null) {
                        //强制改变字段值
                        newValue = injectValueOverride.apply(injectDomain);
                    } else {
                        //强制改变字段值
                        newValue = expressionParser.parseExpression(injectDomain.injectValueExpr()).getValue(ctx);
                    }

                    //
                    if (isNullOrEmpty(newValue)) {
                        throw new SecurityException(injectDomain.remark() + " [" + field + "] 值不允许为 null 或 空字符串");
                    }

                    ResolvableType forField = ResolvableType.forField(field, classHolder);

                    //转换数据类型
                    newValue = conversionService.convert(newValue, ResolvableType.forType(field.getGenericType(), forField).resolve(field.getType()));

                    field.set(bean, newValue);

                } else if (isNullOrEmpty(oldValue)
                        && StringUtils.hasText(injectDomain.defaultValueExpr())) {
                    field.set(bean, expressionParser.parseExpression(injectDomain.defaultValueExpr()).getValue(ctx));
                }

            }
        }

    }


    private static boolean isNullOrEmpty(Object newValue) {
        return newValue == null || ((newValue instanceof CharSequence) && newValue.toString().trim().length() == 0);
    }

    private static final Map<String, List<Field>> fieldMap = new ConcurrentReferenceHashMap<>();

    private static List<Field> getFields(Class clazz) {

        final String className = clazz.getName();

        synchronized (loker.getLock(className)) {

            List<Field> fields = fieldMap.get(className);

            if (fields == null) {

                final List<Field> tempList = new ArrayList<>();

                ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
                    @Override
                    public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

                        if (field.isAnnotationPresent(InjectDomain.class)) {
                            tempList.add(field);
                        }
                    }
                });

                fieldMap.put(className, tempList);

                return tempList;

            } else {

                return fields;
            }

        }

    }
}
