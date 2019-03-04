package com.iamazy.springcloud.elasticsearch.dsl.sql.parser.query.method.term;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.google.common.collect.ImmutableList;
import com.iamazy.springcloud.elasticsearch.dsl.sql.parser.query.method.AbstractFieldSpecificMethodQueryParser;
import com.iamazy.springcloud.elasticsearch.dsl.sql.parser.query.method.MethodInvocation;
import com.iamazy.springcloud.elasticsearch.dsl.sql.exception.ElasticSql2DslException;
import com.iamazy.springcloud.elasticsearch.dsl.sql.listener.ParseActionListener;
import com.iamazy.springcloud.elasticsearch.dsl.sql.utils.Constants;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.List;
import java.util.Map;

public class FuzzyAtomicQueryParser extends AbstractFieldSpecificMethodQueryParser {

    private static final List<String> FUZZY_QUERY_METHOD = ImmutableList.of("fuzzy", "fuzzy_query", "fuzzyQuery");

    public FuzzyAtomicQueryParser(ParseActionListener parseActionListener) {
        super(parseActionListener);
    }

    @Override
    public List<String> defineMethodNames() {
        return FUZZY_QUERY_METHOD;
    }

    @Override
    public SQLExpr defineFieldExpr(MethodInvocation invocation) {
        return invocation.getParameter(0);
    }

    @Override
    protected String defineExtraParamString(MethodInvocation invocation) {
        int extraParamIdx = 2;

        return (invocation.getParameterCount() == extraParamIdx + 1)
                ? invocation.getParameterAsString(extraParamIdx) : StringUtils.EMPTY;
    }

    @Override
    public void checkMethodInvocation(MethodInvocation invocation) {
        if (invocation.getParameterCount() != 2 && invocation.getParameterCount() != 3) {
            throw new ElasticSql2DslException(
                    String.format("[syntax error] There's no %s args method named [%s].",
                            invocation.getParameterCount(), invocation.getMethodName()));
        }

        String text = invocation.getParameterAsString(1);
        if (StringUtils.isEmpty(text)) {
            throw new ElasticSql2DslException("[syntax error] Fuzzy search text can not be blank!");
        }

        if (invocation.getParameterCount() == 3) {
            String extraParamString = defineExtraParamString(invocation);
            if (StringUtils.isEmpty(extraParamString)) {
                throw new ElasticSql2DslException("[syntax error] The extra param of fuzzy method can not be blank");
            }
        }
    }

    @Override
    protected QueryBuilder buildQuery(MethodInvocation invocation, String fieldName, Map<String, String> extraParams) {
        String text = invocation.getParameterAsString(1);
        FuzzyQueryBuilder fuzzyQuery = QueryBuilders.fuzzyQuery(fieldName, text);

        setExtraMatchQueryParam(fuzzyQuery, extraParams);
        return fuzzyQuery;
    }

    private void setExtraMatchQueryParam(FuzzyQueryBuilder fuzzyQuery, Map<String, String> extraParamMap) {
        if (MapUtils.isEmpty(extraParamMap)) {
            return;
        }
        if (extraParamMap.containsKey(Constants.BOOST)) {
            String val = extraParamMap.get(Constants.BOOST);
            fuzzyQuery.boost(Float.valueOf(val));
        }
        if (extraParamMap.containsKey(Constants.TRANSPOSITIONS)) {
            String val = extraParamMap.get(Constants.TRANSPOSITIONS);
            fuzzyQuery.transpositions(Boolean.parseBoolean(val));
        }
        if (extraParamMap.containsKey(Constants.PREFIX_LENGTH)) {
            String val = extraParamMap.get(Constants.PREFIX_LENGTH);
            fuzzyQuery.prefixLength(Integer.valueOf(val));
        }
        if (extraParamMap.containsKey(Constants.MAX_EXPANSIONS)) {
            String val = extraParamMap.get(Constants.MAX_EXPANSIONS);
            fuzzyQuery.maxExpansions(Integer.valueOf(val));
        }
        if (extraParamMap.containsKey(Constants.REWRITE)) {
            String val = extraParamMap.get(Constants.REWRITE);
            fuzzyQuery.rewrite(val);
        }

        if (extraParamMap.containsKey(Constants.FUZZINESS)) {
            String val = extraParamMap.get(Constants.FUZZINESS).toLowerCase();

            switch (val){
                case "0":
                case "zero":{
                    fuzzyQuery.fuzziness(Fuzziness.ZERO);
                    break;
                }
                case "1":
                case "one":{
                    fuzzyQuery.fuzziness(Fuzziness.ONE);
                    break;
                }
                case "2":
                case "two":{
                    fuzzyQuery.fuzziness(Fuzziness.TWO);
                    break;
                }
                default:{
                    fuzzyQuery.fuzziness(Fuzziness.AUTO);
                }
            }
        }
    }
}
