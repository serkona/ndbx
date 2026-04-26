#!/bin/bash
set -e

echo "Waiting for config servers..."
until mongosh --host configsvr1 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host configsvr2 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host configsvr3 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done

echo "Init config replica set"
mongosh --host configsvr1 --port $MONGODB_PORT --eval "
rs.initiate({
  _id: 'configReplSet',
  configsvr: true,
  members: [
    { _id: 0, host: 'configsvr1:$MONGODB_PORT' },
    { _id: 1, host: 'configsvr2:$MONGODB_PORT' },
    { _id: 2, host: 'configsvr3:$MONGODB_PORT' }
  ]
})
" || true
sleep 5

echo "Waiting for shards..."
until mongosh --host shard1-primary --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host shard1-secondary1 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host shard1-secondary2 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host shard2-primary --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host shard2-secondary1 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done
until mongosh --host shard2-secondary2 --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done

echo "Init shard 1"
mongosh --host shard1-primary --port $MONGODB_PORT --eval "
rs.initiate({
  _id: 'shard1ReplSet',
  members: [
    { _id: 0, host: 'shard1-primary:$MONGODB_PORT' },
    { _id: 1, host: 'shard1-secondary1:$MONGODB_PORT' },
    { _id: 2, host: 'shard1-secondary2:$MONGODB_PORT' }
  ]
})
" || true

echo "Init shard 2"
mongosh --host shard2-primary --port $MONGODB_PORT --eval "
rs.initiate({
  _id: 'shard2ReplSet',
  members: [
    { _id: 0, host: 'shard2-primary:$MONGODB_PORT' },
    { _id: 1, host: 'shard2-secondary1:$MONGODB_PORT' },
    { _id: 2, host: 'shard2-secondary2:$MONGODB_PORT' }
  ]
})
" || true

sleep 10

echo "Waiting for mongos..."
until mongosh --host mongos --port $MONGODB_PORT --eval "db.adminCommand('ping')" > /dev/null 2>&1; do sleep 2; done

echo "Adding shards"
mongosh --host mongos --port $MONGODB_PORT --eval "
sh.addShard('shard1ReplSet/shard1-primary:$MONGODB_PORT,shard1-secondary1:$MONGODB_PORT,shard1-secondary2:$MONGODB_PORT');
sh.addShard('shard2ReplSet/shard2-primary:$MONGODB_PORT,shard2-secondary1:$MONGODB_PORT,shard2-secondary2:$MONGODB_PORT');
" || true

echo "Creating user"
mongosh --host mongos --port $MONGODB_PORT --eval "
db.getSiblingDB('$MONGODB_DATABASE').createUser({
  user: '$MONGODB_USER',
  pwd: '$MONGODB_PASSWORD',
  roles: [
    { role: 'dbOwner', db: '$MONGODB_DATABASE' }
  ]
});
" || true

echo "Enabling sharding"
mongosh --host mongos --port $MONGODB_PORT --eval "
sh.enableSharding('$MONGODB_DATABASE');
" || true

echo "Sharding events collection"
mongosh --host mongos --port $MONGODB_PORT --eval "
db.getSiblingDB('$MONGODB_DATABASE').events.createIndex({ created_by: 'hashed' });
sh.shardCollection('$MONGODB_DATABASE.events', { created_by: 'hashed' });
" || true

echo "Done!"
