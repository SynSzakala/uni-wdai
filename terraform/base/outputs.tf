output "api_ecr_url" {
  value = aws_ecr_repository.api.repository_url
}

output "vpc_id" {
  value = aws_vpc.main.id
}

output "mongodb_username" {
  value = aws_docdb_cluster.mongo.master_username
}

output "mongodb_password" {
  value     = aws_docdb_cluster.mongo.master_password
  sensitive = true
}

output "mongodb_url" {
  value = aws_docdb_cluster.mongo.endpoint
}

output "public_subnets" {
  value = aws_subnet.public[*].id
}
