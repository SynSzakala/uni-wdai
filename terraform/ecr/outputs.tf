output "api_repository_url" {
  value = aws_ecr_repository.api.repository_url
}

output "converter_repository_url" {
  value = aws_ecr_repository.converter.repository_url
}
