package org.example;

public class StringConstants {
    public static String IDENTITY_URL = "https://identityinternal-e2e.api.intuit.com/v1/graphql";

    public static String METADATA_URL = "https://cdc-spp-metadata-api-e2e.api.intuit.com/v1/metadata";

    public static String AUTH_QUERY = "mutation identitySignInInternalApplicationWithPrivateAuth($input: Identity_SignInApplicationWithPrivateAuthInput!) {identitySignInInternalApplicationWithPrivateAuth(input: $input) {authorizationHeader}}";
    public static String WAVEFRONT_PREFIX = "https://intuit.wavefront.com/dashboards/spp-";

    public static String JENKINS_JOB_URL = "https://build.intuit.com/profile-2/job/profile-ingestion/job/build/job/c360-self-serve/job/InfraC360_Pipeline_Anomaly/";
    public static int NUMBER_OF_SECONDS_DAY =  86400;

    public static final String REAL_TIME_STRING = "STREAMING";

    public static final String BATCH_STRING = "BATCH";

    public static final String LINE_BREAK = "\n";


}

