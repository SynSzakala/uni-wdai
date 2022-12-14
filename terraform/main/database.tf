resource "aws_secretsmanager_secret" "mongo_password" {
  name_prefix = "mongopwd"
}

resource "random_password" "password" {
  length  = 16
  special = false
}

resource "aws_secretsmanager_secret_version" "mongo_password" {
  secret_id     = aws_secretsmanager_secret.mongo_password.id
  secret_string = random_password.password.result
}

resource "aws_docdb_subnet_group" "mongo" {
  name_prefix = "mongo"
  subnet_ids  = aws_subnet.private[*].id
}

resource "aws_docdb_cluster_parameter_group" "mongo" {
  name_prefix = "mongoconverter"
  family      = "docdb4.0"

  parameter {
    name  = "tls"
    value = "disabled"
  }
}

resource "aws_docdb_cluster" "mongo" {
  master_username                 = "root"
  master_password                 = aws_secretsmanager_secret_version.mongo_password.secret_string
  apply_immediately               = true
  deletion_protection             = false
  skip_final_snapshot             = true
  db_subnet_group_name            = aws_docdb_subnet_group.mongo.name
  db_cluster_parameter_group_name = aws_docdb_cluster_parameter_group.mongo.name
  vpc_security_group_ids          = [aws_security_group.mongo.id]
}

resource "aws_docdb_cluster_instance" "mongo" {
  identifier_prefix  = "mongo-instance"
  cluster_identifier = aws_docdb_cluster.mongo.id
  instance_class     = "db.t3.medium"
}

resource "aws_security_group" "mongo" {
  name_prefix = "mongo"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port       = 0
    to_port         = 27017
    protocol        = "TCP"
    security_groups = [aws_security_group.service.id, aws_security_group.auth_service.id, aws_security_group.worker.id]
  }
}
