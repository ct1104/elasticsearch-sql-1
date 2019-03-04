package com.iamazy.springcloud.elasticsearch.dsl.sql.parser.sql.sort;


import com.iamazy.springcloud.elasticsearch.dsl.sql.helper.ElasticSqlMethodInvokeHelper;
import com.iamazy.springcloud.elasticsearch.dsl.sql.parser.query.method.MethodInvocation;
import com.iamazy.springcloud.elasticsearch.dsl.sql.exception.ElasticSql2DslException;
import com.iamazy.springcloud.elasticsearch.dsl.sql.parser.query.method.expr.AbstractParameterizedMethodExpression;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Map;

public abstract class AbstractMethodSortParser extends AbstractParameterizedMethodExpression implements MethodSortParser {

    protected abstract SortBuilder parseMethodSortBuilder(
            MethodInvocation invocation, SortOrder order, Map<String, Object> extraParamMap) throws ElasticSql2DslException;

    @Override
    protected String defineExtraParamString(MethodInvocation invocation) {
        return StringUtils.EMPTY;
    }

    @Override
    public void checkMethodInvocation(MethodInvocation invocation) throws ElasticSql2DslException {

    }

    @Override
    public boolean isMatchMethodInvocation(MethodInvocation invocation) {
        return ElasticSqlMethodInvokeHelper.isMethodOf(defineMethodNames(), invocation.getMethodName());
    }

    @Override
    public SortBuilder parseMethodSortBuilder(MethodInvocation invocation, SortOrder order) throws ElasticSql2DslException {
        if (!isMatchMethodInvocation(invocation)) {
            throw new ElasticSql2DslException(
                    String.format("[syntax error] Expected method name is one of [%s],but get [%s]",
                            defineMethodNames(), invocation.getMethodName()));
        }
        checkMethodInvocation(invocation);

        Map<String, Object> extraParamMap = generateRawTypeParameterMap(invocation);
        return parseMethodSortBuilder(invocation, order, extraParamMap);
    }
}
