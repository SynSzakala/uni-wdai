resource "aws_s3_bucket" "input" {
  bucket_prefix = "converter-input"
  force_destroy = true
}

resource "aws_s3_bucket_cors_configuration" "input" {
  bucket = aws_s3_bucket.input.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT", "POST", "GET"]
    allowed_origins = ["*"]
  }
}

resource "aws_s3_bucket_ownership_controls" "input_bucket" {
  bucket = aws_s3_bucket.input.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "input_bucket" {
  bucket = aws_s3_bucket.input.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_sqs_queue" "input" {
  name_prefix = "converter-input"
}

resource "aws_sqs_queue_policy" "input" {
  queue_url = aws_sqs_queue.input.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Principal" : {
          "AWS" : "*"
        },
        "Action" : "SQS:SendMessage",
        "Resource" : aws_sqs_queue.input.arn,
        "Condition" : {
          "ArnLike" : {
            "aws:SourceArn" : aws_s3_bucket.input.arn
          }
        }
      }
    ]
  })
}

resource "aws_s3_bucket" "output" {
  bucket_prefix = "converter-output"
  force_destroy = true
}

resource "aws_s3_bucket_cors_configuration" "output" {
  bucket = aws_s3_bucket.output.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT", "POST", "GET"]
    allowed_origins = ["*"]
  }
}

resource "aws_s3_bucket_ownership_controls" "output_bucket" {
  bucket = aws_s3_bucket.output.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "output_bucket" {
  bucket = aws_s3_bucket.output.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
