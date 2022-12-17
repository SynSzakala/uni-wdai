FROM adoptopenjdk/openjdk14

COPY api-0.0.1-SNAPSHOT.jar api.jar

ENTRYPOINT java -jar api.jar \
    -Duni.wdai.s3.input_bucket=$S3_INPUT_BUCKET \
    -Duni.wdai.s3.output_bucket=$S3_OUTPUT_BUCKET \
    -Duni.wdai.sqs.start_queue=$SQS_QUEUE