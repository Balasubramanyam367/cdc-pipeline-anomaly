package org.example;

import static org.example.StringConstants.JENKINS_JOB_URL;
import static org.example.StringConstants.NUMBER_OF_SECONDS_DAY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Main {

    private static long epochEndTime;

    private static String endDate;

    private static long epochStartTime;

    private static String startDate;



    public static void main(String[] args) throws IOException {
        AnomalyDetector anomalyDetector = null;
        String amiWebHook = null;
        String ingestor_slack_hook = null;
        try {
            Properties props = initProperties();
            String profileId = props.getProperty("profileId");
            String appSecret = props.getProperty("intuit_app_secret");
            amiWebHook = props.getProperty("ami_slack_hook");
            ingestor_slack_hook = props.getProperty("ingestor_slack_hook");
            String wavefront_token = props.getProperty("wavefront_token");
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime utc12 = now.with(ChronoField.HOUR_OF_DAY, 0)
                .with(ChronoField.MINUTE_OF_HOUR, 0)
                .with(ChronoField.SECOND_OF_MINUTE, 0)
                .with(ChronoField.NANO_OF_SECOND, 0);
            // job has to run after 5PM est PDT and 4PM est PST
            epochEndTime = utc12.minusDays(1).toEpochSecond(ZoneOffset.UTC);
            epochStartTime = epochEndTime - ((NUMBER_OF_SECONDS_DAY) * 7);

            endDate = HelperUtil.convertToMMddYYYY(epochEndTime);
            anomalyDetector = new AnomalyDetector();
            String authHeader = anomalyDetector.getIdentityAuthHeader(profileId, appSecret);
            // Get Healthy and undefined pipelines
            List<PipelineMetricsPojo> healthyPipelinesList =
                anomalyDetector.getHealthyPipelineMetadataApi(authHeader, appSecret, null);
            // populating the wavefront metrics
            List<PipelineMetricsPojo> healthyPipelinesListWithTrafficInfo =
                anomalyDetector.populateWaveFrontMetrics(healthyPipelinesList, epochStartTime, epochEndTime,
                    wavefront_token);
            // method to identify anomalies
            List<PipelineMetricsPojo> anomaliesPipelines =
                anomalyDetector.identifyAnomalies(healthyPipelinesListWithTrafficInfo, epochStartTime, epochEndTime);
            anomaliesPipelines.forEach(System.out::println);
            if (anomaliesPipelines.size() > 0) {
                anomalyDetector.pushToSlackUsingWebHook(healthyPipelinesListWithTrafficInfo, anomaliesPipelines,
                    endDate, ingestor_slack_hook);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            anomalyDetector.pushToSlackUsingWebHook(
                "Anomaly report creation Jenkins job failed. Please check the jenkins logs, if auth error, rerun " +
                    JENKINS_JOB_URL, ingestor_slack_hook);
        }

    }

    public static Object readAnomalyConfigJSON() throws Exception {
        String anomalyConfigPath = "anomaly_config.json";
        File file = new File(anomalyConfigPath);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return mapper.readTree(file);
        } else {
            throw new Exception(anomalyConfigPath + " Config file not found");
        }
    }

    private static Properties initProperties()
        throws IOException, FileNotFoundException {
        // Read properties file
        String propertyPath = "anomaly_prd.properties";
        Properties props = new Properties();
        props.load(new FileInputStream(propertyPath));

        return props;
    }
}

