terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

data "aws_caller_identity" "current" {}

provider "aws" {
  region = "eu-central-1"
}

resource "aws_vpc" "vpc" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_s3_bucket" "upload" {
  bucket_prefix = "converter"
}

resource "aws_s3_bucket_ownership_controls" "upload_bucket" {
  bucket = aws_s3_bucket.upload.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "upload_bucket" {
  bucket = aws_s3_bucket.upload.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_sqs_queue" "upload" {
  name_prefix = "converter"
}

resource "aws_sqs_queue_policy" "upload" {
  queue_url = aws_sqs_queue.upload.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Principal" : {
          "AWS" : "*"
        },
        "Action" : "SQS:SendMessage",
        "Resource" : aws_sqs_queue.upload.arn,
        "Condition" : {
          "ArnLike" : {
            "aws:SourceArn" : aws_s3_bucket.upload.arn
          }
        }
      }
    ]
  })
}

resource "aws_s3_bucket_notification" "upload_notification" {
  bucket = aws_s3_bucket.upload.id

  queue {
    queue_arn = aws_sqs_queue.upload.arn
    events    = ["s3:ObjectCreated:*"]
  }
}

