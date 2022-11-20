output "input_bucket_name" {
  value = aws_s3_bucket.input.bucket
}

output "output_bucket_name" {
  value = aws_s3_bucket.output.bucket
}

output "output_queue_name" {
  value = aws_sqs_queue.input.name
}

output "output_queue_url" {
  value = aws_sqs_queue.input.url
}
