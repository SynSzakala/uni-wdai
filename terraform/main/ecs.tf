data "aws_caller_identity" "current" {}

resource "random_string" "cluster_postfix" {
  length  = 8
  special = false
}

resource "aws_ecs_cluster" "main" {
  name = join("_", ["converter", random_string.cluster_postfix.result])


  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "main" {
  name_prefix = "converter-logs"
}

resource "aws_iam_role_policy" "api_task_role_policy" {
  name_prefix = "api_task_role_policy"
  role        = aws_iam_role.api_task_role.id

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

resource "aws_iam_role" "api_task_role" {
  name_prefix = "api_task_role"

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

resource "aws_iam_role_policy" "api_task_execution_role_policy" {
  name_prefix = "api_task_role_policy"
  role        = aws_iam_role.api_task_execution_role.id

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

resource "aws_iam_role" "api_task_execution_role" {
  name_prefix = "api_task_execution_role"

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

resource "aws_ecs_task_definition" "api" {
  family                   = "api"
  execution_role_arn       = aws_iam_role.api_task_execution_role.arn
  task_role_arn            = aws_iam_role.api_task_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([
    {
      name      = "api"
      image     = var.api_repository_url
      essential = true
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "${aws_cloudwatch_log_group.main.id}",
          awslogs-region        = "eu-central-1",
          awslogs-stream-prefix = "api"
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

resource "aws_lb_target_group" "api" {
  name_prefix = "api"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  lifecycle {
    create_before_destroy = true
  }

  health_check {
    healthy_threshold   = "3"
    interval            = "300"
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = "3"
    path                = "/actuator/health"
    unhealthy_threshold = "2"
  }
}

resource "aws_security_group" "loadbalancer" {
  name_prefix = "loadbalancer"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 0
    to_port     = 80
    protocol    = "TCP"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "service" {
  name_prefix = "loadbalancer"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port       = 0
    to_port         = 8080
    protocol        = "TCP"
    security_groups = [aws_security_group.loadbalancer.id]
  }
}

resource "aws_alb" "api" {
  name               = "api"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.loadbalancer.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_alb_listener" "api" {
  load_balancer_arn = aws_alb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.id
  }
}

resource "aws_ecs_service" "api" {
  name                 = "api"
  cluster              = aws_ecs_cluster.main.id
  task_definition      = aws_ecs_task_definition.api.arn
  launch_type          = "FARGATE"
  scheduling_strategy  = "REPLICA"
  desired_count        = 3
  force_new_deployment = true
  # wait_for_steady_state = true

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.service.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    # elb_name       = aws_alb.api.id
    container_name = "api"
    container_port = 8080
  }

  lifecycle {
    ignore_changes = [
      # Allow for autoscaling
      desired_count
    ]
  }

  # triggers = {
  #   redeployment = timestamp()
  # }

  depends_on = [
    aws_alb_listener.api,
    aws_docdb_cluster_instance.mongo,
    aws_iam_role_policy.api_task_role_policy,
    aws_iam_role_policy.api_task_execution_role_policy
  ]
}
