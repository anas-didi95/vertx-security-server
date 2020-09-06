#!/bin/bash

BACKUP_NAME=$1
MONGO_USERNAME=$MONGO_INITDB_ROOT_USERNAME
MONGO_PASSWORD=$MONGO_INITDB_ROOT_PASSWORD
MONGO_AUTH_SOURCE=admin

echo // Parameters
echo / BACKUP_NAME = $BACKUP_NAME
echo / MONGO_USERNAME=$MONGO_USERNAME
echo / MONGO_PASSWORD=$MONGO_PASSWORD
echo / MONGO_AUTH_SOURCE=$MONGO_AUTH_SOURCE
echo

if [ -z "$BACKUP_NAME" ]; then
  echo "ERROR: BACKUP_NAME (param#1) is undefined!"
  exit 1
fi

mongorestore --username=$MONGO_USERNAME --password=$MONGO_PASSWORD --authenticationDatabase=$MONGO_AUTH_SOURCE --drop --verbose $BACKUP_NAME

exit 0
