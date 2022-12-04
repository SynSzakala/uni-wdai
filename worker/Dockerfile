FROM adoptopenjdk/openjdk14

RUN apt -y update && apt install -y ffmpeg

COPY build/libs/worker-0.0.1-SNAPSHOT.jar worker.jar

ENTRYPOINT java -jar worker.jar \
    -Duni.wdai.s3.input_bucket=$S3_INPUT_BUCKET \
    -Duni.wdai.s3.output_bucket=$S3_OUTPUT_BUCKET \
    -Duni.wdai.sqs.start_queue=$SQS_QUEUE