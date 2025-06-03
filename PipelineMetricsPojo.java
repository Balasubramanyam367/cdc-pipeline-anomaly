package org.example;

import java.util.List;
import java.util.Map;

public class PipelineMetricsPojo {

    private String pipelineName;
    private String pipelineTopic;
    private String onBoardingType;
    private boolean isTestPipeline;
    private String wavefrontUrl;
    private String typeOfAnomaly;
    private long createDateEpoch;
    private Map<String, Long> metricsDateEventsCountMap;

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getPipelineTopic() {
        return pipelineTopic;
    }

    public void setPipelineTopic(String pipelineTopic) {
        this.pipelineTopic = pipelineTopic;
    }

    public String getOnBoardingType() {
        return onBoardingType;
    }

    public void setOnBoardingType(String onBoardingType) {
        this.onBoardingType = onBoardingType;
    }

    public boolean isTestPipeline() {
        return isTestPipeline;
    }

    public void setTestPipeline(boolean testPipeline) {
        isTestPipeline = testPipeline;
    }

    public String getWavefrontUrl() {
        return wavefrontUrl;
    }

    public void setWavefrontUrl(String wavefrontUrl) {
        this.wavefrontUrl = wavefrontUrl;
    }

    public String getTypeOfAnomaly() {
        return typeOfAnomaly;
    }

    public void setTypeOfAnomaly(String typeOfAnomaly) {
        this.typeOfAnomaly = typeOfAnomaly;
    }

    public Map<String, Long> getMetricsDateEventsCountMap() {
        return metricsDateEventsCountMap;
    }

    public void setMetricsDateEventsCountMap(Map<String, Long> metricsDateEventsCountMap) {
        this.metricsDateEventsCountMap = metricsDateEventsCountMap;
    }


    public long getCreateDateEpoch() {
        return createDateEpoch;
    }

    public void setCreateDateEpoch(long createDateEpoch) {
        this.createDateEpoch = createDateEpoch;
    }

    @Override
    public String toString() {
        return "PipelineMetricsPojo{" +
            "pipelineName='" + pipelineName + '\'' +
            ", pipelineTopic='" + pipelineTopic + '\'' +
            ", onBoardingType='" + onBoardingType + '\'' +
            ", isTestPipeline=" + isTestPipeline +
            ", wavefrontUrl='" + wavefrontUrl + '\'' +
            ", typeOfAnomaly='" + typeOfAnomaly + '\'' +
            ", createDateEpoch=" + createDateEpoch +
            ", metricsDateEventsCountMap=" + metricsDateEventsCountMap +
            '}';
    }

    public String toSlackString() {
        return "["+pipelineTopic + ", " + wavefrontUrl + "]" ;
    }

}

