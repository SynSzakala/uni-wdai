terraform -chdir=base apply -auto-approve
API_ECR_URL=$(terraform -chdir=base output -json | jq -r ".api_ecr_url.value")
CONVERTER_ECR_URL=$(terraform -chdir=base output -json | jq -r ".converter_ecr_url.value")
VPC_ID=$(terraform -chdir=base output -json | jq -r ".vpc_id.value")

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $API_ECR_URL
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $CONVERTER_ECR_URL

# build the api image
# TODO -- PLACEHOLDER
docker pull containous/whoami
docker tag containous/whoami $API_ECR_URL
docker push $API_ECR_URL

# build the ecr image
# TODO -- PLACEHOLDER
docker pull containous/whoami
docker tag containous/whoami $CONVERTER_ECR_URL
docker push $CONVERTER_ECR_URL
