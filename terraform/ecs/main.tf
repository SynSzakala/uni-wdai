terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

data "aws_caller_identity" "current" {}

resource "aws_ecs_cluster" "main" {
  name = "converter"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "main" {
  name = "converter-logs"
}

resource "aws_ecs_task_definition" "api" {
  family                   = "api"
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ecsTaskExecutionRole"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([
    {
      name      = "api"
      image     = var.api_ecr_url
      essential = true
      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
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
    },
  ])
  cpu    = 256
  memory = 512
}

resource "aws_lb_target_group" "api" {
  name_prefix = "api"
  port        = 80
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  /* health_check {
    healthy_threshold   = "3"
    interval            = "300"
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = "3"
    path                = "/v1/status"
    unhealthy_threshold = "2"
  } */
}

resource "aws_security_group" "loadbalancer" {
  name_prefix = "loadbalancer"
  vpc_id      = var.vpc_id

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
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port       = 0
    to_port         = 80
    protocol        = "TCP"
    security_groups = [aws_security_group.loadbalancer.id]
  }
}

resource "aws_alb" "api" {
  name               = "api"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.loadbalancer.id]
  subnets            = var.loadbalancer_subnet_ids
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
  name                  = "api"
  cluster               = aws_ecs_cluster.main.id
  task_definition       = aws_ecs_task_definition.api.arn
  launch_type           = "FARGATE"
  scheduling_strategy   = "REPLICA"
  desired_count         = 3
  force_new_deployment  = true
  wait_for_steady_state = true

  network_configuration {
    subnets         = var.container_subnet_ids
    security_groups = [aws_security_group.service.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    # elb_name       = aws_alb.api.id
    container_name = "api"
    container_port = 80
  }

  lifecycle {
    ignore_changes = [
      # Allow for autoscaling
      desired_count
    ]
  }

  depends_on = [
    aws_alb_listener.api
  ]
}
