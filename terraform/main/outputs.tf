output "converter_url" {
  value = aws_alb.api.dns_name
}

output "auth_admin_secret_key" {
  value = random_id.auth_admin_secret_key.hex
}
