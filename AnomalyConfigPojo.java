package org.example;

import java.util.List;

public class AnomalyConfigPojo {
    public String alertName;
    public List<String> filterCriteriaList;
    public String wavefrontQuery;
    public boolean useGenAi;

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public List<String> getFilterCriteriaList() {
        return filterCriteriaList;
    }

    public void setFilterCriteriaList(List<String> filterCriteriaList) {
        this.filterCriteriaList = filterCriteriaList;
    }

    public String getWavefrontQuery() {
        return wavefrontQuery;
    }

    public void setWavefrontQuery(String wavefrontQuery) {
        this.wavefrontQuery = wavefrontQuery;
    }

    public boolean isUseGenAi() {
        return useGenAi;
    }

    public void setUseGenAi(boolean useGenAi) {
        this.useGenAi = useGenAi;
    }
}

