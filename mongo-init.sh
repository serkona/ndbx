#!/bin/bash
mongosh \
  -u "$MONGO_INITDB_ROOT_USERNAME" \
  -p "$MONGO_INITDB_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --eval "
    db.getSiblingDB('$MONGODB_DATABASE').createUser({
      user: '$MONGODB_USER',
      pwd: '$MONGODB_PASSWORD',
      roles: [{ role: 'readWrite', db: '$MONGODB_DATABASE' }]
    })
  "