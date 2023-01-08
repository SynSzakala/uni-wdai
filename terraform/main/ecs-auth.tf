resource "aws_secretsmanager_secret" "auth_secret_key" {
  name_prefix = "authsecretkey"
}

resource "random_id" "auth_secret_key" {
  byte_length = 32
}

resource "aws_secretsmanager_secret_version" "auth_secret_key" {
  secret_id     = aws_secretsmanager_secret.auth_secret_key.id
  secret_string = random_id.auth_secret_key.hex
}

resource "aws_secretsmanager_secret" "auth_admin_secret_key" {
  name_prefix = "authadminsecretkey"
}

resource "random_id" "auth_admin_secret_key" {
  byte_length = 16
}

resource "aws_secretsmanager_secret_version" "auth_admin_secret_key" {
  secret_id     = aws_secretsmanager_secret.auth_admin_secret_key.id
  secret_string = random_id.auth_admin_secret_key.hex
}

resource "aws_iam_role_policy" "auth_task_role_policy" {
  name_prefix = "auth_task_role_policy"
  role        = aws_iam_role.auth_task_role.id

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

resource "aws_iam_role" "auth_task_role" {
  name_prefix = "auth_task_role"

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

resource "aws_iam_role_policy" "auth_task_execution_role_policy" {
  name_prefix = "auth_task_role_policy"
  role        = aws_iam_role.auth_task_execution_role.id

  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : ["secretsmanager:GetSecretValue"],
        "Effect" : "Allow",
        "Resource" : [aws_secretsmanager_secret_version.mongo_password.arn]
      },
      {
        "Action" : ["secretsmanager:GetSecretValue"],
        "Effect" : "Allow",
        "Resource" : [aws_secretsmanager_secret_version.auth_secret_key.arn]
      },
      {
        "Action" : ["secretsmanager:GetSecretValue"],
        "Effect" : "Allow",
        "Resource" : [aws_secretsmanager_secret_version.auth_admin_secret_key.arn]
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

resource "aws_iam_role" "auth_task_execution_role" {
  name_prefix = "auth_task_execution_role"

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

resource "aws_ecs_task_definition" "auth" {
  family                   = "auth"
  execution_role_arn       = aws_iam_role.auth_task_execution_role.arn
  task_role_arn            = aws_iam_role.auth_task_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([
    {
      name      = "auth"
      image     = var.auth_repository_url
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
          awslogs-stream-prefix = "auth"
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
          name  = "MONGODB_REPLICA_SET",
          value = "rs0"
        }
      ]
      secrets = [
        {
          name      = "MONGODB_PASSWORD"
          valueFrom = aws_secretsmanager_secret_version.mongo_password.arn
        },
        {
          name      = "AUTH_SECRET_KEY"
          valueFrom = aws_secretsmanager_secret_version.auth_secret_key.arn
        },
        {
          name      = "AUTH_ADMIN_SECRET_KEY"
          valueFrom = aws_secretsmanager_secret_version.auth_admin_secret_key.arn
        },
      ]
    },
  ])
  cpu    = 512
  memory = 1024
}

resource "aws_lb_target_group" "auth" {
  name_prefix = "auth"
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
    timeout             = "8"
    path                = "/actuator/health"
    unhealthy_threshold = "2"
  }
}

resource "aws_lb_listener_rule" "auth_user" {
  listener_arn = aws_alb_listener.api.arn
  priority     = 99

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.auth.id
  }

  condition {
    path_pattern {
      values = ["/user*"]
    }
  }
}

resource "aws_lb_listener_rule" "auth_auth" {
  listener_arn = aws_alb_listener.api.arn
  priority     = 98

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.auth.id
  }

  condition {
    path_pattern {
      values = ["/auth*"]
    }
  }
}

resource "aws_security_group" "auth_service" {
  name_prefix = "auth_service"
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

resource "aws_ecs_service" "auth" {
  name                 = "auth"
  cluster              = aws_ecs_cluster.main.id
  task_definition      = aws_ecs_task_definition.auth.arn
  launch_type          = "FARGATE"
  scheduling_strategy  = "REPLICA"
  desired_count        = 2
  force_new_deployment = true
  # wait_for_steady_state = true

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.auth_service.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.auth.arn
    # elb_name       = aws_alb.auth.id
    container_name = "auth"
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
    aws_iam_role_policy.auth_task_role_policy,
    aws_iam_role_policy.auth_task_execution_role_policy
  ]
}
