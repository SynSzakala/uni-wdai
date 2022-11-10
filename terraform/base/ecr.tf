resource "aws_ecr_repository" "api" {
  name                 = "api"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "converter_ecr_repository" "api" {
  name                 = "api"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}
