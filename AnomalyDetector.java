package org.example;

import static org.example.StringConstants.BATCH_STRING;
import static org.example.StringConstants.LINE_BREAK;
import static org.example.StringConstants.NUMBER_OF_SECONDS_DAY;
import static org.example.StringConstants.REAL_TIME_STRING;
import static org.example.StringConstants.WAVEFRONT_PREFIX;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class AnomalyDetector {

    public List<PipelineMetricsPojo> identifyAnomalies(List<PipelineMetricsPojo> healthyPipelinesListWithTrafficInfo,
                                                               long epochStartTime, long epochEndTime)
        throws ParseException {
        List<PipelineMetricsPojo> anomalyPipelines = new ArrayList<>();
        String currentDate = HelperUtil.convertToMMddYYYY(epochEndTime);
        String previousDate = HelperUtil.convertToMMddYYYY(epochEndTime-NUMBER_OF_SECONDS_DAY);

        for(PipelineMetricsPojo pipelineMetricsPojo:healthyPipelinesListWithTrafficInfo) {

            if (!pipelineMetricsPojo.isTestPipeline()) {
            // logic to identify anamolies in batch pipelines, if they do not receive traffic in last 7 days
                if(pipelineMetricsPojo.getOnBoardingType().equals(BATCH_STRING) && HelperUtil.getNumbersOfDaysSinceOnboarded(epochEndTime,
                    pipelineMetricsPojo.getCreateDateEpoch()) > 30 && pipelineMetricsPojo.getMetricsDateEventsCountMap().size() == 0) {
                    pipelineMetricsPojo.setTypeOfAnomaly("NO_TRAFFIC_LAST7DAYS");
                    anomalyPipelines.add(pipelineMetricsPojo);
                }
                // logic to identify anamolies in batch pipelines, if they do not receive traffic in today
            if ((pipelineMetricsPojo.getOnBoardingType().equals(REAL_TIME_STRING) || pipelineMetricsPojo.getOnBoardingType().equals("N/A"))
                && !pipelineMetricsPojo.getMetricsDateEventsCountMap().containsKey(currentDate) && HelperUtil.getNumbersOfDaysSinceOnboarded(epochEndTime,
                pipelineMetricsPojo.getCreateDateEpoch())>30) {
                pipelineMetricsPojo.setTypeOfAnomaly("DIP_IN_TRAFFIC_TO_0");
                anomalyPipelines.add(pipelineMetricsPojo);
            }

            } else {
                anomalyPipelines.add(pipelineMetricsPojo);
            }
        }
        return anomalyPipelines;
    }

    public String getIdentityAuthHeader(String profileId, String appSecret) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(StringConstants.IDENTITY_URL);

        String query = StringConstants.AUTH_QUERY;
        String json = "{\"query\":\"" + query + "\",\"variables\":{\"input\":{\"profileId\":\"" + profileId + "\"}}}";

        httpPost.setEntity(new StringEntity(json));
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Intuit_IAM_Authentication intuit_appid=Intuit.identity.c360.cdcsppmetadatatestclient, intuit_app_secret=\""+appSecret+"\"");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader("intuit_originatingip", "127.0.0.1");

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        String responseBody = entity != null ? EntityUtils.toString(entity) : null;

        JsonObject jsonObject =  new Gson().fromJson(responseBody, JsonObject.class);
        JsonObject data = jsonObject.getAsJsonObject("data");
        JsonObject signIn = data.getAsJsonObject("identitySignInInternalApplicationWithPrivateAuth");
        return signIn.get("authorizationHeader").getAsString();
    }

    public List<PipelineMetricsPojo> getHealthyPipelineMetadataApi(String authHeader, String appSecret, List<String> filterCriteria)
        throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        Map<String, String> activePipelines = new HashMap<>();
        List<PipelineMetricsPojo> pipelineMetricsPojoList = new ArrayList<>();
        HttpGet httpGet = new HttpGet("https://cdc-spp-metadata-api-e2e.api.intuit.com/v1/metadata");
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Intuit_IAM_Authentication intuit_appid=Intuit.identity.c360.cdcsppmetadatatestclient, " +
            "intuit_app_secret="+appSecret+", intuit_token="+ authHeader);
        httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String responseBody = entity != null ? EntityUtils.toString(entity) : null;

        JsonObject jsonObject =  new Gson().fromJson(responseBody, JsonObject.class);
        JsonArray metadata = jsonObject.getAsJsonArray("metadata");
        for(JsonElement item: metadata) {
            JsonObject inJsonObject = item.getAsJsonObject();

            if(inJsonObject.get("prd")!=null
                && (inJsonObject.get("prd").getAsJsonObject().get("health_status").getAsString().equals("OK")
                || inJsonObject.get("prd").getAsJsonObject().get("health_status").getAsString().equals("WARNING"))) {
                String kubeName = inJsonObject.get("prd").getAsJsonObject().get("kubernetes_container").getAsString();
                String inputTopic = inJsonObject.get("prd").getAsJsonObject().get("input_topic").getAsString();
                PipelineMetricsPojo pipelineMetricsPojo = new PipelineMetricsPojo();
                if (inputTopic.contains("superglue") || inputTopic.contains("sandbox") || inputTopic.contains("test") || inputTopic.contains("test")) {
                    pipelineMetricsPojo.setTestPipeline(true);
                }
                if(inJsonObject.has("type_of_onboarding")) {
                    pipelineMetricsPojo.setOnBoardingType(inJsonObject.get("type_of_onboarding").getAsString());
                } else {
                    pipelineMetricsPojo.setOnBoardingType("N/A");
                }

                pipelineMetricsPojo.setPipelineName(inJsonObject.get("pipeline_name").getAsString());
                pipelineMetricsPojo.setPipelineTopic(inJsonObject.get("prd").getAsJsonObject().get("input_topic").getAsString());
                pipelineMetricsPojo.setCreateDateEpoch(inJsonObject.get("prd").getAsJsonObject().get("created_at").getAsLong());
                pipelineMetricsPojoList.add(pipelineMetricsPojo);

                activePipelines.put(inJsonObject.get("pipeline_name").getAsString(),
                    inJsonObject.get("prd").getAsJsonObject().get("input_topic").getAsString());
            }
        }
        return pipelineMetricsPojoList;

    }

    public List<PipelineMetricsPojo> populateWaveFrontMetrics(List<PipelineMetricsPojo> healthyPipelinesList, long epochStartTime, long epochEndTime,
                                                              String wavefront_token)
        throws IOException {

        HttpClient httpClient = HttpClients.createDefault();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime utc12 = now.with(ChronoField.HOUR_OF_DAY, 0)
            .with(ChronoField.MINUTE_OF_HOUR, 0)
            .with(ChronoField.SECOND_OF_MINUTE, 0)
            .with(ChronoField.NANO_OF_SECOND, 0);

        // Wavefront query window [s;e), to include end time adding +1 to epoch
        String epochEndTimeString = Long.toString(epochEndTime+1);
        String epochStartTimeString = Long.toString(epochStartTime);

        for(PipelineMetricsPojo pipeLine:healthyPipelinesList) {
            String url =
                "https://intuit.wavefront.com/api/v2/chart/api?q=rawsum(align(30s%2C%20ts(%22custom.spp."
                    +pipeLine.getPipelineName()+".*.kafka.records.read.processable%22%20and%20env%3Dprd)))&queryType=HYBRID&s="+epochStartTimeString+"&e="+epochEndTimeString+"&g=d&i=false" +
                    "&summarization=SUM&listMode=false&strict=true&view=METRIC&sorted=false&cached=true&useRawQK=true";
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpGet.setHeader("Authorization", "Bearer " + wavefront_token);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;
            // System.out.println("responseBody: " + responseBody);
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            pipeLine.setWavefrontUrl(WAVEFRONT_PREFIX+pipeLine.getPipelineName());
            pipeLine.setMetricsDateEventsCountMap(new TreeMap<>());
            if(jsonObject.has("timeseries")) {
                JsonArray timeSeries = jsonObject.getAsJsonArray("timeseries");
                JsonObject firstEle = timeSeries.get(0).getAsJsonObject();
                JsonArray dataArray = firstEle.get("data").getAsJsonArray();
                for (JsonElement jsonElement: dataArray) {
                    // Create a Date object from the epoch timestamp
                    long epochTime = jsonElement.getAsJsonArray().get(0).getAsLong();
                    String dateOfEvents = HelperUtil.convertToMMddYYYY(epochTime);
                    pipeLine.getMetricsDateEventsCountMap().put(dateOfEvents, jsonElement.getAsJsonArray().get(1).getAsLong());
                }
            }
        }
        return healthyPipelinesList;
    }


    public void pushToSlackUsingWebHook(List<PipelineMetricsPojo> allPipelines, List<PipelineMetricsPojo> anomalyPipelines,
                                        String endDate, String webHook) throws IOException {
        String slackPayload = createPayload(allPipelines, anomalyPipelines, endDate);
        HttpClient httpClient = HttpClients.createDefault();;
        HttpPost httpPost = new HttpPost(webHook);
        StringEntity stringEntity = new StringEntity(slackPayload, ContentType.DEFAULT_TEXT);
        httpPost.setEntity(stringEntity);

        HttpEntity responseEntity = httpClient.execute(httpPost).getEntity();
        String response = EntityUtils.toString(responseEntity);
        System.out.println("Slack response Generated " + response);
    }

    private static String createPayload(List<PipelineMetricsPojo> allPipelines,
                                        List<PipelineMetricsPojo> anomalyPipelines,
                                        String endDate) throws JsonProcessingException {
        List<String> realTimeAnomalyPipelinesList = new ArrayList<>();
        String realTimeAnomalyPipelines  = "";
        int realTimeAnomalyCount = 0;

        List<String> batchAnomalyPipelinesList = new ArrayList<>();
        String batchAnomalyPipelines = "";
        int batchAnomalyCount = 0;

        List<String> undefinedPipelinesList = new ArrayList<>();
        String undefinedPipelines = "";
        int undefinedAnomalyCount = 0;

        List<String> testPipelinesList = new ArrayList<>();
        String testPipelines = "";
        int testPipelineCount = 0;

        for(PipelineMetricsPojo pipelineMetricsPojo: anomalyPipelines) {
            if (pipelineMetricsPojo.isTestPipeline()) {
                if (testPipelineCount%5 == 0) {
                    testPipelinesList.add(testPipelines);
                    testPipelines = "";
                }
                testPipelines = testPipelines + pipelineMetricsPojo.getPipelineTopic() + LINE_BREAK;
                testPipelineCount++;
            }
            else if (pipelineMetricsPojo.getOnBoardingType().equals(REAL_TIME_STRING)) {
                if (realTimeAnomalyCount%5 == 0) {
                    realTimeAnomalyPipelinesList.add(realTimeAnomalyPipelines);
                    realTimeAnomalyPipelines = "";
                }
                realTimeAnomalyPipelines = realTimeAnomalyPipelines + pipelineMetricsPojo.toSlackString() + LINE_BREAK;
                realTimeAnomalyCount++;
            } else if (pipelineMetricsPojo.getOnBoardingType().equals(BATCH_STRING)) {
                if (batchAnomalyCount%5 == 0) {
                    batchAnomalyPipelinesList.add(batchAnomalyPipelines);
                    batchAnomalyPipelines = "";
                }
                batchAnomalyPipelines = batchAnomalyPipelines + pipelineMetricsPojo.toSlackString() + LINE_BREAK;
                batchAnomalyCount++;
            } else {
                if (undefinedAnomalyCount%5 == 0) {
                    undefinedPipelinesList.add(undefinedPipelines);
                    undefinedPipelines = "";
                }
                undefinedPipelines = undefinedPipelines + pipelineMetricsPojo.toSlackString() + LINE_BREAK;
                undefinedAnomalyCount++;
            }
        }
        if (testPipelines.length() > 0) {
            testPipelinesList.add(testPipelines);
        }
        if (realTimeAnomalyPipelines.length() > 0) {
            realTimeAnomalyPipelinesList.add(realTimeAnomalyPipelines);
        }
        if (batchAnomalyPipelines.length() > 0) {
            batchAnomalyPipelinesList.add(batchAnomalyPipelines);
        }
        if (undefinedPipelines.length() > 0) {
            undefinedPipelinesList.add(undefinedPipelines);
        }

        int healthyAndReceivingTrafficPipelinesCount = allPipelines.size()
            - realTimeAnomalyCount - batchAnomalyCount - undefinedAnomalyCount -testPipelineCount;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", "Report Bot");
        payload.put("icon_emoji", ":sparkles:");
        payload.put("text", "<!subteam^S014TKE37UM> Pipeline *Anomaly Report* for "+endDate+ " 4:00 PM PST/5:00 PM PDT. NOTE: As this report is long, please check for non-datamap realtime topics");
        ArrayNode attachments = payload.putArray("attachments");

        ObjectNode greenAttachment = attachments.addObject();
        greenAttachment.put("color", "#008000");
        ArrayNode greenFields = greenAttachment.putArray("fields");

        greenFields.addObject()
            .put("title", "Total pipelines")
            .put("value", "Healthy Status and receiving traffic: "+healthyAndReceivingTrafficPipelinesCount + LINE_BREAK +
            "Real Time Anomaly Count: "+ realTimeAnomalyCount + LINE_BREAK +
            "Batch Anomaly Count: " + batchAnomalyCount + LINE_BREAK +
            "Undefined pipeline Anomaly Count: " + undefinedAnomalyCount + LINE_BREAK +
            "Test pipeline Count: " + testPipelineCount
                )
            .put("short", false);

        ObjectNode redAttachment = attachments.addObject();
        redAttachment.put("color", "#ff0000");
        ArrayNode redFields = redAttachment.putArray("fields");

        redFields.addObject()
            .put("title", "Real Time Pipelines (On-boarding more than 30 days ago, No traffic today).")
            .put("short", false);
        if(realTimeAnomalyPipelinesList.size()>0) {
            for (String realTimeAnomalyPipeline:realTimeAnomalyPipelinesList) {
                redFields.addObject()
                    .put("value", realTimeAnomalyPipeline)
                    .put("short", false);
            }
        }

        ObjectNode orangeAttachment = attachments.addObject();
        orangeAttachment.put("color", "#ffa500");
        ArrayNode orangeFields = orangeAttachment.putArray("fields");

        orangeFields.addObject()
            .put("title", "Batch Pipelines (On-boarding more than 30 days ago, No traffic last 7 days)")
            .put("short", false);

        if(batchAnomalyPipelinesList.size()>0) {
            for (String batchAnomalyPipeline:batchAnomalyPipelinesList) {
                orangeFields.addObject()
                    .put("value", batchAnomalyPipeline)
                    .put("short", false);
            }
        }

        ObjectNode yellowAttachment = attachments.addObject();
        yellowAttachment.put("color", "#ffff00");
        ArrayNode yellowFields = yellowAttachment.putArray("fields");

        yellowFields.addObject()
            .put("title", "Undefined pipelines")
            .put("short", false);

        if(undefinedPipelinesList.size()>0) {
            for (String undefinedPipeline:undefinedPipelinesList) {
                yellowFields.addObject()
                    .put("value", undefinedPipeline)
                    .put("short", false);
            }
        }

        yellowFields.addObject()
            .put("title", "Test pipelines")
            .put("short", false);

        if(testPipelinesList.size()>0) {
            for (String testPipeline:testPipelinesList) {
                yellowFields.addObject()
                    .put("value", testPipeline)
                    .put("short", false);
            }
        }

        return mapper.writeValueAsString(payload);
    }

    public void pushToSlackUsingWebHook(String msg, String webHook) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", "Report Bot");
        payload.put("icon_emoji", ":sparkles:");
        payload.put("text", "<!subteam^S014TKE37UM> "+msg);

        HttpClient httpClient = HttpClients.createDefault();;
        HttpPost httpPost = new HttpPost(webHook);
        StringEntity stringEntity = new StringEntity(mapper.writeValueAsString(payload),
            ContentType.DEFAULT_TEXT);
        httpPost.setEntity(stringEntity);

        HttpEntity responseEntity = httpClient.execute(httpPost).getEntity();
        String response = EntityUtils.toString(responseEntity);
    }
}


