variable "api_ecr_url" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "loadbalancer_subnet_ids" {
  type = list(string)
}

data "aws_caller_identity" "current" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

resource "aws_security_group" "allow_all" {
  name_prefix = "allowall"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_cluster" "main" {
  name = "converter"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
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

}

resource "aws_alb" "api" {
  name               = "api"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.allow_all.id]
  subnets            = var.loadbalancer_subnet_ids
}

resource "aws_alb_listener" "api" {
  load_balancer_arn = aws_alb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

resource "aws_ecs_service" "api" {
  name            = "api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 3

  load_balancer {
    elb_name       = aws_alb.api.name
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
