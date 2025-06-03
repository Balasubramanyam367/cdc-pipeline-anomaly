# cdc-pipeline-anomaly
Generates daily anamoly report for CDC pipelines(pipelines, that are not receing traffic

Jar is created and uploaded to s3://profilev2-qal-artifacts/onboarding-tools/pipelineanomaly-1.0-SNAPSHOT-jar-with-dependencies-anomalyV1.jar

Jenkins job configured to run daily, in case of failures. please run this manually. 
https://build.intuit.com/profile-2/job/profile-ingestion/job/build/job/c360-self-serve/job/InfraC360_Pipeline_Anomaly/

Secrets uploaded here: 
https://build.intuit.com/profile-2/manage/credentials/store/system/domain/_/credential/anomaly_prd/

