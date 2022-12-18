resource "aws_iam_role" "worker_task_role" {
  name_prefix = "worker_task_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = ["ec2.amazonaws.com",
            "ecs.amazonaws.com",
          "ecs-tasks.amazonaws.com"]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy" "worker_task_role_policy" {
  name_prefix = "worker_task_role_policy"
  role        = aws_iam_role.worker_task_role.id

  # todo
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Sid" : "",
        "Action" : "*",
        "Effect" : "Allow",
        "Resource" : "*"
      }
    ]
  })
}



resource "aws_iam_role_policy" "worker_task_execution_role_policy" {
  name_prefix = "worker_task_execution_role_policy"
  role        = aws_iam_role.worker_task_execution_role.id

  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : ["secretsmanager:GetSecretValue"],
        "Effect" : "Allow",
        "Resource" : [aws_secretsmanager_secret_version.mongo_password.arn]
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        "Resource" : "*"
      }
    ]
  })
}

resource "aws_iam_role" "worker_task_execution_role" {
  name_prefix = "worker_task_execution_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = ["ecs-tasks.amazonaws.com"]
        }
      }
    ]
  })
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "worker"
  execution_role_arn       = aws_iam_role.worker_task_execution_role.arn
  task_role_arn            = aws_iam_role.worker_task_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([
    {
      name      = "worker"
      image     = var.converter_repository_url
      essential = true
      #   portMappings = [
      #     {
      #       containerPort = 8080
      #       hostPort      = 8080
      #     }
      #   ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "${aws_cloudwatch_log_group.main.id}",
          awslogs-region        = "eu-central-1",
          awslogs-stream-prefix = "worker"
        }
      }
      environment = [
        {
          name  = "MONGODB_HOST"
          value = aws_docdb_cluster.mongo.endpoint
        },
        {
          name  = "MONGODB_PORT"
          value = tostring(aws_docdb_cluster.mongo.port)
        },
        {
          name  = "MONGODB_DATABASE"
          value = "converter"
        },
        {
          name  = "MONGODB_USERNAME"
          value = aws_docdb_cluster.mongo.master_username
        },
        {
          name  = "S3_INPUT_BUCKET"
          value = aws_s3_bucket.input.bucket
        },
        {
          name  = "S3_OUTPUT_BUCKET"
          value = aws_s3_bucket.output.bucket
        },
        {
          name  = "SQS_QUEUE"
          value = aws_sqs_queue.input.url
        },
        {
          name  = "MONGODB_REPLICA_SET",
          value = "rs0"
        }
      ]
      secrets = [
        {
          name      = "MONGODB_PASSWORD"
          valueFrom = aws_secretsmanager_secret_version.mongo_password.arn
        },
      ]
    },
  ])
  cpu    = 512
  memory = 1024
}

resource "aws_security_group" "worker" {
  name_prefix = "worker"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_ecs_service" "worker" {
  name                 = "worker"
  cluster              = aws_ecs_cluster.main.id
  task_definition      = aws_ecs_task_definition.worker.arn
  launch_type          = "FARGATE"
  scheduling_strategy  = "REPLICA"
  desired_count        = 3
  force_new_deployment = true
  # wait_for_steady_state = true

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.worker.id]
  }

  lifecycle {
    ignore_changes = [
      # Allow for autoscaling
      desired_count
    ]
  }

  depends_on = [
    aws_docdb_cluster_instance.mongo,
    aws_iam_role_policy.worker_task_role_policy,
    aws_iam_role_policy.worker_task_execution_role_policy
  ]
}
