variable "api_ecr_url" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "loadbalancer_subnet_ids" {
  type = list(string)
}

variable "container_subnet_ids" {
  type = list(string)
}
