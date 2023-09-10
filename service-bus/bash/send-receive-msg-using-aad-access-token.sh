# Script Name: send-receive-msg-using-aad-access-token.sh

#Inputs
TENANT_ID=EnterTenantId
CLIENT_ID=EnterClientId
CLIENT_SECRET=EnterClientSecret
RESOURCE_URI=https://EnterNamespaceName.servicebus.windows.net/
TOPIC_NAME=EnterTopicName
SUBSCRIPTION_NAME=EnterSubscriptionName

#Internal Config
GREEN='\033[0;32m'
RESET='\033[0m'

#Get AAD Access token
printf "\n${GREEN}Get AAD access token${RESET}\n"
jsonAccessToken=$(curl --silent --verbose --location "https://login.microsoftonline.com/$TENANT_ID/oauth2/token" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "scope=https://graph.microsoft.com/.default" \
    --data-urlencode "resource=https://servicebus.azure.net" \
    --data-urlencode "client_id=$CLIENT_ID" \
    --data-urlencode "client_secret=$CLIENT_SECRET")
ACCESS_TOKEN=$(echo $jsonAccessToken | jq -r ' .access_token')

#Send message
printf "\n${GREEN}***Send message to service bus***${RESET}\n"
curl --silent --verbose --location --request POST "${RESOURCE_URI}${TOPIC_NAME}/messages" \
    --header "Authorization: $ACCESS_TOKEN" \
    --header "BrokerProperties: {"Header1":"Value1","Header2":"Value2","Header3":"Value3"}" \
    --header "Content-Type: application/json" \
    --data '{
    "What its doing": "Sending msg to service bus"
    }'

#Receive message (Using PEEK LOCK)
printf "\n${GREEN}***Get message from service bus***${RESET}\n"
curl --silent --verbose --location --request POST "${RESOURCE_URI}${TOPIC_NAME}/subscriptions/${SUBSCRIPTION_NAME}/messages/head" \
    --header "Authorization: $ACCESS_TOKEN" \
    --header "Content-Length: 0"