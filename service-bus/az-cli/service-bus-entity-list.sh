
SUBSCRIPTION=$1
RESOURCE_GROUP=$2
NAMESPACE=$3

echo "az account set --subscription '$SUBSCRIPTION'" | bash

function _getJsonValue() {
    echo ${1} | base64 --decode | jq -r ${2}
}

function printHeader {
    echo "EntityType|EntityName|AccessedAt|UpdatedAt|RequiresSession|ForwardTo" > results.csv
}

function printEntity {
    echo "$(_getJsonValue $1 '.type')|$(_getJsonValue $2 '.name')/$(_getJsonValue $1 '.name')|$(_getJsonValue $1 '.accessedAt')|$(_getJsonValue $1 '.updatedAt')|$(_getJsonValue $1 '.requiresSession')|$(_getJsonValue $1 '.forwardTo')" >> results.csv
}

echo "Export started..."
printHeader
jsonTopicList=$(az servicebus topic list --resource-group $RESOURCE_GROUP --namespace-name $NAMESPACE --output json)
for topic in $(echo "${jsonTopicList}" | jq -r '.[] | @base64'); 
do
    printEntity $topic
    topicName=$(_getJsonValue $topic '.name')
    if [ $(_getJsonValue $topic '.subscriptionCount') -ne "0" ]
    then
        jsonSubscriptionList=$(az servicebus topic subscription list --resource-group $RESOURCE_GROUP --namespace-name $NAMESPACE --topic-name $topicName --output json)
        for subscription in $(echo "${jsonSubscriptionList}" | jq -r '.[] | @base64'); 
        do
            printEntity $subscription $topic
        done
    fi
    echo -n "."
done
echo
jsonQueueList=$(az servicebus queue list --resource-group $RESOURCE_GROUP --namespace-name $NAMESPACE --output json)
for queue in $(echo "${jsonQueueList}" | jq -r '.[] | @base64'); 
do
    printEntity $queue
done
echo "Export completed..."
