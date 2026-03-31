#!/bin/bash

set -e

DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
SERVICE="${SERVICE:-${1:-}}"
SERVICE="${SERVICE:?SERVICE is required}"
LATEST=latest

BASE_DIR=/home/ubuntu/apps/data/service
cd $BASE_DIR

case $SERVICE in
  user)
    IMAGE="chamomile-user"
    CONTAINER="user-service"
    ;;
  product)
    IMAGE="chamomile-product"
    CONTAINER="product-service"
    ;;
  order)
    IMAGE="chamomile-order"
    CONTAINER="order-service"
    ;;
  settlement)
      IMAGE="chamomile-settlement"
      CONTAINER="settlement-service"
      ;;
  payment)
      IMAGE="chamomile-payment"
      CONTAINER="payment-service"
      ;;
  file)
      IMAGE="chamomile-file"
      CONTAINER="file-service"
      ;;
  admin)
      IMAGE="chamomile-admin"
      CONTAINER="admin-service"
      ;;
  *)
    echo "Unknown module"
    exit 1
    ;;
esac

  echo "Backing up current latest"
  docker tag \
    $DOCKERHUB_USERNAME/$IMAGE:latest \
    $DOCKERHUB_USERNAME/$IMAGE:backup || true

  echo "Pulling image: $DOCKERHUB_USERNAME/$IMAGE:$IMAGE_TAG"
  docker pull $DOCKERHUB_USERNAME/$IMAGE:$IMAGE_TAG

  echo "Tagging $IMAGE_TAG -> latest"
  docker tag $DOCKERHUB_USERNAME/$IMAGE:$IMAGE_TAG $DOCKERHUB_USERNAME/$IMAGE:$LATEST  || true

  echo "Restarting container: $CONTAINER"
  docker compose up -d --force-recreate $CONTAINER

  echo "Done: $SERVICE"
