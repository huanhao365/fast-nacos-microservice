package fast.cloud.nacos.rest.client.example;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class FastCommonEsRestClientExampleApplicationTests {
    @Autowired
    private RestHighLevelClient client;

    @Test
    public void testCreateIndex() throws IOException {
        //????????????????????????
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("hello_es");
        createIndexRequest.settings(Settings.builder().put("number_of_shards", 2)
                .put("number_of_replicas", 1).build());
        createIndexRequest.mapping("doc", "{\n" +
                "\t\"properties\": {\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"analyzer\":\"ik_max_word\",\n" +
                "           \"search_analyzer\":\"ik_smart\"\n" +
                "\t\t},\n" +
                "\t\t\"description\": {\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"analyzer\": \"ik_max_word\",\n" +
                "            \"search_analyzer\":\"ik_smart\"\n" +
                "\t\t\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}", XContentType.JSON);
        //?????????????????????
        IndicesClient indices = client.indices();
        CreateIndexResponse createIndexResponse = indices.create(createIndexRequest, RequestOptions.DEFAULT);
        boolean acknowledged = createIndexResponse.isAcknowledged();
        log.info("isAcknowledged:{}", acknowledged);
    }

    @Test
    public void testDeleteIndex() throws IOException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("hello_es");
        AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        boolean acknowledged = deleteIndexResponse.isAcknowledged();
        log.info("isAcknowledged:{}", acknowledged);

    }

    @Test
    public void testAddDoc() throws IOException {
        IndexRequest indexRequest = new IndexRequest("hello_es", "doc");
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("name", "springcloud");
        jsonMap.put("description", "??????????????????????????????????????????: 1.????????????????????? 2.spring cloud????????????3.??????SpringBoot4.????????????eureka");
        indexRequest.source(jsonMap);
        client.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Test
    public void getDoc() throws IOException {
        GetRequest getRequest = new GetRequest(
                "hello_es",
                "doc",
                "gdbSpW4BXv1xyLWLmtrS");

        //?????????????????????
        String[] includes = new String[]{"name", "description", "studymodel"};
        String[] excludes = Strings.EMPTY_ARRAY;
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, excludes);
        getRequest.fetchSourceContext(fetchSourceContext);

        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);

        Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
        System.out.println(sourceAsMap);
    }

    //????????????
    @Test
    public void updateDoc() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest("hello_es", "doc",
                "dda3pW4BXv1xyLWLjNpZ");
        Map<String, String> map = new HashMap<>();
        map.put("name", "Spring Cloud??????");
        updateRequest.doc(map);
        UpdateResponse update = client.update(updateRequest, RequestOptions.DEFAULT);
        RestStatus status = update.status();
        System.out.println(status);
    }

    //????????????
    @Test
    public void testBulkAdd() throws IOException {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("name", "spring cloud??????");
        jsonMap.put("description", "??????????????????????????????????????????: 1.????????????????????? 2.spring cloud????????????3.??????SpringBoot4.????????????eureka");
        jsonMap.put("studymodel", "201001");

        BulkRequest request = new BulkRequest();
        request.add(new IndexRequest("hello_es", "doc")
                .source(jsonMap));
        request.add(new IndexRequest("hello_es", "doc")
                .source(jsonMap));
        request.add(new IndexRequest("hello_es", "doc")
                .source(jsonMap));
        BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        Stream.of(bulkResponse.getItems()).forEach(model -> log.info("index:{},response:{}",
                model.getIndex(), model.getResponse()));
    }

    //??????????????????
    @Test
    public void testSearchAll() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        //???????????????????????????
        searchSourceBuilder.fetchSource(new String[]{"name", "description", "studymodel"}, Strings.EMPTY_ARRAY);
        //???????????????????????????????????????0??????
        searchSourceBuilder.from(0);
        //??????????????????
        searchSourceBuilder.size(2);

        //????????????
        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
