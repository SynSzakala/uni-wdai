terraform -chdir=ecr apply -auto-approve
API_REPOSITORY_URL=$(terraform -chdir=ecr output -json | jq -r ".api_repository_url.value")
CONVERTER_REPOSITORY_URL=$(terraform -chdir=ecr output -json | jq -r ".converter_repository_url.value")

aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $API_REPOSITORY_URL
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $CONVERTER_REPOSITORY_URL

# build the api image
# TODO -- PLACEHOLDER
docker pull containous/whoami
docker tag containous/whoami $API_REPOSITORY_URL
docker push $API_REPOSITORY_URL

# build the converter image
# TODO -- PLACEHOLDER
docker pull containous/whoami
docker tag containous/whoami $CONVERTER_REPOSITORY_URL
docker push $CONVERTER_REPOSITORY_URL

terraform -chdir=main apply -auto-approve -var="api_repository_url=$API_REPOSITORY_URL" -var="converter_repository_url=$CONVERTER_REPOSITORY_URL"