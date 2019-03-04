package com.iamazy.springcloud.elasticsearch.dsl.sql.model;

import com.iamazy.springcloud.elasticsearch.dsl.cons.CoreConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Copyright 2018-2019 iamazy Logic Ltd
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author iamazy
 * @date 2019/2/19
 * @descrition
 **/
@Slf4j
@Data
public class ElasticSqlParseResult {

    private int from=0;
    private int size=15;
    private List<String> indices;
    private String type="_doc";
    private String queryAs;
    private List<String> routingBy;
    private List<String> queryFieldList;
    private transient BoolQueryBuilder whereCondition;
    private transient BoolQueryBuilder matchCondition;
    private transient List<SortBuilder> orderBy;
    private transient boolean isTopStatsAgg=true;
    private transient List<AggregationBuilder> groupBy;
    private transient SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();

    public DeleteByQueryRequest toDelRequest(){
        DeleteByQueryRequest deleteByQueryRequest=new DeleteByQueryRequest(toRequest().indices());
        deleteByQueryRequest.setQuery(searchSourceBuilder.query());
        if(StringUtils.isNotBlank(type)) {
            deleteByQueryRequest.types(type);
        }
        if(CollectionUtils.isNotEmpty(routingBy)) {
            deleteByQueryRequest.setRouting(routingBy.get(0));
        }

        if(size<0){
            deleteByQueryRequest.setSize(15);
        }else {
            deleteByQueryRequest.setSize(size);
        }
        return deleteByQueryRequest;
    }

    public SearchRequest toRequest(){
        SearchRequest searchRequest=new SearchRequest();
        if(CollectionUtils.isNotEmpty(indices)){
            searchRequest.indices(indices.toArray(new String[0]));
        }
        if(StringUtils.isNotBlank(type)){
            searchRequest.types(type);
        }
        if(from<0){
            log.debug("[from] is gte zero, assign 0 to [from(int)] as default value!!!");
            //这里不会修改from的值
            searchSourceBuilder.from(0);
        }else{
            searchSourceBuilder.from(from);
        }
        if(size<0){
            log.debug("[size] is gte zero, assign 15 to [size(int)] as default value!!!");
            searchSourceBuilder.size(15);
        }else{
            searchSourceBuilder.size(size);
        }
        if(whereCondition!=null&&whereCondition.hasClauses()){
            if(matchCondition!=null&&matchCondition.hasClauses()){
                searchSourceBuilder.query(matchCondition.filter(whereCondition));
            }else{
                searchSourceBuilder.query(QueryBuilders.boolQuery().filter(whereCondition));
            }
        }else{
            if(matchCondition!=null&&matchCondition.hasClauses()){
                searchSourceBuilder.query(matchCondition);
            }else{
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            }
        }

        if(CollectionUtils.isNotEmpty(orderBy)){
            for(SortBuilder sortBuilder:orderBy){
                searchSourceBuilder.sort(sortBuilder);
            }
        }

        if(CollectionUtils.isNotEmpty(queryFieldList)){
            searchSourceBuilder.fetchSource(queryFieldList.toArray(new String[0]),null);
        }

        if(CollectionUtils.isNotEmpty(routingBy)){
            searchRequest.routing(routingBy.toArray(new String[0]));
        }

        if(CollectionUtils.isNotEmpty(groupBy)){
            if(!isTopStatsAgg()){
                AggregationBuilder preAgg=null;
                for(AggregationBuilder aggItem:groupBy){
                    if(preAgg==null){
                        preAgg=aggItem;
                        continue;
                    }
                    preAgg.subAggregation(aggItem);
                    preAgg=aggItem;
                }
                searchSourceBuilder.aggregation(groupBy.get(0));
            }else{
                for(AggregationBuilder aggItem:groupBy){
                    searchSourceBuilder.aggregation(aggItem);
                }
            }
        }
        return searchRequest.source(searchSourceBuilder);
    }

    String toDsl(SearchRequest searchRequest){
        return searchRequest.source().toString();
    }

    public String toPrettyDsl(SearchRequest searchRequest){
        try {
            Object o=CoreConstants.OBJECT_MAPPER.readValue(toDsl(searchRequest),Object.class);
            return CoreConstants.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch Dsl解析出错!!!");
        }
    }

    @Override
    public String toString() {
        String ptn = "index:%s,type:%s,query_as:%s,from:%s,size:%s,routing:%s,dsl:%s";
        return String.format(
                ptn, indices, type, queryAs, from, size,
                (routingBy != null ? routingBy.toString() : "[]"), toDsl(toRequest())
        );
    }



}