//        searchSourceBuilder.sort(new FieldSortBuilder("name").order(SortOrder.ASC));

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //Term Query
    @Test
    public void testTermQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        //????????????spring?????????
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("name", "spring");
        searchSourceBuilder.query(termQueryBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //??????id????????????
    @Test
    public void testQueryId() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());

        String[] ids = new String[]{"gdbSpW4BXv1xyLWLmtrS"};
        List<String> idList = Arrays.asList(ids);
        searchSourceBuilder.query(QueryBuilders.termsQuery("_id", idList));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //matchQuery
    @Test
    public void testMatchQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(QueryBuilders.matchQuery("name", "spring??????").operator(Operator.OR));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //minimum_should_match
    @Test
    public void testMinimumShouldMatch() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(QueryBuilders.matchQuery("description", "??????????????????").minimumShouldMatch("80%"));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //multi Query and boost
    @Test
    public void testMultiQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MultiMatchQueryBuilder multiMatchQueryBuilder =
                QueryBuilders.multiMatchQuery("??????", "description", "name");
        multiMatchQueryBuilder.field("name", 10);
        searchSourceBuilder.query(multiMatchQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //bool query
    @Test
    public void testBoolQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MultiMatchQueryBuilder multiMatchQueryBuilder =
                QueryBuilders.multiMatchQuery("??????", "description", "name");
        multiMatchQueryBuilder.field("name", 10);
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("studymodel", 201001);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(termQueryBuilder).must(multiMatchQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //filter
    @Test
    public void testFilter() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MultiMatchQueryBuilder multiMatchQueryBuilder =
                QueryBuilders.multiMatchQuery("??????", "description", "name");
        multiMatchQueryBuilder.field("name", 10);
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("studymodel", 201001);
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(termQueryBuilder).must(multiMatchQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);


        boolQueryBuilder.filter(QueryBuilders.termQuery("studymodel", 201001));
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            log.info("sourceAsString:{}", sourceAsString);
        });
    }

    //??????????????????
    @Test
    public void testHeightLight() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("name", "??????"));

        searchSourceBuilder.fetchSource(new String[]{"name", "description"}, Strings.EMPTY_ARRAY);
        // ??????????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.requireFieldMatch(true).field("name")
                .preTags("<strong>").postTags("</strong>");
        searchSourceBuilder.highlighter(highlightBuilder);


        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Stream.of(searchHits).forEach(model -> {
            String sourceAsString = model.getSourceAsString();
            Map<String, HighlightField> highlightFields = model.getHighlightFields();
            String name = (String) model.getSourceAsMap().get("name");
            if (highlightFields != null) {
                HighlightField nameField = highlightFields.get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text str : fragments) {
                        stringBuffer.append(str.string());
                    }
                    name = stringBuffer.toString();
                }
            }

            log.info("name:{}=====sourceAsString:{}", name, sourceAsString);
        });
    }

    @Test
    public void testSuggest() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hello_es");
        searchRequest.types("doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());

        //???????????????
        //????????????
        SuggestionBuilder termSuggestionBuilder =
                SuggestBuilders.termSuggestion("name").text("???????????????");
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("suggest_user", termSuggestionBuilder);
        searchSourceBuilder.suggest(suggestBuilder);

        searchRequest.source(searchSourceBuilder);
        //3???????????????
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //4???????????????
        //????????????????????????
        if (RestStatus.OK.equals(searchResponse.status())) {
            // ??????????????????
            Suggest suggest = searchResponse.getSuggest();
            TermSuggestion termSuggestion = suggest.getSuggestion("suggest_user");
            for (TermSuggestion.Entry entry : termSuggestion.getEntries()) {
                log.info("text: " + entry.getText().string());
                for (TermSuggestion.Entry.Option option : entry) {
                    String suggestText = option.getText().string();
                    log.info("   suggest option : " + suggestText);
                }

            }
        }
    }

    //Highlight
    @Test
    public void testHighlight() throws IOException {
        //??????????????????
        SearchRequest searchRequest = new SearchRequest("hello_es");
        //????????????
        searchRequest.types("doc");
        //?????????????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //?????????????????????,????????????????????????????????????????????????????????????????????????????????????????????????
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel", "price", "timestamp"}, new String[]{});

        //????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<tag>");
        highlightBuilder.postTags("</tag>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
//        highlightBuilder.fields().add(new HighlightBuilder.Field("description"));
        searchSourceBuilder.highlighter(highlightBuilder);

        //???????????????????????????????????????
        searchRequest.source(searchSourceBuilder);
        //????????????,???ES??????http??????
        SearchResponse searchResponse = client.search(searchRequest);
        //????????????
        SearchHits hits = searchResponse.getHits();
        //????????????????????????
        long totalHits = hits.getTotalHits();
        //???????????????????????????
        SearchHit[] searchHits = hits.getHits();
        //?????????????????????
        for (SearchHit hit : searchHits) {
            //???????????????
            String id = hit.getId();
            //???????????????
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //????????????name????????????
            String name = (String) sourceAsMap.get("name");
            //??????????????????
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                //??????name????????????
                HighlightField nameHighlightField = highlightFields.get("name");
                if (nameHighlightField != null) {
                    Text[] fragments = nameHighlightField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text text : fragments) {
                        stringBuffer.append(text);
                    }
                    name = stringBuffer.toString();
                }
            }

            //???????????????????????????????????????????????????description???????????????
            String description = (String) sourceAsMap.get("description");
            //????????????
            String studymodel = (String) sourceAsMap.get("studymodel");
            //??????
            Double price = (Double) sourceAsMap.get("price");
            //??????
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
        }

    }

    @Test
    public void testAgg() {

        try {
            Map<String, Long> groupMap = getTermsAgg(QueryBuilders.matchAllQuery(), "name", "hello_es");
            groupMap.forEach((key, value) -> System.out.println(key + " -> " + value.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Long> getTermsAgg(QueryBuilder queryBuilder, String field, String... indexs) throws IOException {
        Map<String, Long> groupMap = new HashMap<>();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.size(0);

        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("agg").field(field);
        searchSourceBuilder.aggregation(aggregationBuilder);

        SearchRequest searchRequest = new SearchRequest(indexs);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms terms = searchResponse.getAggregations().get("agg");
        for (Terms.Bucket entry : terms.getBuckets()) {
            groupMap.put(entry.getKey().toString(), entry.getDocCount());
        }
        return groupMap;
    }


    public Map<String, Map<String, Long>> getTermsAggTwoLevel(QueryBuilder queryBuilder, String field1, String field2, String... indexs) throws IOException {
        Map<String, Map<String, Long>> groupMap = new HashMap<>();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.size(0);

        AggregationBuilder agg1 = AggregationBuilders.terms("agg1").field(field1);
        AggregationBuilder agg2 = AggregationBuilders.terms("agg2").field(field2);
        agg1.subAggregation(agg2);
        searchSourceBuilder.aggregation(agg1);

        SearchRequest searchRequest = new SearchRequest(indexs);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms terms1 = searchResponse.getAggregations().get("agg1");
        Terms terms2;
        for (Terms.Bucket bucket1 : terms1.getBuckets()) {
            terms2 = bucket1.getAggregations().get("agg2");
            Map<String, Long> map2 = new HashMap<>();
            for (Terms.Bucket bucket2 : terms2.getBuckets()) {
                map2.put(bucket2.getKey().toString(), bucket2.getDocCount());
            }
            groupMap.put(bucket1.getKey().toString(), map2);
        }
        return groupMap;
    }


}
