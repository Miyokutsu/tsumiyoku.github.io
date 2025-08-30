#!/bin/bash

echo "🛑 Stopping Tsumiyoku application..."

# Stop all containers
docker-compose down

echo "🧹 Do you want to remove all data (volumes)? [y/N]"
read -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])+$ ]]
then
    docker-compose down -v
    echo "📦 All data has been removed"
else
    echo "📦 Data has been preserved"
fi

echo "✅ Application stopped successfully"