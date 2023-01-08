set -xe

terraform -chdir=ecr plan -out=tfplan
terraform -chdir=ecr apply tfplan

API_REPOSITORY_URL=$(terraform -chdir=ecr output -json | jq -r ".api_repository_url.value")
CONVERTER_REPOSITORY_URL=$(terraform -chdir=ecr output -json | jq -r ".converter_repository_url.value")
AUTH_REPOSITORY_URL=$(terraform -chdir=ecr output -json | jq -r ".auth_repository_url.value")

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $API_REPOSITORY_URL
docker build -f ../Dockerfile-api -t $API_REPOSITORY_URL ..
docker push $API_REPOSITORY_URL

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $CONVERTER_REPOSITORY_URL
docker build -f ../Dockerfile-worker -t $CONVERTER_REPOSITORY_URL ..
docker push $CONVERTER_REPOSITORY_URL

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $AUTH_REPOSITORY_URL
docker build -f ../Dockerfile-auth -t $AUTH_REPOSITORY_URL ..
docker push $AUTH_REPOSITORY_URL

terraform -chdir=main plan -var="api_repository_url=$API_REPOSITORY_URL" -var="converter_repository_url=$CONVERTER_REPOSITORY_URL" -var="auth_repository_url=$AUTH_REPOSITORY_URL" -out=tfplan
terraform -chdir=main apply tfplan