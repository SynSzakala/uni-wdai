terraform -chdir=base apply -auto-approve
ECR_API_REGISTRY=$(terraform -chdir=base output -json | jq -r ".api_ecr_url.value")
VPC_ID=$(terraform -chdir=base output -json | jq -r ".vpc_id.value")

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $ECR_API_REGISTRY

# build images
# TODO -- PLACEHOLDER
docker pull containous/whoami
docker tag containous/whoami $ECR_API_REGISTRY
docker push $ECR_API_REGISTRY


