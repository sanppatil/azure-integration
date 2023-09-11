# Script Name: create-blob-using-aad-access-token.sh

#Inputs
TENANT_ID=EnterTenantId
CLIENT_ID=EnterClientId
CLIENT_SECRET=EnterClientSecret
BLOB_STORAGE_ACCOUNT=https://EnterStorageAccountName.blob.core.windows.net/
BLOB_CONTAINER=EnterBLOBContainerName
BLOB_NAME=dummy-test-blob.txt

#Internal Config
GREEN='\033[0;32m'
RESET='\033[0m'

#Get AAD Access token
printf "\n${GREEN}Get AAD access token${RESET}\n"
jsonAccessToken=$(curl --silent --verbose --location --request GET "https://login.microsoftonline.com/$TENANT_ID/oauth2/token" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "scope=https://graph.microsoft.com/.default" \
    --data-urlencode "resource=$BLOB_STORAGE_ACCOUNT" \
    --data-urlencode "client_id=$CLIENT_ID" \
    --data-urlencode "client_secret=$CLIENT_SECRET")
ACCESS_TOKEN=$(echo $jsonAccessToken | jq -r ' .access_token')

#Upload Blob
printf "\n${GREEN}Uploading BLOB${RESET}\n"
curl --silent --verbose --location --request PUT "$BLOB_STORAGE_ACCOUNT$BLOB_CONTAINER/$BLOB_NAME" \
    --header "Authorization: Bearer $ACCESS_TOKEN" \
    --header 'x-ms-version: 2023-08-03' \
    --header 'x-ms-blob-type: BlockBlob' \
    --header 'Content-Type: text/plain' \
    --data 'Sample content of file..'

#Download Blob
printf "\n${GREEN}Downloading BLOB${RESET}\n"
curl --silent --verbose --location --request GET "$BLOB_STORAGE_ACCOUNT$BLOB_CONTAINER/$BLOB_NAME" \
    --header "Authorization: Bearer $ACCESS_TOKEN" \
    --header 'x-ms-version: 2023-08-03'